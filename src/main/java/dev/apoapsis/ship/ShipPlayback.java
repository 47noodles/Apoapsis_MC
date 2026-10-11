package dev.apoapsis.ship;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

/**
 * The smoothing of a ship's pose on a client, with no game objects in it so it can be driven by a test: samples of the
 * server's flight go in with their server tick, and each client tick the pose comes out. See {@link ShipInterpolation}
 * for how it is wired to the entity.
 *
 * <p>A <i>sample</i> is the ship's position, velocity and orientation at the end of one server tick. A playhead
 * {@code tau}, in server ticks, advances exactly one tick per {@link #tick()} (a client tick). The pose is the flight at
 * {@code tau}: a cubic Hermite curve through the samples around it (positions with their velocities as tangents) and a
 * slerp for the orientation. Because {@code tau} does not depend on when samples arrive, jitter in their arrival, two
 * in one frame, or none, never reaches the pose.
 *
 * <ul>
 *   <li>The playhead starts {@link #TARGET_LAG} ticks behind the newest sample, so there are samples on both sides of
 *       it. Only an average lag outside {@code LAG_MIN..LAG_MAX} (a slow client, a slow server) changes its speed, by at
 *       most {@link #STEER_MAX}, and a lag beyond {@link #LAG_JUMP} moves it at once.</li>
 *   <li>Past the newest sample (a late packet, a server hitch) the pose is extrapolated along the velocity and
 *       acceleration for at most {@link #EXTRAPOLATE_MAX} ticks; a server that catches up with a burst of ticks then
 *       lands on the same curve.</li>
 *   <li>A sample more than {@link #SNAP_BLOCKS} metres from where the last one was heading, or an orientation that
 *       turned more than {@link #SNAP_RADIANS} per tick, is a teleport: the pose jumps to it. A gap of more than
 *       {@link #GAP_TICKS} ticks between stamps (a ship that slept on the ground) holds the old pose and starts
 *       again.</li>
 * </ul>
 */
public final class ShipPlayback {
    /** Seconds per server tick. */
    private static final double DT = 0.05;
    private static final int RING = 16;
    private static final int RING_MASK = RING - 1;
    /** Where the playhead starts relative to the newest sample, in ticks. */
    static final double TARGET_LAG = 1.0;
    /** Lag range, in ticks, inside which the playhead runs at exactly one tick per client tick. */
    static final double LAG_MIN = 0.0;
    static final double LAG_MAX = 2.5;
    /** A lag beyond this many ticks (a long client stall) moves the playhead to the target lag at once. */
    static final double LAG_JUMP = 8.0;
    /** Share of the lag error taken into the lag average each client tick. */
    private static final double LAG_AVERAGE = 0.05;
    /** Playhead speed change per tick of average lag outside the range, and its limit. */
    private static final double STEER_GAIN = 0.1;
    static final double STEER_MAX = 0.25;
    /** Longest extrapolation past the newest sample, in ticks. */
    static final double EXTRAPOLATE_MAX = 10.0;
    /** Largest acceleration used to extrapolate, m/s^2. */
    private static final double ACCEL_MAX = 200.0;
    /** A sample this far from where the last one was heading is a teleport, in metres. */
    static final double SNAP_BLOCKS = 16.0;
    /** An orientation turn of more than this between ticks is a snap, in radians per tick. */
    static final double SNAP_RADIANS = 0.6;
    /** A stamp gap larger than this many ticks restarts the playback. */
    static final int GAP_TICKS = 3;

    private final int[] stamp = new int[RING];
    private final double[] px = new double[RING];
    private final double[] py = new double[RING];
    private final double[] pz = new double[RING];
    private final double[] vx = new double[RING];
    private final double[] vy = new double[RING];
    private final double[] vz = new double[RING];
    private final Quaternionf[] rot = new Quaternionf[RING];
    private int head;
    private int count;

    private boolean running;
    /** True once the orientation is smoothed; stays true if the position restarts. */
    private boolean oriented;
    private double tau;
    private double lagAverage;
    /** True when a sample came in since the last tick; the lag is only measured then, so a stall does not count. */
    private boolean arrived;

    /** The smoothed orientation at the latest tick and at the one before. */
    private final Quaternionf current = new Quaternionf();
    private final Quaternionf previous = new Quaternionf();

    /** Result of {@link #evaluate}, which is also the pose after {@link #tick()}. */
    private double ex;
    private double ey;
    private double ez;
    private final Quaternionf eq = new Quaternionf();
    private final Quaternionf scratchA = new Quaternionf();
    private final Quaternionf scratchB = new Quaternionf();

    public ShipPlayback() {
        for (int i = 0; i < RING; i++) {
            rot[i] = new Quaternionf();
        }
    }

    /** True once a sample has started the playback; false again after {@link #reset()}. */
    public boolean isRunning() {
        return running;
    }

