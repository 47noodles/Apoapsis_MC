package dev.apoapsis.ages;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Item gates for using and placing, and the Age lines on tooltips. */
final class AgeGates {
    private AgeGates() {}

    /** Muted red for "Needs Age N" lines. */
    static final int MUTED_RED = 0xC06060;
    /** At most one action-bar refusal per player in this window. */
    private static final long NOTICE_COOLDOWN_MS = 1000L;

    private static final Map<UUID, Long> LAST_NOTICE = new ConcurrentHashMap<>();

    static void register() {
        NeoForge.EVENT_BUS.addListener(AgeGates::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(AgeGates::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(AgeGates::onEntityPlace);
        NeoForge.EVENT_BUS.addListener(AgeGates::onTooltip);
    }

    /** Clears the action-bar cooldown for a player who has left. */
    static void forget(UUID playerId) {
        LAST_NOTICE.remove(playerId);
    }

    /** Right-clicking a block with a locked item: the whole interaction is refused. */
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        int required = Ages.requiredAge(event.getItemStack());
        if (required > 0 && Ages.getAge(event.getEntity()) < required) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            refuse(event.getEntity(), required);
        }
    }

    /** Using a locked item in the air. */
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        int required = Ages.requiredAge(event.getItemStack());
        if (required > 0 && Ages.getAge(event.getEntity()) < required) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            refuse(event.getEntity(), required);
        }
    }

    /**
     * Placing a locked block. NeoForge posts this for entity placement; in 26.3 it is not posted for a player's
     * BlockItem use, where {@link #onRightClickBlock} already refuses the placement.
     */
    static void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        int required = Ages.requiredAgeOf(event.getPlacedBlock().getBlock().asItem());
        if (required > 0 && Ages.getAge(player) < required) {
            event.setCanceled(true);
            refuse(player, required);
        }
    }

    /** A grey "Age N · Name" line on gated items, or a muted red "Needs Age N · Name" when locked. */
    static void onTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        int required = Ages.requiredAge(stack);
        if (player == null || required <= 0) {
            return;
        }
        if (Ages.getAge(player) < required) {
            event.getToolTip().add(needsLine(required));
        } else {
            MutableComponent line = Ages.ageName(required).copy().withStyle(ChatFormatting.GRAY);
            event.getToolTip().add(line);
        }
    }

    /**
     * The refusal is sent from the server only: the client's use packet still reaches the server after a cancelled
     * click, so the server's cancelled event is the one that reports it.
     */
    private static void refuse(Player player, int required) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = LAST_NOTICE.get(serverPlayer.getUUID());
        if (last != null && now - last < NOTICE_COOLDOWN_MS) {
            return;
        }
        LAST_NOTICE.put(serverPlayer.getUUID(), now);
        serverPlayer.sendOverlayMessage(needsLine(required));
    }

    /** "Needs Age 4 · Ascent" in muted red. */
    static MutableComponent needsLine(int required) {
        return Component.translatable("apoapsis.ages.needs", Ages.ageName(required)).withColor(MUTED_RED);
    }
}
