package dev.apoapsis.client.ship;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.ship.ShipEntity;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * A looping engine roar for each thrusting ship near the local player. It follows its ship, scales with throttle, and
 * is much quieter in vacuum, where it is heard through the hull. It fades in and out and stops when the ship is gone.
 * Client only.
 */
@EventBusSubscriber(modid = Apoapsis.MODID, value = Dist.CLIENT)
public final class EngineSound {
    /** Volume of a full-throttle roar in air. Above 1, so it carries further than a vanilla sound: about 48 blocks. */
    private static final float AIR_VOLUME = 3.0F;
    /** Volume factor in vacuum. */
    private static final float VACUUM_VOLUME = 0.15F;
    /** Fade change per client tick: 0.1 is about half a second from silence to full. */
    private static final float FADE_PER_TICK = 0.1F;
    /** Playback pitch. Below 1 reads lower and heavier. */
    private static final float PITCH = 0.5F;
    /** Ships beyond this distance from the local player get no roar, metres. */
    private static final double RANGE = 128.0;

    /** The active roar of each ship, by entity id. */
    private static final Map<Integer, Rumble> RUMBLES = new HashMap<>();

    private EngineSound() {}

    /** Starts, updates and stops the roars once per client tick. Paused games keep their roars as they are. */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            stopAll();
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        Vec3 cam = mc.gameRenderer.mainCamera().position();
        AABB area = new AABB(cam.x - RANGE, cam.y - RANGE, cam.z - RANGE, cam.x + RANGE, cam.y + RANGE, cam.z + RANGE);
        Map<Integer, ShipEntity> thrusting = new HashMap<>();
        for (ShipEntity ship : mc.level.getEntitiesOfClass(ShipEntity.class, area)) {
            if (ship.getThrottle() > 0.0F && ship.fuelKg() > 0.0) {
                thrusting.put(ship.getId(), ship);
            }
        }
        Iterator<Map.Entry<Integer, Rumble>> it = RUMBLES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Rumble> e = it.next();
            if (e.getValue().isStopped()) {
                it.remove();
                continue;
            }
            e.getValue().update(thrusting.remove(e.getKey()));
        }
        for (ShipEntity ship : thrusting.values()) {
            Rumble rumble = new Rumble(ship);
            RUMBLES.put(ship.getId(), rumble);
            mc.getSoundManager().play(rumble);
        }
    }

    private static void stopAll() {
        for (Rumble rumble : RUMBLES.values()) {
            rumble.halt();
        }
        RUMBLES.clear();
    }

    /** One ship's roar. Volume and position follow the ship; it stops once faded out, or at once if the ship is gone. */
    private static final class Rumble extends AbstractTickableSoundInstance {
        private ShipEntity ship;
        private boolean on = true;
        private boolean vacuum;
        private float throttle;
        private float fade;

        Rumble(ShipEntity ship) {
            super(SoundEvents.FIRE_AMBIENT, SoundSource.AMBIENT, RandomSource.create());
            this.looping = true;
            this.pitch = PITCH;
            this.ship = ship;
            update(ship);
        }

        /** Sets the ship this tick: null means it is no longer thrusting, so the roar fades out. */
        void update(ShipEntity s) {
            if (s == null) {
                on = false;
                return;
            }
            ship = s;
            on = true;
            throttle = s.getThrottle();
            vacuum = EngineParticles.airFraction(s) <= EngineParticles.MIN_AIR;
        }

        /** Stops now, without a fade. */
        void halt() {
            stop();
        }

        @Override
        public void tick() {
            if (ship == null || ship.isRemoved()) {
                halt();
                return;
            }
            fade = Mth.clamp(fade + (on ? FADE_PER_TICK : -FADE_PER_TICK), 0.0F, 1.0F);
            if (!on && fade <= 0.0F) {
                halt();
                return;
            }
            Vec3 p = ship.position();
            this.x = p.x;
            this.y = p.y;
            this.z = p.z;
            this.volume = AIR_VOLUME * throttle * fade * (vacuum ? VACUUM_VOLUME : 1.0F);
        }
    }
}
