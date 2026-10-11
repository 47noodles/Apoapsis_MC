package dev.apoapsis.ship;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

/** Rotation and facing helpers shared by the ship classes. */
final class ShipGeometry {
    private ShipGeometry() {}

    /** Row-major 3x3 rotation matrix of a unit quaternion: row r, column c is element 3r + c. */
    static double[] matrix(Quaternionfc q) {
        double x = q.x(), y = q.y(), z = q.z(), w = q.w();
        return new double[] {
            1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w),
            2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w),
            2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y),
        };
    }

    /** Applies a row-major matrix to a vector. */
    static Vec3 apply(double[] m, Vec3 v) {
        return new Vec3(
                m[0] * v.x + m[1] * v.y + m[2] * v.z,
                m[3] * v.x + m[4] * v.y + m[5] * v.z,
                m[6] * v.x + m[7] * v.y + m[8] * v.z);
    }

    /**
     * One of the 24 axis-aligned proper rotations, as a signed permutation: basis vector j maps to
     * {@code sign[j] * e[perm[j]]}.
     *
     * @param angleDeg   angle between the snapped rotation and the source orientation, degrees
     * @param quaternion the snapped rotation
     */
    record Snap(int[] perm, int[] sign, double angleDeg, Quaternionf quaternion) {
        /** Rotates a vector by this snapped rotation. */
        Vec3 apply(Vec3 v) {
            double[] in = {v.x, v.y, v.z};
            double[] out = new double[3];
            for (int j = 0; j < 3; j++) {
                out[perm[j]] = sign[j] * in[j];
            }
            return new Vec3(out[0], out[1], out[2]);
        }

        /** Rotates a unit direction and snaps the result to the nearest direction. */
        Direction apply(Direction d) {
            Vec3i u = d.getUnitVec3i();
            Vec3 r = apply(new Vec3(u.getX(), u.getY(), u.getZ()));
            return Direction.getApproximateNearest(r.x, r.y, r.z);
        }
    }

    /** The nearest of the 24 axis-aligned rotations to {@code q}. */
    static Snap snap(Quaternionfc q) {
        double[] m = matrix(q);
        int[][] perms = {{0, 1, 2}, {1, 2, 0}, {2, 0, 1}, {0, 2, 1}, {2, 1, 0}, {1, 0, 2}};
        int[] bestPerm = null;
        int[] bestSign = null;
        double bestTrace = -Double.MAX_VALUE;
        for (int[] perm : perms) {
            int parity = permutationParity(perm);
            for (int bits = 0; bits < 8; bits++) {
                int[] sign = {(bits & 1) == 0 ? 1 : -1, (bits & 2) == 0 ? 1 : -1, (bits & 4) == 0 ? 1 : -1};
                if (parity * sign[0] * sign[1] * sign[2] != 1) {
                    continue;
                }
                // trace(S^T R) where column j of S is sign[j] * e[perm[j]] and column j of R is m[.][j]
                double trace = 0;
                for (int j = 0; j < 3; j++) {
                    trace += sign[j] * m[3 * perm[j] + j];
                }
                if (trace > bestTrace) {
                    bestTrace = trace;
                    bestPerm = perm;
                    bestSign = sign;
                }
            }
        }
        double cos = Math.max(-1.0, Math.min(1.0, (bestTrace - 1) / 2));
        double angleDeg = Math.toDegrees(Math.acos(cos));

        Vector3f[] columns = new Vector3f[3];
        for (int j = 0; j < 3; j++) {
            float[] c = new float[3];
            c[bestPerm[j]] = bestSign[j];
            columns[j] = new Vector3f(c[0], c[1], c[2]);
        }
        Quaternionf quat = new Quaternionf().setFromNormalized(new Matrix3f().set(columns[0], columns[1], columns[2]));
        return new Snap(bestPerm, bestSign, angleDeg, quat);
    }

    private static int permutationParity(int[] p) {
        int inversions = 0;
        for (int i = 0; i < p.length; i++) {
            for (int j = i + 1; j < p.length; j++) {
                if (p[i] > p[j]) {
                    inversions++;
                }
            }
        }
        return inversions % 2 == 0 ? 1 : -1;
    }

    /** The block's "facing" direction property, or null if it has none. */
    static Direction facingOf(BlockState state) {
        Property<Direction> p = facingProperty(state);
        return p == null ? null : state.getValue(p);
    }

    /**
     * True if the block's facing, turned by {@code snap}, is a value its property allows. A horizontal facing (a
     * cockpit's) turned up or down is not, so such a block cannot be placed by a sideways rotation.
     */
    static boolean canRotateFacing(BlockState state, Snap snap) {
        Property<Direction> p = facingProperty(state);
        return p == null || p.getPossibleValues().contains(snap.apply(state.getValue(p)));
    }

    /**
     * Returns the state with its "facing" property turned by {@code snap}. The caller must have checked
     * {@link #canRotateFacing} for this state and snap.
     */
    static BlockState rotateFacing(BlockState state, Snap snap) {
        Property<Direction> p = facingProperty(state);
        return p == null ? state : state.setValue(p, snap.apply(state.getValue(p)));
    }

    @SuppressWarnings("unchecked")
    private static Property<Direction> facingProperty(BlockState state) {
        for (Property<?> p : state.getProperties()) {
            if ("facing".equals(p.getName()) && p.getValueClass() == Direction.class) {
                return (Property<Direction>) p;
            }
        }
        return null;
    }
}
