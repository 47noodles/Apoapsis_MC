package dev.apoapsis.client;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.client.hud.ReentryVignette;
import dev.apoapsis.client.map.FlightHud;
import dev.apoapsis.client.map.MapScreen;
import dev.apoapsis.client.map3d.ManeuverEditor;
import dev.apoapsis.client.map3d.Map3DPip;
import dev.apoapsis.client.map3d.Map3DScreen;
import dev.apoapsis.client.map3d.SiteMarkers;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.EventPriority;
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
    private static final Identifier REENTRY_VIGNETTE =
            Identifier.fromNamespaceAndPath(Apoapsis.MODID, "reentry_vignette");
    private static final String MAP3D_PROPERTY = "apoapsis.map3d";
    /** Which map the map key opens: the 3D one unless {@code -Dapoapsis.map3d=false}; Tab in a map flips it. */
    private static boolean use3d = Boolean.parseBoolean(System.getProperty(MAP3D_PROPERTY, "true"));
    /**
     * Client ticks the local player may stay out of a ship, after a dismount, before the orbit is forgotten. The tracker
     * can remove a pilot's ship and add it again, and a pilot who crossed to another dimension is seated a few ticks
     * after arriving (up to about 25 in a slow load); the orbit state, sent as the pilot is seated, must survive both.
     */
    private static final int LEAVE_GRACE_TICKS = 40;
    /** {@link ClientState#clientTicks} at the dismount that {@link #settleLeave} has not decided yet, or -1. */
    private static long leaveTick = -1;
    /** {@link ClientState#orbitCount} at that dismount. */
    private static long leaveOrbitCount;

    private MapClient() {}

    /** Called from {@code ApoapsisClient} with the mod event bus. */
    public static void register(IEventBus modBus) {
        modBus.addListener(MapClient::onRegisterGuiLayers);
        modBus.addListener(Map3DPip::onRegisterPip);
        NeoForge.EVENT_BUS.addListener(MapClient::onClientTickPost);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, MapClient::onEntityMount);
        NeoForge.EVENT_BUS.addListener(MapClient::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(MapClient::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(OverlayMessages::onScreenRender);
    }

    /** Opens the full-screen system map (SHIPC's system map key calls this). Shows a hint when not piloting. */
    public static void openMap() {
        Minecraft mc = Minecraft.getInstance();
        if (use3d) {
            if (!(mc.gui.screen() instanceof Map3DScreen)) {
                mc.gui.setScreen(new Map3DScreen());
            }
            return;
        }
        if (mc.gui.screen() instanceof MapScreen) {
            return;
        }
        mc.gui.setScreen(new MapScreen());
    }

    /**
     * Swaps the open map for the other one (the Tab key in either), and makes it the one the map key opens from now on.
     * The 3D map is the default; start the game with {@code -Dapoapsis.map3d=false} to open the 2D map instead.
     */
    public static void switchMap() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof Map3DScreen) {
            use3d = false;
            mc.gui.setScreen(new MapScreen());
        } else if (mc.gui.screen() instanceof MapScreen) {
            use3d = true;
            mc.gui.setScreen(new Map3DScreen());
        }
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, FLIGHT_HUD, FlightHud::render);
        event.registerBelow(FLIGHT_HUD, REENTRY_VIGNETTE, ReentryVignette::render);
    }

    /** Counts the client ticks the game ran (not paused): the clock the orbit extrapolation reads. */
    private static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && !mc.isPaused()) {
            ClientState.clientTicks++;
            SiteMarkers.observe(mc);
        }
        settleLeave(mc);
    }

    /**
     * The local player stopped riding a ship: the orbit is no longer theirs, unless the dismount is undone. Many
     * dismounts are not a pilot leaving. The client applies a ship's passenger list by ejecting every passenger and
     * mounting them again, and the server sends that list more than once as a ship becomes tracked, so a pilot is
     * dismounted and mounted again in one packet. In orbit {@code ShipEntities} cancels that dismount (no spacewalks),
     * and a transition sends the old ship's dismount and removes the new ship and adds it again. Clearing the orbit on
     * each of them took the HUD and the planets away in the first frames after every transition, until the next sync.
     * So this listener runs last, where a cancelled event does not reach it, ignores a ship of another level, and
     * leaves the decision for any other dismount to {@link #settleLeave}.
     */
    private static void onEntityMount(EntityMountEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.isDismounting()
                && event.getLevel().isClientSide()
                && event.getEntityMounting() == mc.player
                && event.getEntityBeingMounted() instanceof ShipEntity ship
                && ship.level() == mc.level) {
            leaveTick = ClientState.clientTicks;
            leaveOrbitCount = ClientState.orbitCount;
        }
    }

    /**
     * After a dismount: forgets the orbit and the maneuver node once the local player has stayed out of a ship for
     * {@link #LEAVE_GRACE_TICKS} ticks. Riding a ship again, or an orbit state arriving after the dismount (the one sent
     * as a pilot is seated in the ship they crossed to), settles it the other way: the orbit is kept.
     */
    private static void settleLeave(Minecraft mc) {
        if (leaveTick < 0) {
            return;
        }
        boolean riding = mc.player != null && mc.player.getVehicle() instanceof ShipEntity;
        if (riding || ClientState.orbitCount != leaveOrbitCount) {
            leaveTick = -1;
        } else if (ClientState.clientTicks - leaveTick >= LEAVE_GRACE_TICKS) {
            leaveTick = -1;
            ClientState.clearOrbit();
            ManeuverEditor.forget();
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        leaveTick = -1;
        ClientState.clearOrbit();
        ManeuverEditor.forget();
        SiteMarkers.forget();
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            leaveTick = -1;
            ClientState.clearOrbit();
            ManeuverEditor.forget();
        }
    }
}
