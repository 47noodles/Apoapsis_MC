package dev.apoapsis.scenario;

import dev.apoapsis.Apoapsis;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Test scenarios: the Kestrel rocket on a pad or already in orbit, commands, and headless test-save creation.
 * Node SCN implements it. The test-save client half lives in {@code scenario.client}.
 */
public final class Scenarios {
    private static final Queue<Runnable> NEXT_TICK = new ConcurrentLinkedQueue<>();

    private Scenarios() {}

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(Scenarios::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(Scenarios::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(Scenarios::onServerTickPost);
        NeoForge.EVENT_BUS.addListener(Scenarios::onServerStopped);
    }

    /** Runs {@code task} at the start of the next server tick (server thread only). */
    public static void runNextTick(Runnable task) {
        NEXT_TICK.add(task);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        ScenarioCommands.register(event.getDispatcher());
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            TestFlight.onPlayerLoggedIn(serverPlayer);
        }
    }

    private static void onServerTickPost(ServerTickEvent.Post event) {
        Runnable task;
        while ((task = NEXT_TICK.poll()) != null) {
            try {
                task.run();
            } catch (RuntimeException e) {
                Apoapsis.LOG.error("Scenario task failed", e);
            }
        }
    }

    /** Drops tasks still queued at shutdown, so they cannot run against a dead server or leak into the next one. */
    private static void onServerStopped(ServerStoppedEvent event) {
        NEXT_TICK.clear();
    }
}
