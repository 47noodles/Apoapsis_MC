package dev.apoapsis.client;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.client.map.FlightHud;
import dev.apoapsis.client.map.MapScreen;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * The full-screen system map (the System map key) and the flight HUD layer. Also keeps the client's tick count and
 * clears the piloted orbit when the player stops piloting, logs out or changes level. Client only. Implemented by
 * node MAP; the screen and HUD live in {@code dev.apoapsis.client.map}.
 */
public final class MapClient {
    private static final Identifier FLIGHT_HUD = Identifier.fromNamespaceAndPath(Apoapsis.MODID, "flight_hud");

    private MapClient() {}

    /** Called from {@code ApoapsisClient} with the mod event bus. */
    public static void register(IEventBus modBus) {
        modBus.addListener(MapClient::onRegisterGuiLayers);
        NeoForge.EVENT_BUS.addListener(MapClient::onClientTickPost);
        NeoForge.EVENT_BUS.addListener(MapClient::onEntityMount);
        NeoForge.EVENT_BUS.addListener(MapClient::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(MapClient::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(OverlayMessages::onScreenRender);
    }

    /** Opens the full-screen system map (SHIPC's system map key calls this). Shows a hint when not piloting. */
    public static void openMap() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof MapScreen) {
            return;
        }
        mc.gui.setScreen(new MapScreen());
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, FLIGHT_HUD, FlightHud::render);
    }

    /** Counts the client ticks the game ran (not paused): the clock the orbit extrapolation reads. */
    private static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && !mc.isPaused()) {
            ClientState.clientTicks++;
        }
    }

    /** The local player stopped riding a ship: the orbit is no longer theirs. */
    private static void onEntityMount(EntityMountEvent event) {
        if (event.isDismounting()
                && event.getLevel().isClientSide()
                && event.getEntityMounting() == Minecraft.getInstance().player
                && event.getEntityBeingMounted() instanceof ShipEntity) {
            ClientState.clearOrbit();
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientState.clearOrbit();
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ClientState.clearOrbit();
        }
    }
}