    /** True once the orientation is smoothed. */
    public boolean hasOrientation() {
        return oriented;
    }

    /** The pose after the latest {@link #tick()}, or after the sample that started the playback. */
    public double x() {
        return ex;
    }

    public double y() {
        return ey;
    }

    public double z() {
        return ez;
    }

    /** The smoothed orientation as of the latest tick. */
    public Quaternionf orientationNow(Quaternionf dest) {
        return dest.set(current);
    }

    /** The smoothed orientation {@code partial} of the way from the previous tick to the latest. */
    public Quaternionf orientationAt(float partial, Quaternionf dest) {
        return previous.slerp(current, Math.min(Math.max(partial, 0.0F), 1.0F), dest);
    }

    /** Lag of the playhead behind the newest sample, in ticks; NaN before the first sample. */
    public double lagTicks() {
        return running ? stamp[slot(0)] - tau : Double.NaN;
    }

    /** Server tick of the newest sample, or -1 before the first. */
    public int newestStamp() {
        return count > 0 ? stamp[slot(0)] : -1;
    }

    /** Forgets the samples: the position was set by someone else. The orientation stays smoothed. */
    public void reset() {
        running = false;
        count = 0;
        previous.set(current);
    }

    /**
     * A sample: the state at the end of server tick {@code serverTick}.
     *
     * @return true when the playback (re)started on this sample, so the pose jumps instead of moving there
     */
    public boolean accept(int serverTick, Vec3 pos, Vec3 velocity, Quaternionfc orientation) {
        if (!running) {
            restartFrom(serverTick, pos, velocity, orientation);
            return true;
        }
        int slot = slot(0);
        int last = stamp[slot];
        if (serverTick < last) {
            return false;
        }
        if (serverTick == last) {
            store(slot, serverTick, pos, velocity, orientation);
            return false;
        }
        int gap = serverTick - last;
        if (gap > GAP_TICKS) {
            restartHolding(serverTick, pos, velocity, orientation);
            return true;
        }
        double dx = pos.x - (px[slot] + vx[slot] * DT * gap);
        double dy = pos.y - (py[slot] + vy[slot] * DT * gap);
        double dz = pos.z - (pz[slot] + vz[slot] * DT * gap);
        if (dx * dx + dy * dy + dz * dz > SNAP_BLOCKS * SNAP_BLOCKS
                || angle(rot[slot], orientation) > SNAP_RADIANS * gap) {
            restartFrom(serverTick, pos, velocity, orientation);
            return true;
        }
        push(serverTick, pos, velocity, orientation);
        arrived = true;
        return false;
    }

    /** One client tick: move the playhead and put the pose on the curve. Does nothing before the first sample. */
    public void tick() {
        if (!running) {
            return;
        }
        double newest = stamp[slot(0)];
        double lag = newest - tau;
        if (arrived) {
            // Measured only on ticks that brought a sample: a stall (no samples) must not look like a slow playhead,
            // since the burst that ends it brings the lag back.
            lagAverage += (lag - lagAverage) * LAG_AVERAGE;
            arrived = false;
        }
        double rate = 1.0;
        if (lagAverage > LAG_MAX) {
            rate += Math.min(STEER_MAX, STEER_GAIN * (lagAverage - LAG_MAX));
        } else if (lagAverage < LAG_MIN) {
            rate -= Math.min(STEER_MAX, STEER_GAIN * (LAG_MIN - lagAverage));
        }
        if (lag > LAG_JUMP) {
            tau = newest - TARGET_LAG;
            lagAverage = TARGET_LAG;
        } else {
            tau += rate;
        }
        evaluate(tau);
        previous.set(current);
        current.set(eq);
    }

    // ---- samples ---------------------------------------------------------------------------------------------

    private void store(int slot, int serverTick, Vec3 pos, Vec3 velocity, Quaternionfc orientation) {
        stamp[slot] = serverTick;
        px[slot] = pos.x;
        py[slot] = pos.y;
        pz[slot] = pos.z;
        vx[slot] = velocity.x;
        vy[slot] = velocity.y;
        vz[slot] = velocity.z;
        rot[slot].set(orientation);
    }

    private void push(int serverTick, Vec3 pos, Vec3 velocity, Quaternionfc orientation) {
        store(head, serverTick, pos, velocity, orientation);
        head = (head + 1) & RING_MASK;
        count = Math.min(count + 1, RING);
    }

    /** Slot of the {@code n}th newest sample (0 is the newest). */
    private int slot(int n) {
        return (head - 1 - n) & RING_MASK;
    }

    /**
     * Starts again from a sample: the three ticks before it are filled in along its velocity, the playhead goes to the
     * target lag, and the pose is the curve there, to be placed without a lerp from where the ship was.
     */
    private void restartFrom(int serverTick, Vec3 pos, Vec3 velocity, Quaternionfc orientation) {
        count = 0;
        for (int k = 3; k >= 1; k--) {
            push(serverTick - k, pos.subtract(velocity.scale(DT * k)), velocity, orientation);
        }
        push(serverTick, pos, velocity, orientation);
        begin();
    }

