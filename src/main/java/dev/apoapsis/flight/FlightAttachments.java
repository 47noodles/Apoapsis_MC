package dev.apoapsis.flight;

import dev.apoapsis.Apoapsis;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** NeoForge data attachments of the flight module. The ship's flight state is saved with the ship entity. */
public final class FlightAttachments {
    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Apoapsis.MODID);

    /** Flight state of a {@code ShipEntity}: regime, body, inertial state, epoch, slot and milestone flags. */
    static final DeferredHolder<AttachmentType<?>, AttachmentType<FlightState>> FLIGHT = TYPES.register("ship_flight",
            () -> AttachmentType.builder(() -> new FlightState()).serialize(FlightState.CODEC).build());

    private FlightAttachments() {}

    static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }

    /** The flight state of a ship or any entity; the default is stored on first access. */
    static FlightState state(Entity entity) {
        return entity.getData(FLIGHT.get());
    }
}
