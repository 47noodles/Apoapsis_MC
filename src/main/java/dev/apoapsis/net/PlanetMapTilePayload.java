package dev.apoapsis.net;

import dev.apoapsis.Apoapsis;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: one {@link #TILE} x {@link #TILE} tile of a refined planet map ({@code level} 1 and up; level 0 is
 * the whole-map {@link PlanetMapPayload}). The full map of this level is {@code width x height} texels, equirectangular as
 * in {@link PlanetMapPayload}; this tile covers columns {@code tx * TILE ..} and rows {@code ty * TILE ..}. Its texels
 * are 3 bytes (RGB) each, row-major, zlib-deflated into {@code deflated}; {@code rawLength} is the size inflated and is
 * always {@code TILE * TILE * 3}.
 */
public record PlanetMapTilePayload(String body, int level, int width, int height, int tx, int ty, int rawLength,
        byte[] deflated) implements CustomPacketPayload {
    /** Tile edge, texels. */
    public static final int TILE = 256;
    /** Inflated size of every tile, bytes. */
    public static final int RAW_LENGTH = TILE * TILE * 3;
    /** Largest accepted deflated tile, bytes: the raw size plus zlib's worst-case growth, well under 1 MiB. */
    public static final int MAX_DEFLATED = RAW_LENGTH + 1024;
    /** Highest refined level the client accepts. */
    public static final int MAX_LEVEL = 4;
    /** Largest map side the client accepts, texels. */
    public static final int MAX_SIDE = 8192;

    public static final Type<PlanetMapTilePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "planet_map_tile"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlanetMapTilePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeUtf(p.body, 32);
                buf.writeVarInt(p.level);
                buf.writeVarInt(p.width);
                buf.writeVarInt(p.height);
                buf.writeVarInt(p.tx);
                buf.writeVarInt(p.ty);
                buf.writeVarInt(p.rawLength);
                buf.writeByteArray(p.deflated);
            },
            buf -> {
                String body = buf.readUtf(32);
                int level = buf.readVarInt();
                int width = buf.readVarInt();
                int height = buf.readVarInt();
                int tx = buf.readVarInt();
                int ty = buf.readVarInt();
                int rawLength = buf.readVarInt();
                byte[] deflated = buf.readByteArray(MAX_DEFLATED);
                PlanetMapTilePayload p = new PlanetMapTilePayload(body, level, width, height, tx, ty, rawLength,
                        deflated);
                if (!p.valid()) {
                    throw new DecoderException("planet map tile out of range");
                }
                return p;
            });

    /** True when the header is consistent: a known body name, level and side, a tile inside the map, the fixed raw size. */
    public boolean valid() {
        return body != null && body.matches("[a-z_]{1,32}")
                && level >= 1 && level <= MAX_LEVEL
                && width >= TILE && width <= MAX_SIDE && width % TILE == 0
                && height >= TILE && height <= MAX_SIDE && height % TILE == 0
                && tx >= 0 && tx < width / TILE && ty >= 0 && ty < height / TILE
                && rawLength == RAW_LENGTH
                && deflated != null && deflated.length > 0 && deflated.length <= MAX_DEFLATED;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
