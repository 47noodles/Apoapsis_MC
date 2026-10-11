package dev.apoapsis.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * Turns placed blocks into a {@link ShipEntity} and back. STUB from S0; node SHIP implements it.
 */
public final class ShipAssembler {
    private ShipAssembler() {}

    /** Outcome of an assembly attempt: exactly one of {@code ship} and {@code error} is non-null. */
    public record Result(ShipEntity ship, Component error) {
    }

    /**
     * Flood-fills face-connected ship blocks from the cockpit, removes them from the world, spawns the ship, and
     * seats {@code pilot} if non-null.
     */
    public static Result assemble(ServerLevel level, BlockPos cockpitPos, Player pilot) {
        return new Result(null, Component.literal("Not implemented"));
    }

    /** Places the ship back as blocks if it is landed and slow; returns an error message or null on success. */
    public static Component disassemble(ShipEntity ship) {
        return Component.literal("Not implemented");
    }
}
