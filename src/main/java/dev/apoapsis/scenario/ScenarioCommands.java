package dev.apoapsis.scenario;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.apoapsis.ages.Ages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

/** {@code /apoapsis scenario pad|orbit}: test scenarios for the Kestrel. Permission level 2. */
public final class ScenarioCommands {
    /** Distance ahead of the player at which the pad's centre sits, blocks. */
    private static final int PAD_DISTANCE = 8;

    private ScenarioCommands() {}

    /** Adds the scenario subtree under the shared {@code apoapsis} literal. */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("apoapsis")
                .then(Commands.literal("scenario")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("pad").executes(ScenarioCommands::pad))
                        .then(Commands.literal("orbit").executes(ScenarioCommands::orbit))));
    }

    private static int pad(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        double rad = Math.toRadians(player.getYRot());
        // Vanilla yaw: 0 is +Z (south), 90 is -X (west). The horizontal look direction ignores pitch.
        int cx = Mth.floor(player.getX() - Math.sin(rad) * PAD_DISTANCE);
        int cz = Mth.floor(player.getZ() + Math.cos(rad) * PAD_DISTANCE);
        Blueprint blueprint;
        try {
            blueprint = Blueprint.load(source.getServer(), "kestrel");
        } catch (IllegalStateException e) {
            source.sendFailure(Component.translatable("apoapsis.scenario.blueprint.failed"));
            return 0;
        }
        ServerLevel level = player.level();
        if (!KestrelPad.place(level, cx, cz, blueprint)) {
            source.sendFailure(Component.translatable("apoapsis.scenario.pad.blocked"));
            return 0;
        }
        // Quiet: no flight has happened, so the Custodian does not announce First Flight.
        // stored value: sandbox must not leak a raised Age into the save
        Ages.setAge(player, Math.max(Ages.stored(player), 4), false);
        source.sendSuccess(() -> Component.translatable("apoapsis.scenario.pad.done"), false);
        return 1;
    }

    private static int orbit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        if (!KestrelOrbit.run(player)) {
            return 0;
        }
        return 1;
    }
}
