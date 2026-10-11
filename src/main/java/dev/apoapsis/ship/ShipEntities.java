package dev.apoapsis.ship;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.flight.FlightHooks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers the ship entity type (id apoapsis:ship) and the dismount guard. Owned by node SHIP. */
public final class ShipEntities {
    private ShipEntities() {}

    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Apoapsis.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ShipEntity>> SHIP = ENTITIES.registerEntityType(
            "ship", ShipEntity::new, MobCategory.MISC,
            b -> b.sized(1.0f, 1.0f).clientTrackingRange(16).updateInterval(1).noSummon());

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        NeoForge.EVENT_BUS.addListener(ShipEntities::onEntityMount);
    }

    /**
     * Refuses a pilot leaving the cockpit in orbit (no EVA in the prototype), unless FLT is moving the ship between
     * dimensions. Surface dismounts, including Shift and disassembly, are allowed.
     */
    private static void onEntityMount(EntityMountEvent event) {
        if (!event.isDismounting() || !(event.getEntityBeingMounted() instanceof ShipEntity ship)) {
            return;
        }
        if (ship.isTransferring() || !FlightHooks.environment(ship).orbital()) {
            return;
        }
        event.setCanceled(true);
        if (event.getEntityMounting() instanceof Player player && !ship.level().isClientSide()) {
            player.sendOverlayMessage(Component.translatable("message.apoapsis.ship.no_spacewalks"));
        }
    }
}
