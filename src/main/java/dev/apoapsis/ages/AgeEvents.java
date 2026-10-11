package dev.apoapsis.ages;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Milestone checks and the Age sync on login, respawn and dimension change. Server side only. */
final class AgeEvents {
    private AgeEvents() {}

    /** Milestone checks run every this many ticks (one second). */
    private static final int MILESTONE_INTERVAL = 20;

    static void register() {
        NeoForge.EVENT_BUS.addListener(AgeEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(AgeEvents::onLoggedIn);
        NeoForge.EVENT_BUS.addListener(AgeEvents::onRespawn);
        NeoForge.EVENT_BUS.addListener(AgeEvents::onChangedDimension);
        NeoForge.EVENT_BUS.addListener(AgeEvents::onLoggedOut);
    }

    /** Copper ingot at Age 0, iron ingot at Age 1, piston at Age 2 (anywhere in the inventory). One step per check. */
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % MILESTONE_INTERVAL != 0) {
            return;
        }
        int age = Ages.getAge(player);
        Item needed = switch (age) {
            case 0 -> Items.COPPER_INGOT;
            case 1 -> Items.IRON_INGOT;
            case 2 -> Items.PISTON;
            default -> null;
        };
        if (needed != null && inventoryHas(player, needed)) {
            Ages.setAge(player, age + 1);
        }
    }

    static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Ages.sync(player);
        }
    }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Ages.sync(player);
        }
    }

    static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Ages.sync(player);
        }
    }

    static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        AgeGates.forget(event.getEntity().getUUID());
    }

    private static boolean inventoryHas(Player player, Item item) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).getItem() == item) {
                return true;
            }
        }
        return false;
    }
}
