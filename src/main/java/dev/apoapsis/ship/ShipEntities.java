package dev.apoapsis.ship;

import dev.apoapsis.Apoapsis;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers the ship entity type (id apoapsis:ship). Owned by node SHIP. */
public final class ShipEntities {
    private ShipEntities() {}

    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Apoapsis.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ShipEntity>> SHIP = ENTITIES.registerEntityType(
            "ship", ShipEntity::new, MobCategory.MISC,
            b -> b.sized(1.0f, 1.0f).clientTrackingRange(16).updateInterval(1).noSummon());

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }
}
