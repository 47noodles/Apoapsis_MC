package dev.apoapsis.client;

import dev.apoapsis.client.sky.PlanetTextures;
import dev.apoapsis.client.sky.SkyBodies;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.material.FogType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Bodies drawn in the sky: Verdance and Pale from the orbit dimension, Pale from Verdance, Verdance from Pale.
 * Client only. The drawing lives in {@link SkyBodies}; the surface maps in {@link PlanetTextures}, which are dropped on
 * logout. They are kept across dimension changes (each unloads the client level), since the server sends them once.
 */
public final class SkyClient {
    private SkyClient() {}

    /** Called from {@code ApoapsisClient} with the mod event bus. */
    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(SubmitCustomGeometryEvent.class, SkyBodies::onSubmitCustomGeometry);
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeFogColor.class, SkyClient::climbFogColour);
        NeoForge.EVENT_BUS.addListener(ViewportEvent.RenderFog.class, SkyClient::climbFogFar);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, e -> PlanetTextures.clear());
    }

    /** The climb's share at the camera now, 0 outside the surface dimensions. */
    private static double climbShare(Camera camera) {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? 0 : SkyBodies.climbShare(level.dimension(), camera.position().y());
    }

    /** Fog goes toward black as the climb goes on, so the far chunks end in the same black as the sky. */
    private static void climbFogColour(ViewportEvent.ComputeFogColor event) {
        double share = climbShare(event.getCamera());
        if (share <= 0) {
            return;
        }
        float keep = (float) (1 - share);
        event.setRed(event.getRed() * keep);
        event.setGreen(event.getGreen() * keep);
        event.setBlue(event.getBlue() * keep);
    }

    /** Atmospheric fog thins as the climb goes on: its far end moves out to three times the distance. */
    private static void climbFogFar(ViewportEvent.RenderFog event) {
        if (event.getType() != FogType.ATMOSPHERIC) {
            return;
        }
        double share = climbShare(event.getCamera());
        if (share > 0) {
            event.scaleFarPlaneDistance((float) (1 + 2 * share));
        }
    }
}
