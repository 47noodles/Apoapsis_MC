package dev.apoapsis.flight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.apoapsis.Apoapsis;
import dev.apoapsis.ship.ShipEntity;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Orbit slots in use, saved on the overworld. A slot stays claimed while its ship's chunk is unloaded. It is released
 * only when the ship comes down to a surface or is killed or discarded.
 */
public final class OrbitSlots extends SavedData {
    static final Codec<OrbitSlots> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.listOf().optionalFieldOf("used", List.of()).forGetter(s -> List.copyOf(s.used))
    ).apply(i, OrbitSlots::new));

    static final SavedDataType<OrbitSlots> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Apoapsis.MODID, "orbit_slots"), OrbitSlots::new, CODEC);

    private final Set<Integer> used = new HashSet<>();

    OrbitSlots() {}

    private OrbitSlots(List<Integer> saved) {
        used.addAll(saved);
    }

    private static OrbitSlots get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * Claims the lowest slot that no saved slot and no loaded ship uses. The loaded-ship scan keeps slots of saves
     * written before slots were saved.
     */
    static int claim(MinecraftServer server, ServerLevel orbit) {
        OrbitSlots slots = get(server);
        Set<Integer> taken = new HashSet<>(slots.used);
        for (Entity e : orbit.getAllEntities()) {
            if (e instanceof ShipEntity s) {
                int slot = FlightAttachments.state(s).slot;
                if (slot >= 0) {
                    taken.add(slot);
                }
            }
        }
        int slot = 0;
        while (taken.contains(slot)) {
            slot++;
        }
        slots.used.add(slot);
        slots.setDirty();
        return slot;
    }

    /** Frees a slot when its ship leaves orbit or is removed. A negative slot (no claim) is ignored. */
    static void release(MinecraftServer server, int slot) {
        if (slot < 0) {
            return;
        }
        OrbitSlots slots = get(server);
        if (slots.used.remove(slot)) {
            slots.setDirty();
        }
    }
}
