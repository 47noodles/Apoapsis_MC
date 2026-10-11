package dev.apoapsis.ages;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.apoapsis.Apoapsis;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /apoapsis guide} shows the current step again; {@code /apoapsis guide book} gives the guide as a book. */
final class GuideCommands {
    private GuideCommands() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener(GuideCommands::onRegisterCommands);
    }

    static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> guide = Commands.literal("guide")
                .executes(ctx -> showCurrent(ctx.getSource()))
                .then(Commands.literal("book")
                        .executes(ctx -> giveBook(ctx.getSource())));

        event.getDispatcher().register(Commands.literal(Apoapsis.MODID).then(guide));
    }

    /** Shows the player's current guide step again, in chat, and returns its number. */
    private static int showCurrent(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Guide.Step step = Guide.current(player);
        Guide.show(player, step);
        return step.number();
    }

    /** Puts a guide book in the player's pack, or drops it at their feet when the pack is full. */
    private static int giveBook(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack book = GuideBook.create();
        if (!player.getInventory().add(book)) {
            player.drop(book, false, Prediction.SERVER_ONLY);
        }
        source.sendSuccess(() -> Component.translatable("apoapsis.guide.cmd.book"), false);
        return 1;
    }
}
