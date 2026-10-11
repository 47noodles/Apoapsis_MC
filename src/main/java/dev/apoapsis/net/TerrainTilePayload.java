package dev.apoapsis.net;

import dev.apoapsis.Apoapsis;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: one square tile of a body's far terrain, centred on the sub-ship point. A tile is an
 * {@code size x size} grid of vertices {@code spacing} blocks apart on the body's surface-dimension block grid
 * (x east, z south; lon = x / R, lat = -z / R as in {@code Frames}). Vertex (i, j) is at block
 * ({@code centerX + (i - half) * spacing}, {@code centerZ + (j - half) * spacing}) with {@code half = (size - 1) / 2};
 * index {@code j * size + i}. {@code heights} is the ground height there in blocks (y of the top solid block + 1), and
 * {@code rgb} its flat surface colour, three bytes per vertex. There are three levels of detail, 0 finest.
 *
 * <p>The nested {@link Request} is the client asking for a tile; the server answers with at most one tile per request.
 */
public record TerrainTilePayload(String body, int level, int centerX, int centerZ, int spacing, int size,
        short[] heights, byte[] rgb) implements CustomPacketPayload {
    /** Number of levels of detail. */
    public static final int LEVELS = 3;
    /** Vertex spacing of each level, blocks. */
    private static final int[] SPACING = {8, 32, 128};
    /** Vertices per side of each level. 257 x 257 x 5 bytes stays well under the 1 MiB client-bound limit. */
    private static final int[] SIZE = {129, 257, 257};
    /** Largest accepted side, vertices. */
    public static final int MAX_SIZE = 257;
    /** Largest accepted spacing, blocks. */
    public static final int MAX_SPACING = 1024;
    /** Largest accepted tile-centre coordinate, blocks; far beyond either body's circumference. */
    public static final int MAX_COORD = 1_000_000;

    public static final Type<TerrainTilePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "terrain_tile"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerrainTilePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeUtf(p.body, 32);
                buf.writeVarInt(p.level);
                buf.writeInt(p.centerX);
                buf.writeInt(p.centerZ);
                buf.writeVarInt(p.spacing);
                buf.writeVarInt(p.size);
                for (short h : p.heights) {
                    buf.writeShort(h);
                }
                buf.writeBytes(p.rgb);
            },
            buf -> {
                String body = buf.readUtf(32);
                int level = buf.readVarInt();
                int centerX = buf.readInt();
                int centerZ = buf.readInt();
                int spacing = buf.readVarInt();
                int size = buf.readVarInt();
                if (!validShape(level, centerX, centerZ, spacing, size)) {
                    throw new DecoderException("terrain tile shape out of range");
                }
                int n = size * size;
                short[] heights = new short[n];
                for (int i = 0; i < n; i++) {
                    heights[i] = buf.readShort();
                }
                byte[] rgb = new byte[3 * n];
                buf.readBytes(rgb);
                return new TerrainTilePayload(body, level, centerX, centerZ, spacing, size, heights, rgb);
            });

    /** Vertex spacing of {@code level}, blocks. */
    public static int spacingOf(int level) {
        return SPACING[level];
    }

    /** Vertices per side of {@code level}. */
    public static int sizeOf(int level) {
        return SIZE[level];
    }

    /** Width of {@code level}'s tile edge to edge, blocks. */
    public static int widthOf(int level) {
        return (SIZE[level] - 1) * SPACING[level];
    }

    /** True when the header values describe a tile this build can hold: a known level, an odd size, sane numbers. */
    public static boolean validShape(int level, int centerX, int centerZ, int spacing, int size) {
        return level >= 0 && level < LEVELS
                && spacing >= 1 && spacing <= MAX_SPACING
                && size >= 3 && size <= MAX_SIZE && (size & 1) == 1
                && Math.abs(centerX) <= MAX_COORD && Math.abs(centerZ) <= MAX_COORD;
    }

    /** True when the whole tile is consistent: a valid shape and arrays of exactly the right length. */
    public boolean valid() {
        int n = size * size;
        return body != null && body.matches("[a-z_]{1,32}")
                && validShape(level, centerX, centerZ, spacing, size)
                && heights != null && heights.length == n
                && rgb != null && rgb.length == 3 * n;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Client to server: please send the tile of {@code level} for {@code body} centred near block
     * ({@code centerX}, {@code centerZ}). The server snaps the centre to the level's grid and may ignore the request.
     */
    public record Request(String body, int level, int centerX, int centerZ) implements CustomPacketPayload {
        public static final Type<Request> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "terrain_tile_request"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Request> STREAM_CODEC = StreamCodec.of(
                (buf, r) -> {
                    buf.writeUtf(r.body, 32);
                    buf.writeVarInt(r.level);
                    buf.writeInt(r.centerX);
                    buf.writeInt(r.centerZ);
                },
                buf -> new Request(buf.readUtf(32), buf.readVarInt(), buf.readInt(), buf.readInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
