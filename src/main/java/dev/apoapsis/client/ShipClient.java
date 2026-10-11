package dev.apoapsis.client;

import dev.apoapsis.client.ship.EngineParticles;
import dev.apoapsis.client.ship.PilotInput;
import dev.apoapsis.client.ship.PilotKeys;
import dev.apoapsis.client.ship.ShipRenderer;
import dev.apoapsis.ship.ShipEntities;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Pilot input (key mappings, ShipInputPayload sending, drop-key suppression), the ship entity renderer, engine
 * particles. Client only.
 */
public final class ShipClient {
    private ShipClient() {}

    /** Called from {@code ApoapsisClient} with the mod event bus. */
    public static void register(IEventBus modBus) {
        modBus.addListener(ShipClient::onRegisterKeys);
        modBus.addListener(ShipClient::onRegisterRenderers);
        NeoForge.EVENT_BUS.addListener(ShipClient::onClientTickPre);
        NeoForge.EVENT_BUS.addListener(ShipClient::onClientTickPost);
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

    /** After the tick: exhaust particles for ships near the player. Paused games spawn nothing. */
    private static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isPaused()) {
            EngineParticles.tick(mc);
        }
    }
}
