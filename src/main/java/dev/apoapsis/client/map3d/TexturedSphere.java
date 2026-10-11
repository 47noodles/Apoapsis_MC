package dev.apoapsis.client.map3d;

/**
 * A reusable, allocation-free textured sphere mesh for the 3D map. The sphere is a {@link #LAT} by {@link #LON} grid
 * of quads in the body's own frame (latitude south to north, longitude 0 to 2 pi), projected through an
 * {@link OrbitCamera} into screen space. Texture coordinates are equirectangular: U = (lon + pi) / (2 pi), V = (pi/2
 * - lat) / pi, with the north edge at V = 0. Each vertex is tinted by the sun on +X via {@link Scene3D#shade}.
 * Pure maths, no game state. Client only.
 */
public final class TexturedSphere {
    /** Latitude cells (pole to pole). */
    public static final int LAT = 40;
    /** Longitude cells (one turn). */
    public static final int LON = 80;

    /** Vertices written by the last build, 4 per visible cell. Arrays hold {@code LAT * LON * 4} vertices. */
    public final float[] x = new float[LAT * LON * 4];
    /** Screen y of each vertex, in pixels. */
    public final float[] y = new float[LAT * LON * 4];
    /** Texture U of each vertex. */
    public final float[] u = new float[LAT * LON * 4];
    /** Texture V of each vertex. */
    public final float[] v = new float[LAT * LON * 4];
    /** Opaque ARGB tint of each vertex. */
    public final int[] argb = new int[LAT * LON * 4];
    /** Vertices written by the last build, a multiple of 4. */
    public int count;

    /** Offsets from a cell's first grid vertex to its four corners: (i,j), (i+1,j), (i+1,j+1), (i,j+1). */
    private static final int[] CORNER = {0, LON + 1, LON + 2, 1};
    /** Unit normal of each grid vertex, which is also its unit position. */
    private static final double[] NX = new double[(LAT + 1) * (LON + 1)];
    private static final double[] NY = new double[(LAT + 1) * (LON + 1)];
    private static final double[] NZ = new double[(LAT + 1) * (LON + 1)];
    /** Texture coordinates of each grid vertex. */
    private static final float[] GU = new float[(LAT + 1) * (LON + 1)];
    private static final float[] GV = new float[(LAT + 1) * (LON + 1)];

    static {
        for (int i = 0; i <= LAT; i++) {
            double lat = -Math.PI / 2 + Math.PI * i / LAT;
            for (int j = 0; j <= LON; j++) {
                double lon = 2 * Math.PI * j / LON;
                int g = i * (LON + 1) + j;
                NX[g] = Math.cos(lat) * Math.cos(lon);
                NY[g] = Math.cos(lat) * Math.sin(lon);
                NZ[g] = Math.sin(lat);
                GU[g] = (float) ((double) j / LON + 0.5);
                GV[g] = (float) Math.min(1 - 1e-6, (Math.PI / 2 - lat) / Math.PI);
            }
        }
    }

    private final double[] out = new double[3];
    private final double[] sx = new double[4];
    private final double[] sy = new double[4];

    /**
     * Builds the visible quads for a sphere of {@code radius} centred at ({@code ccx}, {@code ccy}, {@code ccz}) and
     * seen from the eye at ({@code ex}, {@code ey}, {@code ez}). All positions are relative to the camera's focus, as
     * in {@link Scene3D}. {@code cx}, {@code cy} and {@code focal} are the screen centre and focal length in pixels.
     * A cell is drawn when any of its corners faces the eye, and skipped when any corner is behind the eye. Sets
     * {@link #count}; allocates nothing.
     */
    public void build(OrbitCamera cam, double cx, double cy, double focal, double ccx, double ccy, double ccz,
            double radius, double ex, double ey, double ez) {
        count = 0;
        for (int i = 0; i < LAT; i++) {
            for (int j = 0; j < LON; j++) {
                int a = i * (LON + 1) + j;
                boolean facing = false;
                for (int k = 0; k < 4 && !facing; k++) {
                    int g = a + CORNER[k];
                    double dot = NX[g] * (ex - (ccx + radius * NX[g])) + NY[g] * (ey - (ccy + radius * NY[g]))
                            + NZ[g] * (ez - (ccz + radius * NZ[g]));
                    facing = dot > 0;
                }
                if (!facing) {
                    continue;
                }
                boolean inFront = true;
                for (int k = 0; k < 4 && inFront; k++) {
                    int g = a + CORNER[k];
                    double depth = cam.project(ccx + radius * NX[g], ccy + radius * NY[g], ccz + radius * NZ[g],
                            cx, cy, focal, out);
                    inFront = !Double.isNaN(depth);
                    sx[k] = out[0];
                    sy[k] = out[1];
                }
                if (!inFront) {
                    continue;
                }
                for (int k = 0; k < 4; k++) {
                    int g = a + CORNER[k];
                    x[count] = (float) sx[k];
                    y[count] = (float) sy[k];
                    u[count] = GU[g];
                    v[count] = GV[g];
                    argb[count] = Scene3D.shade(0xFFFFFFFF, NX[g]);
                    count++;
                }
            }
        }
    }
}
