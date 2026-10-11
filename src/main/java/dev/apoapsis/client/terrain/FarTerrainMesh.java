package dev.apoapsis.client.terrain;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import dev.apoapsis.net.TerrainTilePayload;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import org.jspecify.annotations.Nullable;

/**
 * One level's tile as GPU buffers: a vertex buffer (position and colour, 16 bytes each) and a triangle index buffer.
 * Built on the render thread when the tile, the mode or the finer level's extent changes, and drawn unchanged every
 * frame; the camera only enters through the shader's uniforms.
 *
 * <p>The vertices are the tile's grid, then a second copy of every vertex dropped by the skirt depth. The skirt
 * (quads hanging from the cut edges: the tile's border and the border of the hole a finer level fills) closes the
 * cracks between neighbouring levels. Cells inside the hole are left out, so a finer level's surface is never covered
 * by a coarser one's.
 *
 * <p>Surface mode stores positions in blocks relative to the tile's centre column (east, up, south). Orbit mode
 * stores them in metres relative to the tile centre's point on the sphere, in the body's inertial axes, at the radius
 * {@code R + (height - sea level)}.
 */
final class FarTerrainMesh implements AutoCloseable {
    /** Bytes per vertex: three floats and four colour bytes. */
    private static final int VERTEX_BYTES = 16;
    /** Brightness range of the slope shading: flat ground 1.0, a slope facing away from the light this much. */
    private static final float SHADE_MIN = 0.62f;
    /** Light direction for the slope shading (unnormalised): from the south-east, high. Axes: x east, y up, z south. */
    private static final double LX = 0.40, LY = 0.85, LZ = 0.35;
    /** The skirt hangs this many vertex spacings, at least {@link #SKIRT_MIN} blocks. */
    private static final int SKIRT_SPACINGS = 2;
    private static final int SKIRT_MIN = 24;

    /** The finer tile's extent in block coordinates, vertex to vertex (inclusive); cells inside it are left out. */
    record Hole(int minX, int maxX, int minZ, int maxZ) {
        static Hole of(TerrainTilePayload t) {
            int reach = (t.size() - 1) / 2 * t.spacing();
            return new Hole(t.centerX() - reach, t.centerX() + reach, t.centerZ() - reach, t.centerZ() + reach);
        }
    }

    final TerrainTilePayload tile;
    final FarTerrainView.Mode mode;
    final @Nullable Hole hole;
    final GpuBuffer vertices;
    final GpuBuffer indices;
    final int indexCount;
    /** The tile's centre point on the sphere, inertial, metres (orbit mode; zero in surface mode). */
    final double originX, originY, originZ;

    private FarTerrainMesh(TerrainTilePayload tile, FarTerrainView.Mode mode, @Nullable Hole hole, GpuBuffer vertices,
            GpuBuffer indices, int indexCount, double ox, double oy, double oz) {
        this.tile = tile;
        this.mode = mode;
        this.hole = hole;
        this.vertices = vertices;
        this.indices = indices;
        this.indexCount = indexCount;
        this.originX = ox;
        this.originY = oy;
        this.originZ = oz;
    }

    /** True when this mesh is still right for the tile, mode and hole. */
    boolean matches(TerrainTilePayload tile, FarTerrainView.Mode mode, @Nullable Hole hole) {
        return this.tile == tile && this.mode == mode && java.util.Objects.equals(this.hole, hole);
    }

    @Override
    public void close() {
        vertices.close();
        indices.close();
    }

