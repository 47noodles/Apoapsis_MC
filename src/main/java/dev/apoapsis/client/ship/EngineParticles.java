package dev.apoapsis.client.ship;

import dev.apoapsis.flight.FlightDimensions;
import dev.apoapsis.grid.ShipParts;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.ship.ShipEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Exhaust for every thruster of each thrusting ship near the local player. In air: a bright core, a smoke trail, and a
 * ground billow when the ship is within {@link #BILLOW_M} of terrain. In vacuum: a wide, short-lived flame with no
 * smoke. Client only.
 */
public final class EngineParticles {
    /** Particles per ship per client tick within {@link #NEAR_M} of the camera, shared across its thrusters. */
    public static final int MAX_PARTICLES_PER_SHIP = 48;
    /** Particles per ship per client tick beyond {@link #NEAR_M} and inside the render distance. */
    public static final int FAR_PARTICLES_PER_SHIP = 8;
    /** Distance from the camera within which a ship gets the full budget, metres. */
    public static final double NEAR_M = 64.0;
    /** Height above terrain within which exhaust billows on the ground, and the probe length. Metres. */
    static final double BILLOW_M = 20.0;
    /** Air density over sea-level density below which the ship counts as being in vacuum. */
    static final double MIN_AIR = 0.01;

    /** Exhaust speed at full throttle, in blocks per tick. */
    private static final double FLAME_SPEED = 0.7;
    private static final double SMOKE_SPEED = 0.35;
    private static final double VACUUM_SPEED = 0.9;

    private EngineParticles() {}

    /** Spawns exhaust for this client tick. Called after the tick, so the particle engine picks it up next. */
    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            return;
        }
        Vec3 cam = mc.gameRenderer.mainCamera().position();
        double reach = mc.options.getEffectiveRenderDistance() * 16.0;
        AABB area = new AABB(cam.x - reach, cam.y - reach, cam.z - reach,
                cam.x + reach, cam.y + reach, cam.z + reach);
        for (ShipEntity ship : level.getEntitiesOfClass(ShipEntity.class, area)) {
            float throttle = ship.getThrottle();
            if (throttle <= 0.0F || ship.fuelKg() <= 0.0) {
                continue;
            }
            int budget = budget(Math.sqrt(ship.distanceToSqr(cam)), reach);
            if (budget > 0) {
                emit(level, ship, throttle, budget);
            }
        }
    }

    /** Particles this ship may spawn this tick: the full budget near the camera, a few further out, none past reach. */
    static int budget(double distance, double reach) {
        if (distance > reach) {
            return 0;
        }
        return distance <= NEAR_M ? MAX_PARTICLES_PER_SHIP : FAR_PARTICLES_PER_SHIP;
    }

    /** One thruster block of a ship: where it is and how it exhausts. */
    private record Thruster(BlockPos pos, ShipParts.ThrusterSpec spec) {}

    /**
     * Spawns the exhaust of one ship, at most {@code budget} particles in all. The budget is split evenly across the
     * thrusters; the remainder goes to a run of thrusters from a random start, so no thruster is always cut off.
     */
    private static void emit(ClientLevel level, ShipEntity ship, float throttle, int budget) {
        List<Thruster> thrusters = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockState> e : ship.getStructure().blocks().entrySet()) {
            ShipParts.ThrusterSpec spec = ShipParts.thruster(e.getValue());
            if (spec != null) {
                thrusters.add(new Thruster(e.getKey(), spec));
            }
        }
        int n = thrusters.size();
        if (n == 0) {
            return;
        }
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Vec3 com = ship.getCenterOfMassLocal();
        Quaternionf q = ship.getOrientation();
        Vec3 origin = ship.position();
        boolean air = airFraction(ship) > MIN_AIR;
        int share = budget / n;
        int extra = budget % n;
        int start = rnd.nextInt(n);
        for (int i = 0; i < n; i++) {
            int b = share + (i < extra ? 1 : 0);
            if (b == 0) {
                continue;
            }
            Thruster thruster = thrusters.get((start + i) % n);
            BlockPos p = thruster.pos();
            Direction exhaust = thruster.spec().exhaust();
            double ex = exhaust.getStepX();
            double ey = exhaust.getStepY();
            double ez = exhaust.getStepZ();
            // Exhaust face: the block centre, half a block out along the exhaust direction (body axes).
            Vector3f face = q.transform(new Vector3f(
                    (float) (p.getX() + 0.5 + 0.5 * ex - com.x),
                    (float) (p.getY() + 0.5 + 0.5 * ey - com.y),
                    (float) (p.getZ() + 0.5 + 0.5 * ez - com.z)));
            Vector3f dir = q.transform(new Vector3f((float) ex, (float) ey, (float) ez));
            double x = origin.x + face.x;
            double y = origin.y + face.y;
            double z = origin.z + face.z;
            if (air) {
                b -= billow(level, ship, x, y, z, throttle, b / 3, rnd);
                plumeAir(level, rnd, x, y, z, dir, throttle, b);
            } else {
                plumeVacuum(level, rnd, x, y, z, dir, throttle, b);
            }
        }
    }

    /** Bright flame core, a few sparks, and a wider smoke trail that drifts behind the ship. */
    private static void plumeAir(ClientLevel level, ThreadLocalRandom rnd, double x, double y, double z, Vector3f dir,
            float throttle, int budget) {
        int left = budget;
        int core = Math.min(left, (int) (throttle * 4.0 + rnd.nextDouble()));
        double coreSpeed = FLAME_SPEED * (0.5 + 0.5 * throttle);
        for (int k = 0; k < core; k++) {
            level.addParticle(ParticleTypes.FLAME,
                    x + scatter(rnd, 0.2), y + scatter(rnd, 0.2), z + scatter(rnd, 0.2),
                    dir.x * coreSpeed, dir.y * coreSpeed, dir.z * coreSpeed);
        }
        left -= core;
        if (left > 0 && rnd.nextDouble() < 0.3 * throttle) {
            level.addParticle(ParticleTypes.END_ROD,
                    x + scatter(rnd, 0.3), y + scatter(rnd, 0.3), z + scatter(rnd, 0.3),
                    dir.x * coreSpeed * 1.5, dir.y * coreSpeed * 1.5, dir.z * coreSpeed * 1.5);
            left--;
        }
        int smoke = Math.min(left, (int) (throttle * 6.0 + rnd.nextDouble()));
        double smokeSpeed = SMOKE_SPEED * (0.5 + 0.5 * throttle);
        for (int k = 0; k < smoke; k++) {
            // Sideways scatter on the velocity makes the trail spread as it rises and drifts behind the ship.
            level.addParticle(ParticleTypes.LARGE_SMOKE,
                    x + scatter(rnd, 0.5), y + scatter(rnd, 0.5), z + scatter(rnd, 0.5),
                    dir.x * smokeSpeed + scatter(rnd, 0.1), dir.y * smokeSpeed + scatter(rnd, 0.1),
                    dir.z * smokeSpeed + scatter(rnd, 0.1));
        }
    }

    /** A wide, short-lived flame with no smoke, for the vacuum of orbit and Pale. */
    private static void plumeVacuum(ClientLevel level, ThreadLocalRandom rnd, double x, double y, double z,
            Vector3f dir, float throttle, int budget) {
        int flames = Math.min(budget, (int) (throttle * 5.0 + rnd.nextDouble()));
        double speed = VACUUM_SPEED * (0.5 + 0.5 * throttle);
        for (int k = 0; k < flames; k++) {
            level.addParticle(ParticleTypes.SMALL_FLAME,
                    x + scatter(rnd, 0.8), y + scatter(rnd, 0.8), z + scatter(rnd, 0.8),
                    dir.x * speed + scatter(rnd, 0.3), dir.y * speed + scatter(rnd, 0.3),
                    dir.z * speed + scatter(rnd, 0.3));
        }
    }

    /**
     * Smoke and poof on the ground in a ring around the point under the exhaust, moving outward. The count rises as the
     * thruster nears the ground. Returns the particles spawned, at most {@code max}.
     */
    private static int billow(ClientLevel level, Entity ship, double x, double y, double z, float throttle, int max,
            ThreadLocalRandom rnd) {
        if (max <= 0) {
            return 0;
        }
        BlockHitResult hit = groundBelow(level, ship, new Vec3(x, y, z));
        if (hit == null) {
            return 0;
        }
        Vec3 g = hit.getLocation();
        double nearness = Math.max(0.0, 1.0 - (y - g.y) / BILLOW_M);
        int count = Math.min(max, (int) (throttle * (1.0 + 4.0 * nearness) + rnd.nextDouble()));
        double speed = 0.15 + 0.2 * throttle;
        for (int k = 0; k < count; k++) {
            double a = rnd.nextDouble() * Math.PI * 2.0;
            double r = 1.0 + 2.0 * rnd.nextDouble();
            double cx = Math.cos(a);
            double cz = Math.sin(a);
            level.addParticle(rnd.nextBoolean() ? ParticleTypes.LARGE_SMOKE : ParticleTypes.POOF,
                    g.x + cx * r, g.y + 0.1, g.z + cz * r,
                    cx * speed, 0.02 + 0.04 * rnd.nextDouble(), cz * speed);
        }
        return count;
    }

    /** The first terrain block hit straight down from {@code from} within {@link #BILLOW_M}, or null. Client only. */
    static BlockHitResult groundBelow(ClientLevel level, Entity ship, Vec3 from) {
        Vec3 to = from.add(0.0, -BILLOW_M, 0.0);
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                ship));
        return hit.getType() == HitResult.Type.MISS ? null : hit;
    }

    /** Air density over sea-level density at the ship: 0 in orbit and on Pale, else from the body's atmosphere. */
    static double airFraction(ShipEntity ship) {
        ResourceKey<Level> dim = ship.level().dimension();
        if (FlightDimensions.ORBIT.equals(dim) || FlightDimensions.PALE.equals(dim)) {
            return 0.0;
        }
        CelestialBody sys = FlightDimensions.system(FlightDimensions.surfaceBody(dim));
        if (sys.seaLevelDensity <= 0.0) {
            return 0.0;
        }
        double h = ship.getY() - FlightDimensions.seaLevel(dim);
        return sys.density(Math.max(h, 0.0)) / sys.seaLevelDensity;
    }

    /** A uniform random offset in [-width/2, width/2). */
    private static double scatter(ThreadLocalRandom rnd, double width) {
        return (rnd.nextDouble() - 0.5) * width;
    }
}
