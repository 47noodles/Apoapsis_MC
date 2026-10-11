package dev.apoapsis.client.ship;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.client.ClientState;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Engine rumble in the camera while the local player rides a thrusting ship. At LOW priority it adds to the angles
 * CockpitCamera set at NORMAL. The engines alone shake by at most 0.6 degrees, scaled by throttle and air density;
 * within 20 blocks of terrain up to 0.4 degrees more. Nothing in vacuum. Client only.
 */
@EventBusSubscriber(modid = Apoapsis.MODID, value = Dist.CLIENT)
public final class LaunchShake {
    /** Largest shake from the engines alone, degrees, at full throttle in sea-level air. */
    private static final double ENGINE_DEG = 0.6;
    /** Extra shake at the ground, degrees, at full throttle in sea-level air. */
    private static final double GROUND_DEG = 0.4;

    private LaunchShake() {}

    /** Adds the shake to the yaw and pitch CockpitCamera already set, so it runs after it. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !(mc.player.getVehicle() instanceof ShipEntity ship)) {
            return;
        }
        float throttle = ship.getThrottle();
        double air = EngineParticles.airFraction(ship);
        if (throttle <= 0.0F || ship.fuelKg() <= 0.0 || air <= EngineParticles.MIN_AIR) {
            return;
        }
        double nearGround = 0.0;
        BlockHitResult ground = EngineParticles.groundBelow(mc.level, ship, ship.position());
        if (ground != null) {
            nearGround = Math.max(0.0, 1.0 - (ship.getY() - ground.getLocation().y) / EngineParticles.BILLOW_M);
        }
        double amp = throttle * Math.min(1.0, air) * (ENGINE_DEG + GROUND_DEG * nearGround);
        double t = (ClientState.clientTicks + event.getPartialTick()) / 20.0;
        event.setYaw(event.getYaw() + (float) (amp * wobble(t, 0.0)));
        event.setPitch(event.getPitch() + (float) (amp * wobble(t, 1.0)));
    }

    /** Smooth noise in [-1, 1]: three sines at unrelated frequencies, phase-shifted per axis. */
    static double wobble(double t, double axis) {
        double p = axis * 1.7;
        return (Math.sin(2.0 * Math.PI * 0.9 * t + p)
                + 0.5 * Math.sin(2.0 * Math.PI * 2.3 * t + 2.0 * p)
                + 0.25 * Math.sin(2.0 * Math.PI * 4.1 * t + 3.0 * p)) / 1.75;
    }
}
