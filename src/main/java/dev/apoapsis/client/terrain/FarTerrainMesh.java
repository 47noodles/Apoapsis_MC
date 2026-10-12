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
 * One level's tile as GPU buffers: a vertex buffer (position and colour with alpha, 16 bytes each) and a triangle index
 * buffer. The vertex data is worked out by {@link #compute} on any thread and uploaded by {@link #upload} on the render
 * thread when the tile, the mode, the finer level's extent or the coarser tile changes, and drawn unchanged every frame;
 * the camera only enters through the shader's uniforms.
 *
 * <p>The vertices are the tile's grid, then a second copy of every vertex dropped by the skirt depth. The skirt
 * (quads hanging from the cut edges: the tile's border and the border of the hole a finer level fills) closes the
 * cracks between neighbouring levels. Cells inside the hole are left out, so a finer level's surface is never covered
 * by a coarser one's.
 *
 * <p>The seam between levels is hidden in the tile's outer band (a tenth of its width): where a coarser tile covers the
 * ground the band's heights and colours blend into that tile's surface, ending exactly on it at the border, so a fine
 * tile and the coarse tile around it meet with no step. Where nothing coarser covers it (the coarsest tile, or a tile
 * that has outrun the coarser one), orbit mode sinks the band to the body's sea level and fades it out, so the tile
 * dissolves into the planet beneath instead of ending in a cliff.
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
    /** The outer band that blends or fades is {@code size / BAND_DIVISOR} cells wide. */
    private static final int BAND_DIVISOR = 10;

    /** The finer tile's extent in block coordinates, vertex to vertex (inclusive); cells inside it are left out. */
    record Hole(int minX, int maxX, int minZ, int maxZ) {
        static Hole of(TerrainTilePayload t) {
            int reach = (t.size() - 1) / 2 * t.spacing();
            return new Hole(t.centerX() - reach, t.centerX() + reach, t.centerZ() - reach, t.centerZ() + reach);
        }
    }

    /**
     * What a mesh is made from: its tile, the mode, the hole a finer tile leaves and the coarser tile its band blends
     * into. Tiles are compared by identity; a mesh is rebuilt when any of the four changes.
     */
    record Key(TerrainTilePayload tile, FarTerrainView.Mode mode, @Nullable Hole hole,
            @Nullable TerrainTilePayload coarser) {
        boolean sameAs(Key o) {
            return tile == o.tile && mode == o.mode && java.util.Objects.equals(hole, o.hole) && coarser == o.coarser;
        }
    }

    /** A mesh's vertex and index data, ready to upload. The buffers are direct and not shared. */
    record Data(Key key, ByteBuffer vertices, ByteBuffer indices, int indexCount, double originX, double originY,
            double originZ) {}

    final Key key;
    final TerrainTilePayload tile;
    final GpuBuffer vertices;
    final GpuBuffer indices;
    final int indexCount;
    /** The tile's centre point on the sphere, inertial, metres (orbit mode; zero in surface mode). */
    final double originX, originY, originZ;

    private FarTerrainMesh(Data d, GpuBuffer vertices, GpuBuffer indices) {
        this.key = d.key();
        this.tile = d.key().tile();
        this.vertices = vertices;
        this.indices = indices;
        this.indexCount = d.indexCount();
        this.originX = d.originX();
        this.originY = d.originY();
        this.originZ = d.originZ();
    }

    @Override
    public void close() {
        vertices.close();
        indices.close();
    }

    /** Uploads {@code d} to the GPU. Render thread only. */
    static FarTerrainMesh upload(Data d) {
        GpuBuffer vertexBuffer = RenderSystem.getDevice()
                .createBuffer(() -> "Far terrain vertices", GpuBuffer.USAGE_VERTEX, d.vertices());
        GpuBuffer indexBuffer = RenderSystem.getDevice()
                .createBuffer(() -> "Far terrain indices", GpuBuffer.USAGE_INDEX, d.indices());
        return new FarTerrainMesh(d, vertexBuffer, indexBuffer);
    }

    /**
     * Works out the mesh of {@code key.tile()}. {@code water} flattens ground below {@code seaLevel} to the sea
     * surface (Verdance); {@code radius} is the body radius in metres (orbit mode). Reads only immutable data, so it may
     * run on any thread.
     */
    static Data compute(Key key, double radius, int seaLevel, boolean water) {
        TerrainTilePayload t = key.tile();
        boolean orbit = key.mode() == FarTerrainView.Mode.ORBIT;
        int n = t.size();
        int half = (n - 1) / 2;
        int sp = t.spacing();
        int count = n * n;
        float[] y = new float[count];
        for (int k = 0; k < count; k++) {
            y[k] = flat(t.heights()[k], water, seaLevel);
        }
        byte[] rgb = t.rgb().clone();
        float[] alpha = null;
        if (orbit) {
            alpha = new float[count];
            java.util.Arrays.fill(alpha, 1f);
        }
        blendBand(t, key.coarser(), water, seaLevel, y, rgb, alpha);

        double drop = Math.max(SKIRT_MIN, SKIRT_SPACINGS * sp);
        double lightNorm = Math.sqrt(LX * LX + LY * LY + LZ * LZ);
        double flatLight = LY / lightNorm;

        double lon0 = t.centerX() / radius, lat0 = -t.centerZ() / radius;
        double ox = 0, oy = 0, oz = 0;
        double[] cosLon = null, sinLon = null, cosLat = null, sinLat = null;
        if (orbit) {
            ox = radius * Math.cos(lat0) * Math.cos(lon0);
            oy = radius * Math.cos(lat0) * Math.sin(lon0);
            oz = radius * Math.sin(lat0);
            cosLon = new double[n];
            sinLon = new double[n];
            cosLat = new double[n];
            sinLat = new double[n];
            for (int i = 0; i < n; i++) {
                double lon = (t.centerX() + (double) (i - half) * sp) / radius;
                cosLon[i] = Math.cos(lon);
                sinLon[i] = Math.sin(lon);
                double lat = -(t.centerZ() + (double) (i - half) * sp) / radius;
                cosLat[i] = Math.cos(lat);
                sinLat[i] = Math.sin(lat);
            }
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
                    float shade = (float) Math.max(SHADE_MIN, Math.min(1.0, SHADE_MIN + (1 - SHADE_MIN) * lam / flatLight))
                            * dark;
                    float px, py, pz;
                    if (orbit) {
                        double rr = radius + (y[k] - seaLevel) + dy;
                        double cl = cosLat[j];
                        px = (float) (rr * cl * cosLon[i] - ox);
                        py = (float) (rr * cl * sinLon[i] - oy);
                        pz = (float) (rr * sinLat[j] - oz);
                    } else {
                        px = (i - half) * sp;
                        py = (float) (y[k] + dy);
                        pz = (j - half) * sp;
                    }
                    vb.putFloat(px).putFloat(py).putFloat(pz);
                    vb.put(colour(rgb[3 * k], shade)).put(colour(rgb[3 * k + 1], shade))
                            .put(colour(rgb[3 * k + 2], shade))
                            .put((byte) (alpha == null ? 255 : Math.round(alpha[k] * 255f)));
                }
            }
        }
        vb.flip();

        // Indices: two triangles per cell outside the hole, then a skirt quad on every edge that borders nothing.
        Hole hole = key.hole();
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
        return new Data(key, vb, ibBytes, indexCount, ox, oy, oz);
    }

    /**
     * Blends the tile's outer band into the coarser tile's surface where that covers it, and elsewhere (orbit mode only,
     * {@code alpha} non-null) sinks the band to sea level and fades it out. The weight is 1 on the border vertices and
     * eases to 0 across the band, so the border lies exactly on the coarser surface or is fully transparent.
     */
    private static void blendBand(TerrainTilePayload t, @Nullable TerrainTilePayload coarser, boolean water,
            int seaLevel, float[] y, byte[] rgb, float @Nullable [] alpha) {
        if (coarser == null && alpha == null) {
            return;
        }
        int n = t.size();
        int half = (n - 1) / 2;
        int sp = t.spacing();
        int band = Math.max(4, n / BAND_DIVISOR);
        double[] sample = new double[4];
        for (int j = 0; j < n; j++) {
            boolean edgeRow = j < band || j >= n - band;
            for (int i = 0; i < n; i++) {
                if (!edgeRow && i == band) {
                    i = n - band; // the interior of the row is untouched
                }
                int d = Math.min(Math.min(i, j), Math.min(n - 1 - i, n - 1 - j));
                double w = 1 - FarTerrainView.smooth((double) d / band);
                int k = j * n + i;
                double x = t.centerX() + (double) (i - half) * sp;
                double z = t.centerZ() + (double) (j - half) * sp;
                if (coarser != null && sampleCoarse(coarser, water, seaLevel, x, z, sample)) {
                    y[k] += (float) ((sample[0] - y[k]) * w);
                    for (int c = 0; c < 3; c++) {
                        double from = rgb[3 * k + c] & 0xFF;
                        rgb[3 * k + c] = (byte) Math.round(from + (sample[1 + c] - from) * w);
                    }
                } else if (alpha != null) {
                    alpha[k] = (float) (1 - w);
                    y[k] += (float) ((seaLevel - y[k]) * w);
                }
            }
        }
    }

    /**
     * Height (index 0) and colour (1 to 3) of {@code c}'s surface at block ({@code x}, {@code z}), bilinear between its
     * four grid vertices, into {@code out}. False when the point is outside {@code c}.
     */
    private static boolean sampleCoarse(TerrainTilePayload c, boolean water, int seaLevel, double x, double z,
            double[] out) {
        int nc = c.size();
        int hc = (nc - 1) / 2;
        double gx = (x - c.centerX()) / c.spacing() + hc;
        double gz = (z - c.centerZ()) / c.spacing() + hc;
        if (gx < 0 || gz < 0 || gx > nc - 1 || gz > nc - 1) {
            return false;
        }
        int i0 = Math.min((int) gx, nc - 2);
        int j0 = Math.min((int) gz, nc - 2);
        double fx = gx - i0;
        double fz = gz - j0;
        int k00 = j0 * nc + i0;
        int k10 = k00 + 1;
        int k01 = k00 + nc;
        int k11 = k01 + 1;
        short[] h = c.heights();
        out[0] = bilinear(flat(h[k00], water, seaLevel), flat(h[k10], water, seaLevel), flat(h[k01], water, seaLevel),
                flat(h[k11], water, seaLevel), fx, fz);
        byte[] rgb = c.rgb();
        for (int ch = 0; ch < 3; ch++) {
            out[1 + ch] = bilinear(rgb[3 * k00 + ch] & 0xFF, rgb[3 * k10 + ch] & 0xFF, rgb[3 * k01 + ch] & 0xFF,
                    rgb[3 * k11 + ch] & 0xFF, fx, fz);
        }
        return true;
    }

    private static double bilinear(double v00, double v10, double v01, double v11, double fx, double fz) {
        double a = v00 + (v10 - v00) * fx;
        double b = v01 + (v11 - v01) * fx;
        return a + (b - a) * fz;
    }

    private static float flat(int h, boolean water, int seaLevel) {
        return water && h < seaLevel ? seaLevel : h;
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
