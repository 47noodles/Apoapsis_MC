package dev.apoapsis.ages;

import dev.apoapsis.flight.BodyId;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;

/**
 * SevTech-style Ages: progression gates on items. STUB from S0; node AGE implements it. Signatures are fixed.
 */
public final class Ages {
    private Ages() {}

    public static final int MAX_AGE = 6;

    public static void register(IEventBus modBus) {
    }

    /** The player's Age, 0..6. Works on both sides (client reads the synced value). */
    public static int getAge(Player player) {
        return MAX_AGE;
    }

    /** Sets the Age, syncs it to the client, and announces it if it went up. */
    public static void setAge(ServerPlayer player, int age) {
    }

    /** The Age an item needs, 0 if it is not gated. */
    public static int requiredAge(ItemStack stack) {
        return 0;
    }

    public static boolean canUse(Player player, ItemStack stack) {
        return getAge(player) >= requiredAge(stack);
    }

    /** Milestone: the player flew a ship they built above 1,500 m (Age 3 to 4). */
    public static void onFirstFlight(ServerPlayer player) {
    }

    /** Milestone: the player landed a ship on a body. */
    public static void onLanded(ServerPlayer player, BodyId body) {
    }

    /** Display name, for example "Age 4 · Ascent". */
    public static Component ageName(int age) {
        return Component.literal("Age " + age);
    }
}
