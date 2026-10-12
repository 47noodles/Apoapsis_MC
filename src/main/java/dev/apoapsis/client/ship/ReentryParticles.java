package dev.apoapsis.client.ship;

import dev.apoapsis.client.ship.ReentryState.Heat;
import dev.apoapsis.client.ship.fx.FxLayers;
import dev.apoapsis.client.ship.fx.FxParticle;
import dev.apoapsis.flight.FlightDimensions;
import dev.apoapsis.ship.ShipEntity;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Re-entry streaks for ships moving fast through air: sparse additive streaks and sparks on the windward faces of the hull, scaled by
 * {@link ReentryState.Heat#intensity()}. In a surface dimension the particles hang in still air and the ship flies out
 * through them, so they trail behind; in the orbit dimension they stream aft. Spawns and velocities that would reach the
 * camera are skipped, so the glow never fills the view. Client only.
 */
public final class ReentryParticles {
    /** Particles per ship per client tick within {@link EngineParticles#NEAR_M} of the camera, at full heating. */
    private static final int MAX_PER_SHIP = 6;
    /** Particles per ship per client tick beyond {@link EngineParticles#NEAR_M}, at full heating. */
    private static final int FAR_PER_SHIP = 2;
    /** Spawn points closer than this to the camera are skipped, blocks. */
    private static final double CLEAR_M = 8.0;
    /** Particles heading into a cone of this half-angle toward the camera, within {@link #CONE_M}, are skipped. */
    private static final double CONE_COS = Math.cos(Math.toRadians(25.0));
    private static final double CONE_M = 20.0;
    /** How far ahead of the windward block a particle starts, blocks. */
    private static final double AHEAD_M = 0.55;
    /** Width of the random spawn offset, blocks: +-0.4. */
    private static final double SPAWN_SCATTER_M = 0.8;
    /** Width of the random velocity offset, blocks per tick. */
    private static final double VELOCITY_SCATTER = 0.03;
    /** Ship speed at which the particles stream aft at one block per tick, m/s. */
    private static final double AFT_FULL_MPS = 40.0;
    /** Attempts to find a windward block before a particle is skipped. */
    private static final int PICK_TRIES = 3;
    /** Client ticks before a ship's block list is read again. */
    private static final long REBUILD_TICKS = 10;

    /** Block positions of one ship, and the client tick they were read on. */
    private record Blocks(BlockPos[] pos, long tick) {}

    private static final Map<ShipEntity, Blocks> BLOCKS = new WeakHashMap<>();
    private static long ticks;

    private ReentryParticles() {}

    /** Spawns re-entry glow for this client tick. Called after the tick, so the particle engine picks it up next. */
    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            return;
        }
        ticks++;
        Vec3 cam = mc.gameRenderer.mainCamera().position();
        double reach = mc.options.getEffectiveRenderDistance() * 16.0;
        AABB area = new AABB(cam.x - reach, cam.y - reach, cam.z - reach,
                cam.x + reach, cam.y + reach, cam.z + reach);
        for (ShipEntity ship : level.getEntitiesOfClass(ShipEntity.class, area)) {
            Heat h = ReentryState.of(ship);
            if (h.intensity() <= 0f) {
                continue;
            }
            double distance = Math.sqrt(ship.distanceToSqr(cam));
            int budget = budget(h.intensity(), distance, reach);
            if (budget > 0) {
                emit(level, ship, h, budget, cam);
            }
        }
    }

    /** Particles this ship may spawn this tick: scaled by heating, the full share near the camera, none past reach. */
    static int budget(float intensity, double distance, double reach) {
        if (distance > reach) {
            return 0;
        }
        int full = distance <= EngineParticles.NEAR_M ? MAX_PER_SHIP : FAR_PER_SHIP;
        return Math.min(MAX_PER_SHIP, Math.round(intensity * full));
    }

    /** Spawns up to {@code n} glow particles on the windward side of one ship. */
    private static void emit(ClientLevel level, ShipEntity ship, Heat h, int n, Vec3 cam) {
        BlockPos[] pos = blocksOf(ship);
        if (pos.length == 0) {
            return;
        }
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Vec3 com = ship.getCenterOfMassLocal();
        Quaternionf q = ship.getOrientation();
        Vec3 origin = ship.position();
        Vec3 wind = h.wind();
        boolean orbit = FlightDimensions.ORBIT.equals(level.dimension());
        double spread = orbit ? 0.0 : h.speed() / 20.0;
        double aft = orbit ? -Math.min(h.speed() / AFT_FULL_MPS, 1.0) : 0.0;
        for (int i = 0; i < n; i++) {
            Vector3f off = windwardOffset(pos, q, com, wind, rnd);
            if (off == null) {
                continue;
            }
            double x = origin.x + off.x + wind.x * AHEAD_M - wind.x * spread * rnd.nextDouble()
                    + scatter(rnd, SPAWN_SCATTER_M);
            double y = origin.y + off.y + wind.y * AHEAD_M - wind.y * spread * rnd.nextDouble()
                    + scatter(rnd, SPAWN_SCATTER_M);
            double z = origin.z + off.z + wind.z * AHEAD_M - wind.z * spread * rnd.nextDouble()
                    + scatter(rnd, SPAWN_SCATTER_M);
            double vx = wind.x * aft + scatter(rnd, VELOCITY_SCATTER);
            double vy = wind.y * aft + scatter(rnd, VELOCITY_SCATTER);
            double vz = wind.z * aft + scatter(rnd, VELOCITY_SCATTER);
            if (nearCamera(cam, x, y, z, vx, vy, vz)) {
                continue;
            }
            spawn(level, rnd, x, y, z, wind, h.intensity(), vx, vy, vz);
        }
    }

    /**
     * A random block's centre relative to the centre of mass, in world axes, if it faces the wind (its offset has a
     * non-negative dot product with {@code wind}). Tries {@link #PICK_TRIES} blocks, else returns null.
     */
    private static Vector3f windwardOffset(BlockPos[] pos, Quaternionf q, Vec3 com, Vec3 wind, ThreadLocalRandom rnd) {
        for (int t = 0; t < PICK_TRIES; t++) {
            BlockPos p = pos[rnd.nextInt(pos.length)];
            Vector3f off = q.transform(new Vector3f(
                    (float) (p.getX() + 0.5 - com.x),
                    (float) (p.getY() + 0.5 - com.y),
                    (float) (p.getZ() + 0.5 - com.z)));
            if (off.x * wind.x + off.y * wind.y + off.z * wind.z >= 0.0) {
                return off;
            }
        }
        return null;
    }

    /**
     * One streak or spark: additive, white-hot to orange to dull red, drifting aft along the wind so a streak lines up
     * with the flow. Hotter heating gives brighter and longer streaks.
     */
    private static void spawn(ClientLevel level, ThreadLocalRandom rnd, double x, double y, double z, Vec3 wind,
            float intensity, double vx, double vy, double vz) {
        boolean streak = rnd.nextDouble() < 0.7;
        double drift = 0.2 + 0.3 * intensity;
        FxParticle p = new FxParticle(level, x, y, z, FxLayers.sprite(streak ? FxLayers.STREAK : FxLayers.SPARK),
                FxLayers.additive())
                .motion(vx - wind.x * drift, vy - wind.y * drift, vz - wind.z * drift, 0.97F,
                        8 + rnd.nextInt(6))
                .colors(1.0F, 0.95F, 0.85F, 1.0F, 0.55F + 0.2F * intensity, 0.15F, 0.6F, 0.12F, 0.03F)
                .alpha(0.45F + 0.5F * intensity, 1.2F);
        if (streak) {
            p.streak().sizes(0.5F + 0.5F * intensity, 0.35F);
        } else {
            p.sizes(0.14F, 0.05F);
        }
        Minecraft.getInstance().particleEngine.add(p);
    }

    /**
     * True when a particle at (x, y, z) moving (vx, vy, vz) is within {@link #CLEAR_M} of the camera, or within
     * {@link #CONE_M} and heading into the cone toward it.
     */
    private static boolean nearCamera(Vec3 cam, double x, double y, double z, double vx, double vy, double vz) {
        double dx = cam.x - x;
        double dy = cam.y - y;
        double dz = cam.z - z;
        double d2 = dx * dx + dy * dy + dz * dz;
        if (d2 <= CLEAR_M * CLEAR_M) {
            return true;
        }
        if (d2 > CONE_M * CONE_M) {
            return false;
        }
        double v2 = vx * vx + vy * vy + vz * vz;
        if (v2 < 1e-9) {
            return false;
        }
        double cos = (vx * dx + vy * dy + vz * dz) / (Math.sqrt(v2) * Math.sqrt(d2));
        return cos > CONE_COS;
    }

    /** The ship's block positions, re-read every {@link #REBUILD_TICKS} or when the block count changes. */
    private static BlockPos[] blocksOf(ShipEntity ship) {
        int size = ship.getStructure().blocks().size();
        Blocks c = BLOCKS.get(ship);
        if (c == null || c.pos().length != size || ticks - c.tick() >= REBUILD_TICKS) {
            c = new Blocks(ship.getStructure().blocks().keySet().toArray(new BlockPos[0]), ticks);
            BLOCKS.put(ship, c);
        }
        return c.pos();
    }

    /** A uniform random offset in [-width/2, width/2). */
    private static double scatter(ThreadLocalRandom rnd, double width) {
        return (rnd.nextDouble() - 0.5) * width;
    }
}