    /** Starts again after a gap in the stamps: the old pose is held for the ticks before the sample. */
    private void restartHolding(int serverTick, Vec3 pos, Vec3 velocity, Quaternionfc orientation) {
        int slot = slot(0);
        Vec3 held = new Vec3(px[slot], py[slot], pz[slot]);
        Quaternionf heldRotation = new Quaternionf(rot[slot]);
        count = 0;
        for (int k = 3; k >= 1; k--) {
            push(serverTick - k, held, Vec3.ZERO, heldRotation);
        }
        push(serverTick, pos, velocity, orientation);
        begin();
    }

    private void begin() {
        tau = stamp[slot(0)] - TARGET_LAG;
        lagAverage = TARGET_LAG;
        arrived = false;
        running = true;
        oriented = true;
        evaluate(tau);
        current.set(eq);
        previous.set(eq);
    }

    private static double angle(Quaternionfc a, Quaternionfc b) {
        float dot = Math.abs(a.x() * b.x() + a.y() * b.y() + a.z() * b.z() + a.w() * b.w());
        return 2.0 * Math.acos(Math.min(1.0, dot));
    }

    // ---- the curve -------------------------------------------------------------------------------------------

    /** Puts the flight's position and orientation at playhead {@code t} into {@link #ex}, {@link #ey}, {@link #ez}, {@link #eq}. */
    private void evaluate(double t) {
        int newest = slot(0);
        double newestStamp = stamp[newest];
        if (t >= newestStamp) {
            extrapolate(newest, t - newestStamp);
            return;
        }
        for (int n = 0; n + 1 < count; n++) {
            int b = slot(n);
            int a = slot(n + 1);
            if (stamp[a] <= t) {
                hermite(a, b, t);
                return;
            }
        }
        int oldest = slot(count - 1);
        ex = px[oldest];
        ey = py[oldest];
        ez = pz[oldest];
        eq.set(rot[oldest]);
    }

    /** Cubic Hermite between samples {@code a} (older) and {@code b}, with the velocities as tangents. */
    private void hermite(int a, int b, double t) {
        double span = stamp[b] - stamp[a];
        double u = (t - stamp[a]) / span;
        double u2 = u * u;
        double u3 = u2 * u;
        double h00 = 2 * u3 - 3 * u2 + 1;
        double h10 = u3 - 2 * u2 + u;
        double h01 = -2 * u3 + 3 * u2;
        double h11 = u3 - u2;
        double k = DT * span;
        ex = h00 * px[a] + h10 * vx[a] * k + h01 * px[b] + h11 * vx[b] * k;
        ey = h00 * py[a] + h10 * vy[a] * k + h01 * py[b] + h11 * vy[b] * k;
        ez = h00 * pz[a] + h10 * vz[a] * k + h01 * pz[b] + h11 * vz[b] * k;
        rot[a].slerp(rot[b], (float) u, eq);
    }

    /**
     * Past the newest sample by {@code ticks}: constant acceleration (from the last two velocities) for the position, and
     * the turn of the last two orientations carried on for the orientation. Limited to {@link #EXTRAPOLATE_MAX}.
     */
    private void extrapolate(int newest, double ticks) {
        double d = Math.min(ticks, EXTRAPOLATE_MAX);
        double s = d * DT;
        double ax = 0;
        double ay = 0;
        double az = 0;
        if (count >= 2) {
            int prev = slot(1);
            double gap = (stamp[newest] - stamp[prev]) * DT;
            if (gap > 0) {
                ax = clampAccel((vx[newest] - vx[prev]) / gap);
                ay = clampAccel((vy[newest] - vy[prev]) / gap);
                az = clampAccel((vz[newest] - vz[prev]) / gap);
            }
        }
        ex = px[newest] + vx[newest] * s + 0.5 * ax * s * s;
        ey = py[newest] + vy[newest] * s + 0.5 * ay * s * s;
        ez = pz[newest] + vz[newest] * s + 0.5 * az * s * s;
        eq.set(rot[newest]);
        if (count >= 2 && d > 0) {
            int prev = slot(1);
            double gap = stamp[newest] - stamp[prev];
            if (gap > 0) {
                // Body-frame turn over the last gap, repeated d / gap times.
                scratchA.set(rot[prev]).conjugate().mul(rot[newest]);
                if (scratchA.w < 0) {
                    scratchA.set(-scratchA.x, -scratchA.y, -scratchA.z, -scratchA.w);
                }
                scratchB.identity().slerp(scratchA, (float) (d / gap), scratchB);
                eq.mul(scratchB).normalize();
            }
        }
    }

    private static double clampAccel(double a) {
        return Math.max(-ACCEL_MAX, Math.min(ACCEL_MAX, a));
    }
}
