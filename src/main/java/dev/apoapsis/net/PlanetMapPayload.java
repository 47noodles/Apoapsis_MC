package dev.apoapsis.net;

import dev.apoapsis.Apoapsis;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: one body's surface colour map, equirectangular, row-major from the north edge, 3 bytes (RGB) per
 * texel. Column u covers longitude -pi + 2 pi (u + 0.5) / width, row v latitude pi/2 - pi (v + 0.5) / height, with
 * lon = x / R and lat = -z / R on the body's surface dimension, as in {@code Frames}. Sent on login and when a map
 * finishes generating.
 */
public record PlanetMapPayload(String body, int width, int height, byte[] rgb) implements CustomPacketPayload {
    /** Largest accepted side, texels; 512 x 256 x 3 bytes stays well under the 1 MiB client-bound limit. */
    public static final int MAX_WIDTH = 512;
    public static final int MAX_HEIGHT = 256;

    public static final Type<PlanetMapPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "planet_map"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlanetMapPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeUtf(p.body, 32);
                buf.writeVarInt(p.width);
                buf.writeVarInt(p.height);
                buf.writeByteArray(p.rgb);
            },
            buf -> {
                String body = buf.readUtf(32);
                int width = buf.readVarInt();
                int height = buf.readVarInt();
                if (width < 1 || height < 1 || width > MAX_WIDTH || height > MAX_HEIGHT) {
                    throw new DecoderException("planet map size out of range");
                }
                byte[] rgb = buf.readByteArray(width * height * 3);
                if (rgb.length != width * height * 3) {
                    throw new DecoderException("planet map length mismatch");
                }
                return new PlanetMapPayload(body, width, height, rgb);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
