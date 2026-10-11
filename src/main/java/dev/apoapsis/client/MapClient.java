package dev.apoapsis.client;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.client.map.FlightHud;
import dev.apoapsis.client.map.MapScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * The full-screen system map (M) and the flight HUD layer.
 * Client only. Implemented by node MAP; the screen and HUD live in {@code dev.apoapsis.client.map}.
 */
public final class MapClient {
    private static final Identifier FLIGHT_HUD = Identifier.fromNamespaceAndPath(Apoapsis.MODID, "flight_hud");

    private MapClient() {}

    /** Called from {@code ApoapsisClient} with the mod event bus. */
    public static void register(IEventBus modBus) {
        modBus.addListener(MapClient::onRegisterGuiLayers);
    }

    /** Opens the full-screen system map (SHIPC's M key calls this). Shows a hint when not piloting. */
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
}
