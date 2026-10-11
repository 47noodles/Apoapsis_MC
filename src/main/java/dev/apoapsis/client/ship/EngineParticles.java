package dev.apoapsis.client.ship;

import dev.apoapsis.grid.ShipParts;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Exhaust flame and smoke from every thruster of each thrusting ship near the local player. Client only. */
public final class EngineParticles {
    /** Ships within this distance of the local player emit exhaust, in metres. */
    public static final double RANGE = 128.0;

    /** Most exhaust particles one ship spawns per client tick, shared across its thrusters. */
    public static final int MAX_PARTICLES_PER_SHIP = 24;

    /** Exhaust speed at full throttle, in blocks per tick. */
    private static final double FLAME_SPEED = 0.7;
    private static final double SMOKE_SPEED = 0.35;

    private EngineParticles() {}

    /** Spawns exhaust for this client tick. Called after the tick, so the particle engine picks it up next. */
    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            return;
        }
        Vec3 center = mc.player.position();
        AABB area = new AABB(center.x - RANGE, center.y - RANGE, center.z - RANGE,
                center.x + RANGE, center.y + RANGE, center.z + RANGE);
        for (ShipEntity ship : level.getEntitiesOfClass(ShipEntity.class, area)) {
            float throttle = ship.getThrottle();
            if (throttle <= 0.0F || ship.distanceToSqr(center) > RANGE * RANGE) {
                continue;
            }
            emit(level, ship, throttle);
        }
    }

    /** One thruster block of a ship: where it is and how it exhausts. */
    private record Thruster(BlockPos pos, ShipParts.ThrusterSpec spec) {}

    /**
     * Spawns the exhaust of one ship, at most {@link #MAX_PARTICLES_PER_SHIP} particles in all. The budget is split
     * evenly across the thrusters; the remainder goes to a run of thrusters from a random start, so no thruster is
     * always the one cut off when there are more thrusters than particles.
     */
    private static void emit(ClientLevel level, ShipEntity ship, float throttle) {
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
        int share = MAX_PARTICLES_PER_SHIP / n;
        int extra = MAX_PARTICLES_PER_SHIP % n;
        int start = rnd.nextInt(n);
        for (int i = 0; i < n; i++) {
            int budget = share + (i < extra ? 1 : 0);
            if (budget == 0) {
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

            int flames = Math.min(budget, (int) (throttle * 2.0 + rnd.nextDouble()));
            double flameSpeed = FLAME_SPEED * (0.5 + 0.5 * throttle);
            for (int k = 0; k < flames; k++) {
                level.addParticle(ParticleTypes.FLAME,
                        x + jitter(rnd), y + jitter(rnd), z + jitter(rnd),
                        dir.x * flameSpeed, dir.y * flameSpeed, dir.z * flameSpeed);
            }
            int smoke = Math.min(budget - flames, (int) (throttle * 0.5 + rnd.nextDouble()));
            double smokeSpeed = SMOKE_SPEED * (0.5 + 0.5 * throttle);
            for (int k = 0; k < smoke; k++) {
                level.addParticle(ParticleTypes.SMOKE,
                        x + jitter(rnd), y + jitter(rnd), z + jitter(rnd),
                        dir.x * smokeSpeed, dir.y * smokeSpeed, dir.z * smokeSpeed);
            }
        }
    }

    private static double jitter(ThreadLocalRandom rnd) {
        return (rnd.nextDouble() - 0.5) * 0.2;
    }
}
