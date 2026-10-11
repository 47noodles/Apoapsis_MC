package dev.apoapsis.scenario;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.ages.Ages;
import dev.apoapsis.ship.ShipAssembler;
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
    /** JVM system property naming the test save, when a run uses its own fresh save instead of the default one. */
    public static final String SAVE_PROPERTY = "apoapsis.testSave";
    /** JVM system property that picks the start: "pad" puts the player in the Kestrel on the pad, else the orbit start. */
    public static final String START_PROPERTY = "apoapsis.testStart";
    /** Level name of the test save; must equal {@code TestFlightClient.SAVE_NAME}. */
    private static final String SAVE_NAME = "Apoapsis Test Flight";

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
        // Only that save: the name must match TestFlightClient.SAVE_NAME (copied here, the client class is not imported).
        String level = server.getWorldData().getLevelName();
        if ((SAVE_NAME.equals(level) || level.equals(System.getProperty(SAVE_PROPERTY)))
                && server.getWorldData() instanceof PrimaryLevelData data) {
            data.withConfirmedWarning(true);
        }
        Scenarios.runNextTick(() -> setUp(server, player));
    }

    /** True for the pad start: the Kestrel on the pad, assembled, with the player in its cockpit. */
    public static boolean padStart() {
        return "pad".equals(System.getProperty(START_PROPERTY));
    }

    private static void setUp(MinecraftServer server, ServerPlayer player) {
        ServerLevel overworld = server.overworld();
        // The orbit start runs once per world. The pad start has no flag of its own, so each pad-start run places its pad.
        boolean padStart = padStart();
        if (!padStart) {
            TestFlightFlag flag = overworld.getDataStorage().computeIfAbsent(TestFlightFlag.TYPE);
            if (flag.isSetUp()) {
                return;
            }
            // Mark first so a failure below is not retried on every login.
            flag.markSetUp();
        }

        BlockPos spawn = overworld.getRespawnData().globalPos().pos();
        Blueprint blueprint;
        try {
            blueprint = Blueprint.load(server, "kestrel");
        } catch (IllegalStateException e) {
            Apoapsis.LOG.error("Test flight: the Kestrel blueprint could not be read");
            player.sendSystemMessage(Component.translatable("apoapsis.scenario.blueprint.failed"));
            return;
        }
        if (padStart) {
            setUpPadStart(overworld, player, spawn, blueprint);
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

    /**
     * Pad start: the pad's blocks become a ship through the normal assembly from the cockpit, which seats the player.
     * The cockpit needs Age 4, as in the orbit start.
     */
    private static void setUpPadStart(ServerLevel overworld, ServerPlayer player, BlockPos spawn, Blueprint blueprint) {
        BlockPos cockpit = KestrelPad.placeCockpit(overworld, spawn.getX(), spawn.getZ(), blueprint);
        if (cockpit == null) {
            Apoapsis.LOG.warn("Test flight: no room for the spawn pad at {}", spawn);
            player.sendSystemMessage(Component.translatable("apoapsis.scenario.testflight.pad_failed"));
            return;
        }
        Ages.setAge(player, Math.max(Ages.getAge(player), 4), false);
        ShipAssembler.Result result = ShipAssembler.assemble(overworld, cockpit, player);
        if (result.error() != null) {
            player.sendSystemMessage(result.error());
        } else {
            player.sendSystemMessage(Component.translatable("apoapsis.scenario.testflight.done"));
        }
    }
}
