package dev.apoapsis.client;

import dev.apoapsis.client.ship.CockpitCamera;
import dev.apoapsis.client.ship.EngineParticles;
import dev.apoapsis.client.ship.PilotInput;
import dev.apoapsis.client.ship.PilotKeys;
import dev.apoapsis.client.ship.ReentryParticles;
import dev.apoapsis.client.ship.ShipRenderCache;
import dev.apoapsis.client.ship.ShipRenderer;
import dev.apoapsis.ship.ShipEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Pilot input (key mappings, ShipInputPayload sending, drop-key suppression), the ship entity renderer, engine
 * particles, and the ship render cache. Client only.
 */
public final class ShipClient {
    /** Client ticks between purges of the ship render cache: once a second. */
    private static final int PURGE_INTERVAL_TICKS = 20;

    private static int ticksSincePurge;

    private ShipClient() {}

    /** Called from {@code ApoapsisClient} with the mod event bus. */
    public static void register(IEventBus modBus) {
        modBus.addListener(ShipClient::onRegisterKeys);
        modBus.addListener(ShipClient::onRegisterRenderers);
        NeoForge.EVENT_BUS.addListener(ShipClient::onClientTickPre);
        NeoForge.EVENT_BUS.addListener(ShipClient::onClientTickPost);
        NeoForge.EVENT_BUS.addListener(ShipClient::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(ShipClient::onLevelUnload);
        CockpitCamera.register();
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        PilotKeys.registerTo(event);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ShipEntities.SHIP.get(), ShipRenderer::new);
    }

    /** Before vanilla handles keys: the system map key, then the pilot's input for this tick. */
    private static void onClientTickPre(ClientTickEvent.Pre event) {
        if (PilotKeys.pressed(PilotKeys.SYSTEM_MAP)) {
            MapClient.openMap();
        }
        PilotInput.tick(Minecraft.getInstance());
    }

    /**
     * After the tick: the render cache's tick count and its once-a-second purge, then exhaust particles for ships
     * near the player. Paused games spawn nothing.
     */
    private static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ShipRenderCache.advanceClientTick();
        if (mc.level != null && ++ticksSincePurge >= PURGE_INTERVAL_TICKS) {
            ticksSincePurge = 0;
            ShipRenderCache.purge(mc.level);
        }
        if (!mc.isPaused()) {
            EngineParticles.tick(mc);
            ReentryParticles.tick(mc);
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ShipRenderCache.clear();
        CockpitCamera.reset();
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel) {
            ShipRenderCache.clear();
            CockpitCamera.onLevelUnload();
        }
    }
}
