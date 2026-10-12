package dev.apoapsis.client.ship;

import dev.apoapsis.client.ship.fx.FxLayers;
import dev.apoapsis.client.ship.fx.FxParticle;
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
 * Exhaust for every thruster of each thrusting ship near the local player: soft additive sprites that carry the ship's
 * own motion (see {@link dev.apoapsis.client.ship.fx.FxParticle}), a hot white-blue core running to orange, widening
 * and dimming as the air thins; a light ground dust within {@link #BILLOW_M} of terrain. In vacuum only a faint
 * blue-white cone, no smoke. Client only.
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
    /** Share of the ship's own motion per tick that exhaust sprites start with. */
    private static final double CARRY = 0.85;
    /** Vacuum flames closer than this to the camera are not spawned, blocks. */
    private static final double CLEAR_M = 6.0;
    /** Vacuum flames heading into a cone of this half-angle toward the camera, within {@link #CONE_M}, are not spawned. */
    private static final double CONE_COS = Math.cos(Math.toRadians(25.0));
    private static final double CONE_M = 14.0;

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
                emit(level, ship, throttle, budget, cam);
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
    private static void emit(ClientLevel level, ShipEntity ship, float throttle, int budget, Vec3 cam) {
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
        // The plume goes along with a ship that flies through the level: its sprites start with most of the ship's own
        // velocity. In the orbit dimension the ship stands still in the level, so there is nothing to carry.
        Vec3 carry = FlightDimensions.ORBIT.equals(level.dimension())
                ? Vec3.ZERO : ship.getVelocity().scale(CARRY / 20.0);
        double air = airFraction(ship);
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
            if (air > MIN_AIR) {
                b -= billow(level, ship, x, y, z, throttle, b / 3, rnd);
            }
            plume(level, rnd, x, y, z, dir, throttle, air, b, cam, carry);
        }
    }

    /**
     * The hot plume of one thruster: a small white glow at the nozzle, and plume sprites running from white-blue through
     * orange to dull red along the exhaust. As the air thins the plume widens and dims (it is nearly invisible in
     * vacuum, a faint blue-white cone), and the only smoke is a thin pale trail low in the atmosphere. Sprites that
     * would sit too close to the camera, or head at it, are skipped so the plume never fills the view.
     */
    private static void plume(ClientLevel level, ThreadLocalRandom rnd, double x, double y, double z, Vector3f dir,
            float throttle, double air, int budget, Vec3 cam, Vec3 carry) {
        if (budget <= 0) {
            return;
        }
        float open = 1.0F - (float) Math.sqrt(Math.min(1.0, air));
        float dim = 1.0F - 0.45F * open;
        double speed = FLAME_SPEED * (0.55 + 0.45 * throttle) * (1.0 + 0.7 * open);
        float drag = 0.94F + 0.06F * open;
        int life = 7 - (int) (open * 1.5F);
        var plumeSprite = FxLayers.sprite(FxLayers.PLUME);
        int left = budget;
        int core = Math.min(left, (int) (throttle * 3.0 + rnd.nextDouble()) + 2);
        for (int k = 0; k < core; k++) {
            // Spread over this tick's travel of the ship, so a fast ship's plume has no gap behind the nozzle.
            double ahead = rnd.nextDouble() * 1.2;
            double px = x + scatter(rnd, 0.3) + carry.x * ahead;
            double py = y + scatter(rnd, 0.3) + carry.y * ahead;
            double pz = z + scatter(rnd, 0.3) + carry.z * ahead;
            if (nearCamera(cam, px, py, pz, dir)) {
                continue;
            }
            double lateral = 0.03 + 0.10 * open;
            Minecraft.getInstance().particleEngine.add(new FxParticle(level, px, py, pz, plumeSprite, FxLayers.additive())
                    .motion(carry.x + dir.x * speed + scatter(rnd, lateral), carry.y + dir.y * speed + scatter(rnd, lateral),
                            carry.z + dir.z * speed + scatter(rnd, lateral), drag, life)
                    .sizes(0.9F + 0.3F * throttle, 1.3F + 2.4F * open + 0.6F * throttle)
                    .colors(0.85F, 0.93F, 1.0F, mix(1.0F, 0.72F, open), mix(0.62F, 0.8F, open), mix(0.2F, 1.0F, open),
                            mix(0.55F, 0.3F, open), mix(0.12F, 0.25F, open), mix(0.03F, 0.6F, open))
                    .alpha(0.85F * dim * (0.45F + 0.55F * throttle), 1.3F));
        }
        left -= core;
        if (left > 0 && !nearCamera(cam, x, y, z, dir)) {
            Minecraft.getInstance().particleEngine.add(new FxParticle(level, x + carry.x * 0.6, y + carry.y * 0.6, z + carry.z * 0.6,
                    FxLayers.sprite(FxLayers.GLOW), FxLayers.additive())
                    .motion(carry.x + dir.x * 0.1, carry.y + dir.y * 0.1, carry.z + dir.z * 0.1, 1.0F, 3)
                    .sizes(1.4F, 1.0F)
                    .colors(1.0F, 0.97F, 0.9F, 1.0F, 0.8F, 0.45F, 1.0F, 0.5F, 0.2F)
                    .alpha(0.75F * dim * throttle, 1.0F));
            left--;
        }
        if (left > 0 && air > 0.2 && throttle > 0.4F && rnd.nextDouble() < 0.35) {
            double s = speed * (1.2 + rnd.nextDouble());
            Minecraft.getInstance().particleEngine.add(new FxParticle(level, x, y, z,
                    FxLayers.sprite(FxLayers.STREAK), FxLayers.additive())
                    .streak()
                    .motion(carry.x + dir.x * s + scatter(rnd, 0.25), carry.y + dir.y * s + scatter(rnd, 0.25),
                            carry.z + dir.z * s + scatter(rnd, 0.25), 0.96F, 6)
                    .sizes(0.28F, 0.28F)
                    .colors(1.0F, 0.95F, 0.8F, 1.0F, 0.6F, 0.15F, 0.7F, 0.2F, 0.05F)
                    .alpha(0.9F, 1.0F));
            left--;
        }
        if (left > 0 && air > 0.5 && rnd.nextDouble() < 0.3 * throttle) {
            // Thin pale exhaust haze, only low in the atmosphere.
            Minecraft.getInstance().particleEngine.add(new FxParticle(level, x + scatter(rnd, 0.5), y + scatter(rnd, 0.5),
                    z + scatter(rnd, 0.5), FxLayers.sprite(FxLayers.PUFF), FxLayers.DUST)
                    .spin()
                    .motion(carry.x * 0.5 + dir.x * speed * 0.5, carry.y * 0.5 + dir.y * speed * 0.5,
                            carry.z * 0.5 + dir.z * speed * 0.5, 0.88F, 18)
                    .sizes(0.5F, 1.6F)
                    .colors(0.9F, 0.9F, 0.92F, 0.8F, 0.8F, 0.82F, 0.7F, 0.7F, 0.72F)
                    .alpha(0.16F, 1.0F));
        }
    }

    /**
     * True when a vacuum flame at (px, py, pz) with exhaust direction {@code dir} would be too close to the camera: within
     * {@link #CLEAR_M}, or within {@link #CONE_M} and heading inside the cone toward the camera.
     */
    private static boolean nearCamera(Vec3 cam, double px, double py, double pz, Vector3f dir) {
        double dx = cam.x - px;
        double dy = cam.y - py;
        double dz = cam.z - pz;
        double d2 = dx * dx + dy * dy + dz * dz;
        if (d2 <= CLEAR_M * CLEAR_M) {
            return true;
        }
        if (d2 > CONE_M * CONE_M) {
            return false;
        }
        double d = Math.sqrt(d2);
        // dir is a unit vector (a rotated axis), so this is the cosine of the angle between heading and camera.
        return (dir.x * dx + dir.y * dy + dir.z * dz) / d > CONE_COS;
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
            Minecraft.getInstance().particleEngine.add(new FxParticle(level, g.x + cx * r, g.y + 0.15, g.z + cz * r,
                    FxLayers.sprite(FxLayers.PUFF), FxLayers.DUST)
                    .spin()
                    .motion(cx * speed, 0.01 + 0.03 * rnd.nextDouble(), cz * speed, 0.9F, 20 + rnd.nextInt(8))
                    .sizes(0.7F, 2.4F)
                    .colors(0.82F, 0.78F, 0.7F, 0.78F, 0.75F, 0.7F, 0.74F, 0.72F, 0.68F)
                    .alpha((float) (0.14 + 0.2 * nearness), 1.0F));
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

    private static float mix(float from, float to, float t) {
        return from + (to - from) * t;
    }

    /** A uniform random offset in [-width/2, width/2). */
    private static double scatter(ThreadLocalRandom rnd, double width) {
        return (rnd.nextDouble() - 0.5) * width;
    }
}
