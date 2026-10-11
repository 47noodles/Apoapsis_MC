package dev.apoapsis.flight;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** NeoForge listeners of the flight layer: universal time, pending seats, Pale gravity, cleanup on stop. */
final class FlightEvents {
    /** Seconds of game time per server tick at warp 1. */
    static final double TICK_SECONDS = 0.05;

    private FlightEvents() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener(FlightEvents::onServerTickPost);
        NeoForge.EVENT_BUS.addListener(FlightEvents::onEntityJoinLevel);
        NeoForge.EVENT_BUS.addListener(FlightEvents::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(FlightEvents::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(FlightEvents::onServerStopping);
    }

    private static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        UniverseTime.get(server).advance(TICK_SECONDS * FlightWarp.factor(server));
        Transitions.tickPendingSeats(server);
    }

    private static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            PaleGravity.sync(living);
        }
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        PaleGravity.sync(event.getEntity());
    }

    private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        PaleGravity.sync(event.getEntity());
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        FlightWarp.clear();
        Transitions.clearPending();
    }
}