    /**
     * Builds the mesh of {@code t}. {@code water} flattens ground below {@code seaLevel} to the sea surface (Verdance);
     * {@code radius} is the body radius in metres (orbit mode).
     */
    static FarTerrainMesh build(TerrainTilePayload t, FarTerrainView.Mode mode, double radius, int seaLevel,
            boolean water, @Nullable Hole hole) {
        int n = t.size();
        int half = (n - 1) / 2;
        int sp = t.spacing();
        int count = n * n;
        float[] y = new float[count];
        for (int k = 0; k < count; k++) {
            int h = t.heights()[k];
            y[k] = water && h < seaLevel ? seaLevel : h;
        }
        double drop = Math.max(SKIRT_MIN, SKIRT_SPACINGS * sp);
        double lightNorm = Math.sqrt(LX * LX + LY * LY + LZ * LZ);
        double flat = LY / lightNorm;

        boolean orbit = mode == FarTerrainView.Mode.ORBIT;
        double lon0 = t.centerX() / radius, lat0 = -t.centerZ() / radius;
        double ox = 0, oy = 0, oz = 0;
        if (orbit) {
            ox = radius * Math.cos(lat0) * Math.cos(lon0);
            oy = radius * Math.cos(lat0) * Math.sin(lon0);
            oz = radius * Math.sin(lat0);
        }

        ByteBuffer vb = ByteBuffer.allocateDirect(2 * count * VERTEX_BYTES).order(ByteOrder.nativeOrder());
        for (int pass = 0; pass < 2; pass++) {
            double dy = pass == 0 ? 0 : -drop;
            float dark = pass == 0 ? 1f : 0.85f;
            for (int j = 0; j < n; j++) {
                for (int i = 0; i < n; i++) {
                    int k = j * n + i;
                    float hx = (y[j * n + Math.min(i + 1, n - 1)] - y[j * n + Math.max(i - 1, 0)])
                            / ((Math.min(i + 1, n - 1) - Math.max(i - 1, 0)) * sp);
                    float hz = (y[Math.min(j + 1, n - 1) * n + i] - y[Math.max(j - 1, 0) * n + i])
                            / ((Math.min(j + 1, n - 1) - Math.max(j - 1, 0)) * sp);
                    double nl = Math.sqrt(hx * hx + 1 + hz * hz);
                    double lam = (-hx * LX + LY - hz * LZ) / nl / lightNorm;
                    float shade = (float) Math.max(SHADE_MIN, Math.min(1.0, SHADE_MIN + (1 - SHADE_MIN) * lam / flat))
                            * dark;
                    int bx = (i - half) * sp, bz = (j - half) * sp;
                    float px, py, pz;
                    if (orbit) {
                        double x = t.centerX() + bx, z = t.centerZ() + bz;
                        double lon = x / radius, lat = -z / radius;
                        double rr = radius + (y[k] - seaLevel) + dy;
                        double cl = Math.cos(lat);
                        px = (float) (rr * cl * Math.cos(lon) - ox);
                        py = (float) (rr * cl * Math.sin(lon) - oy);
                        pz = (float) (rr * Math.sin(lat) - oz);
                    } else {
                        px = bx;
                        py = (float) (y[k] + dy);
                        pz = bz;
                    }
                    vb.putFloat(px).putFloat(py).putFloat(pz);
                    vb.put(colour(t.rgb()[3 * k], shade)).put(colour(t.rgb()[3 * k + 1], shade))
                            .put(colour(t.rgb()[3 * k + 2], shade)).put((byte) 255);
                }
            }
        }
        vb.flip();

        // Indices: two triangles per cell outside the hole, then a skirt quad on every edge that borders nothing.
        int cells = n - 1;
        boolean[] skip = new boolean[cells * cells];
        int drawn = 0;
        for (int j = 0; j < cells; j++) {
            for (int i = 0; i < cells; i++) {
                int x0 = t.centerX() + (i - half) * sp, z0 = t.centerZ() + (j - half) * sp;
                boolean inside = hole != null && x0 >= hole.minX() && x0 + sp <= hole.maxX()
                        && z0 >= hole.minZ() && z0 + sp <= hole.maxZ();
                skip[j * cells + i] = inside;
                if (!inside) {
                    drawn++;
                }
            }
        }
        // Room for the cells and a skirt along the tile border and along the hole border (each at most 4 * cells long).
        ByteBuffer ibBytes = ByteBuffer.allocateDirect((drawn * 6 + 8 * cells * 6) * 4).order(ByteOrder.nativeOrder());
        IntBuffer ib = ibBytes.asIntBuffer();
        for (int j = 0; j < cells; j++) {
            for (int i = 0; i < cells; i++) {
                if (skip[j * cells + i]) {
                    continue;
                }
                int v00 = j * n + i, v10 = v00 + 1, v01 = v00 + n, v11 = v01 + 1;
                ib.put(v00).put(v01).put(v10);
                ib.put(v10).put(v01).put(v11);
                if (j == 0 || skip[(j - 1) * cells + i]) {
                    skirt(ib, v00, v10, count);
                }
                if (j == cells - 1 || skip[(j + 1) * cells + i]) {
                    skirt(ib, v01, v11, count);
                }
                if (i == 0 || skip[j * cells + i - 1]) {
                    skirt(ib, v00, v01, count);
                }
                if (i == cells - 1 || skip[j * cells + i + 1]) {
                    skirt(ib, v10, v11, count);
                }
            }
        }
        int indexCount = ib.position();
        ibBytes.limit(indexCount * 4);

        GpuBuffer vertexBuffer = RenderSystem.getDevice()
                .createBuffer(() -> "Far terrain vertices", GpuBuffer.USAGE_VERTEX, vb);
        GpuBuffer indexBuffer = RenderSystem.getDevice()
                .createBuffer(() -> "Far terrain indices", GpuBuffer.USAGE_INDEX, ibBytes);
        return new FarTerrainMesh(t, mode, hole, vertexBuffer, indexBuffer, indexCount, ox, oy, oz);
    }

    /** A quad from the top vertices {@code a}, {@code b} down to their dropped copies. */
    private static void skirt(IntBuffer ib, int a, int b, int count) {
        ib.put(a).put(b).put(a + count);
        ib.put(b).put(b + count).put(a + count);
    }

    private static byte colour(byte c, float shade) {
        return (byte) Math.max(0, Math.min(255, Math.round((c & 0xFF) * shade)));
    }

    static int vertexBytes() {
        return DefaultVertexFormat.POSITION_COLOR.getVertexSize();
    }
}
