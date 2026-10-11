package dev.apoapsis.ages;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.apoapsis.Apoapsis;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /apoapsis age ...}. Other modules add their own children under the same literal; Brigadier merges them. */
final class AgeCommands {
    private AgeCommands() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener(AgeCommands::onRegisterCommands);
    }

    static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> age = Commands.literal("age")
                .executes(ctx -> showSelf(ctx.getSource()))
                .then(Commands.literal("get")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> showOther(ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("set")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("age", IntegerArgumentType.integer(0, Ages.MAX_AGE))
                                        .executes(ctx -> setAge(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "age"))))))
                .then(Commands.literal("sandbox")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("on")
                                .executes(ctx -> setSandbox(ctx.getSource(), true)))
                        .then(Commands.literal("off")
                                .executes(ctx -> setSandbox(ctx.getSource(), false))));

        event.getDispatcher().register(Commands.literal(Apoapsis.MODID).then(age));
    }

    private static int showSelf(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int age = Ages.getAge(player);
        source.sendSuccess(() -> Component.translatable("apoapsis.ages.cmd.self", Ages.ageName(age)), false);
        return age;
    }

    private static int showOther(CommandSourceStack source, ServerPlayer target) {
        int age = Ages.getAge(target);
        source.sendSuccess(() -> Component.translatable("apoapsis.ages.cmd.get", target.getDisplayName(),
                Ages.ageName(age)), false);
        return age;
    }

    private static int setAge(CommandSourceStack source, ServerPlayer target, int age) {
        Ages.setAge(target, age);
        int effective = Ages.getAge(target);
        source.sendSuccess(() -> Component.translatable("apoapsis.ages.cmd.set", target.getDisplayName(),
                Ages.ageName(effective)), true);
        return effective;
    }

    private static int setSandbox(CommandSourceStack source, boolean on) {
        Ages.setSandbox(source.getServer(), on);
        source.sendSuccess(() -> Component.translatable("apoapsis.ages.cmd.sandbox",
                Component.translatable(on ? "apoapsis.ages.cmd.on" : "apoapsis.ages.cmd.off")), true);
        return on ? 1 : 0;
    }
}
