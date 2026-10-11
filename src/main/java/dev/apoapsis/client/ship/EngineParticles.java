package dev.apoapsis.client.ship;

import dev.apoapsis.grid.ShipParts;
import dev.apoapsis.ship.ShipEntity;
import dev.apoapsis.ship.ShipStructure;
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

    private static void emit(ClientLevel level, ShipEntity ship, float throttle) {
        ShipStructure structure = ship.getStructure();
        Vec3 com = structure.centerOfMassLocal();
        Quaternionf q = ship.getOrientation();
        Vec3 origin = ship.position();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (Map.Entry<BlockPos, BlockState> e : structure.blocks().entrySet()) {
            ShipParts.ThrusterSpec spec = ShipParts.thruster(e.getValue());
            if (spec == null) {
                continue;
            }
            BlockPos p = e.getKey();
            Direction exhaust = spec.exhaust();
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

            int flames = (int) (throttle * 2.0 + rnd.nextDouble());
            for (int i = 0; i < flames; i++) {
                double speed = FLAME_SPEED * (0.5 + 0.5 * throttle);
                level.addParticle(ParticleTypes.FLAME,
                        x + jitter(rnd), y + jitter(rnd), z + jitter(rnd),
                        dir.x * speed, dir.y * speed, dir.z * speed);
            }
            int smoke = (int) (throttle * 0.5 + rnd.nextDouble());
            for (int i = 0; i < smoke; i++) {
                double speed = SMOKE_SPEED * (0.5 + 0.5 * throttle);
                level.addParticle(ParticleTypes.SMOKE,
                        x + jitter(rnd), y + jitter(rnd), z + jitter(rnd),
                        dir.x * speed, dir.y * speed, dir.z * speed);
            }
        }
    }

    private static double jitter(ThreadLocalRandom rnd) {
        return (rnd.nextDouble() - 0.5) * 0.2;
    }
}
