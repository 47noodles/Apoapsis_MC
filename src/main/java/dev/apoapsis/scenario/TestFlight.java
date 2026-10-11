package dev.apoapsis.scenario;

import dev.apoapsis.Apoapsis;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.PrimaryLevelData;

/**
 * The self-creating test save, server half. Active only when the JVM property {@value #PROPERTY} is "true" (set by the
 * testFlight run). On the first login of such a world it builds a Kestrel on a pad at spawn and runs the orbit scenario
 * for that player, once per world.
 */
public final class TestFlight {
    /** JVM system property that enables the test flight. */
    public static final String PROPERTY = "apoapsis.testFlight";

    private TestFlight() {}

    public static boolean enabled() {
        return "true".equals(System.getProperty(PROPERTY));
    }

    /** Called on the server thread from the login event. The set-up runs on the next tick, once the player is in. */
    static void onPlayerLoggedIn(ServerPlayer player) {
        if (!enabled()) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        // Vanilla counts any world with extra dimensions as experimental and asks for a backup each time the save is
        // reopened; the test save is ours, so record the warning as seen (written to level.dat on the next save).
        if (server.getWorldData() instanceof PrimaryLevelData data) {
            data.withConfirmedWarning(true);
        }
        Scenarios.runNextTick(() -> setUp(server, player));
    }

    private static void setUp(MinecraftServer server, ServerPlayer player) {
        ServerLevel overworld = server.overworld();
        TestFlightFlag flag = overworld.getDataStorage().computeIfAbsent(TestFlightFlag.TYPE);
        if (flag.isSetUp()) {
            return;
        }
        // Mark first so a failure below is not retried on every login.
        flag.markSetUp();

        BlockPos spawn = overworld.getRespawnData().globalPos().pos();
        Blueprint blueprint;
        try {
            blueprint = Blueprint.load(server, "kestrel");
        } catch (IllegalStateException e) {
            Apoapsis.LOG.error("Test flight: the Kestrel blueprint could not be read");
            player.sendSystemMessage(Component.translatable("apoapsis.scenario.blueprint.failed"));
            return;
        }
        boolean padOk = KestrelPad.place(overworld, spawn.getX(), spawn.getZ(), blueprint);
        if (!padOk) {
            Apoapsis.LOG.warn("Test flight: no room for the spawn pad at {}", spawn);
        }
        boolean orbitOk = KestrelOrbit.run(player);
        if (padOk && orbitOk) {
            player.sendSystemMessage(Component.translatable("apoapsis.scenario.testflight.done"));
        } else if (!padOk) {
            player.sendSystemMessage(Component.translatable("apoapsis.scenario.testflight.pad_failed"));
        }
    }
}
