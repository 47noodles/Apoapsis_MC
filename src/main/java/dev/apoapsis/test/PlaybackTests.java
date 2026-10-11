package dev.apoapsis.test;

import dev.apoapsis.ship.ShipPlayback;
import dev.apoapsis.test.ApoapsisGameTest.ShipCleanup;
import java.util.Random;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Tests of {@link ShipPlayback}, the client's smoothing of a ship's pose, driven without a client: a synthetic flight
 * (constant acceleration, a steadily quickening turn) is sampled once per server tick, the samples are handed over at
 * jittered client ticks with a stall and a burst in the middle, and the pose each client tick must lie on the flight
 * whatever the arrivals did.
 */
final class PlaybackTests {
    private static final double DT = 0.05;
    private static final double V0 = 60.0;
    private static final double ACCEL = 9.0;
    private static final int TICKS = 240;
    /** The samples of these server ticks all arrive together, after the stall. */
    private static final int STALL_FROM = 100;
    private static final int STALL_TO = 108;

    private PlaybackTests() {}

    /** Flight position (metres up) at server tick {@code k}, which may be fractional. */
    private static double height(double k) {
        double t = k * DT;
        return 100.0 + V0 * t + 0.5 * ACCEL * t * t;
    }

    private static double speed(double k) {
        return V0 + ACCEL * k * DT;
    }

    /** Turn angle about X at server tick {@code k}: quickening, up to a few degrees a tick. */
    private static double angle(double k) {
        return 0.01 * k + 0.0004 * k * k;
    }

    private static Vec3 position(int k) {
        return new Vec3(10.0, height(k), -20.0 + 0.5 * k);
    }

    private static Vec3 velocity(int k) {
        return new Vec3(0.0, speed(k), 0.5 / DT);
    }

    private static Quaternionf orientation(double k) {
        return new Quaternionf().rotationX((float) angle(k));
    }

    /**
     * The pose follows the flight at one server tick per client tick, to a centimetre and two milliradians, whether the
     * samples arrive a tick early or late, in a pair, or after an eight tick stall in a burst.
     */
    static void poseIgnoresArrivalJitter(GameTestHelper helper, ShipCleanup ships) {
        Random rng = new Random(7);
        int[] arrival = new int[TICKS];
        for (int k = 0; k < TICKS; k++) {
            arrival[k] = k + 2 + rng.nextInt(3) - 1;
            if (k >= STALL_FROM && k <= STALL_TO) {
                arrival[k] = STALL_TO + 2;
            }
            if (k > 0) {
                arrival[k] = Math.max(arrival[k], arrival[k - 1]);
            }
        }
        ShipPlayback playback = new ShipPlayback();
        int next = 0;
        int started = -1;
        double tau = 0;
        double worstMetres = 0;
        double worstRadians = 0;
        int worstTick = 0;
        int checked = 0;
        for (int j = 0; j < TICKS + 4; j++) {
            while (next < TICKS && arrival[next] <= j) {
                playback.accept(next, position(next), velocity(next), orientation(next));
                next++;
            }
            if (!playback.isRunning()) {
                continue;
            }
            if (started < 0) {
                started = j;
                tau = -1.0;
            } else {
                playback.tick();
                tau += 1.0;
            }
            if (tau < 0 || tau > TICKS - 2) {
                continue;
            }
            checked++;
            double dy = playback.y() - height(tau);
            if (Math.abs(dy) > worstMetres) {
                worstMetres = Math.abs(dy);
                worstTick = j;
            }
            if (j < STALL_FROM + 2 || j > STALL_TO + 4) {
                Quaternionf got = playback.orientationNow(new Quaternionf());
                worstRadians = Math.max(worstRadians,
                        2.0 * Math.acos(Math.min(1.0, Math.abs(orientation(tau).dot(got)))));
            }
        }
        helper.assertTrue(started >= 0, "the playback never started");
        helper.assertTrue(checked > TICKS - 10, "only " + checked + " ticks were checked");
        helper.assertTrue(worstMetres < 0.01,
                "the pose left the flight by " + worstMetres + " m at client tick " + worstTick);
        helper.assertTrue(worstRadians < 0.002, "the orientation left the flight by " + worstRadians + " rad");
        helper.succeed();
    }

    /**
     * A sample far from where the last one was heading is a teleport: the playback restarts there (accept returns true),
     * and the pose is within a tick of flight of it. A gap in the stamps (a ship that slept) holds the old pose.
     */
    static void teleportsAndSleepRestart(GameTestHelper helper, ShipCleanup ships) {
        ShipPlayback playback = new ShipPlayback();
        Quaternionf q = new Quaternionf();
        boolean first = playback.accept(0, new Vec3(0, 100, 0), new Vec3(0, 10, 0), q);
        helper.assertTrue(first, "the first sample should start the playback");
        for (int k = 1; k <= 10; k++) {
            boolean restarted = playback.accept(k, new Vec3(0, 100 + 0.5 * k, 0), new Vec3(0, 10, 0), q);
            helper.assertTrue(!restarted, "a sample on the flight restarted the playback at tick " + k);
            playback.tick();
        }
        boolean jumped = playback.accept(11, new Vec3(0, 600, 0), new Vec3(0, 10, 0), q);
        helper.assertTrue(jumped, "a sample 500 m off the flight should restart the playback");
        helper.assertTrue(Math.abs(playback.y() - 600) < 1.0,
                "after a teleport the pose is " + playback.y() + ", expected within a tick of 600");

        playback.tick();
        double before = playback.y();
        boolean slept = playback.accept(40, new Vec3(0, before, 0), Vec3.ZERO, q);
        helper.assertTrue(slept, "a 29 tick gap in the stamps should restart the playback");
        helper.assertTrue(Math.abs(playback.y() - before) < 0.6,
                "after a gap the pose moved from " + before + " to " + playback.y() + ", expected it held");
        helper.succeed();
    }
}
