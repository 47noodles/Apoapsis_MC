package dev.apoapsis.ship;

import dev.apoapsis.ages.Ages;
import dev.apoapsis.grid.FuelTankBlockEntity;
import dev.apoapsis.grid.GridBlocks;
import dev.apoapsis.grid.ShipParts;
import dev.apoapsis.ownership.ShipOwnership;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import org.joml.Quaternionf;

/**
 * Turns placed blocks into a {@link ShipEntity} and back. When a player is involved, every block must pass
 * {@code Level.mayInteract} and the NeoForge break (assembly) or place (disassembly) event, or nothing changes.
 */
public final class ShipAssembler {
    /** Removing ship blocks: client update only, no drops, no neighbour updates (so nothing cascades). */
    private static final int REMOVE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    /** Largest lift tried when the blocks would overlap solid terrain, in blocks. */
    private static final int MAX_LIFT = 3;
    /** A ship more tilted than this cannot be turned back into blocks. */
    private static final double MAX_LEVEL_DEG = 20.0;
    /** Speed and spin limits for leaving the ship as blocks. */
    private static final double MAX_STOPPED_SPEED = 1.0;
    private static final double MAX_STOPPED_SPIN = 0.2;

    private ShipAssembler() {}

    /** Outcome of an assembly attempt: exactly one of {@code ship} and {@code error} is non-null. */
    public record Result(ShipEntity ship, Component error) {
    }

    /** One block to place when disassembling: its target cell, the state it takes and the fuel it keeps. */
    private record Placement(BlockPos cell, BlockState state, double fuelKg) {
    }

    /**
     * Flood-fills face-connected ship blocks from the cockpit, removes them from the world, spawns the ship, and
     * seats {@code pilot} if non-null. Nothing changes in the world if the attempt fails. With a pilot, the cockpit
     * must be usable at the pilot's Age and every block must be breakable by the pilot.
     */
    public static Result assemble(ServerLevel level, BlockPos cockpitPos, Player pilot) {
        if (!ShipParts.isCockpit(level.getBlockState(cockpitPos))) {
            return new Result(null, Component.translatable("message.apoapsis.ship.no_cockpit"));
        }
        // Break events are built by hand below, so they do not get the checks NeoForge applies to player breaks.
        if (pilot != null) {
            if (!pilot.getAbilities().mayBuild) {
                return new Result(null, protectedError());
            }
            ItemStack cockpitItem = new ItemStack(GridBlocks.COCKPIT.get());
            if (!Ages.canUse(pilot, cockpitItem)) {
                return new Result(null, Component.translatable("apoapsis.ages.needs",
                        Ages.ageName(Ages.requiredAge(cockpitItem))));
            }
        }

        Set<BlockPos> found = new LinkedHashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        BlockPos start = cockpitPos.immutable();
        found.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos at = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos next = at.relative(d);
                if (found.contains(next) || !level.hasChunkAt(next)
                        || !ShipParts.isShipBlock(level.getBlockState(next))) {
                    continue;
                }
                if (found.size() >= ShipStructure.MAX_BLOCKS) {
                    return new Result(null, Component.translatable("message.apoapsis.ship.too_many_blocks"));
                }
                BlockPos imm = next.immutable();
                found.add(imm);
                queue.add(imm);
            }
        }

        if (pilot != null) {
            for (BlockPos p : found) {
                if (!mayBreak(level, pilot, p)) {
                    return new Result(null, protectedError());
                }
            }
        }

        ShipStructure structure = new ShipStructure();
        for (BlockPos p : found) {
            BlockPos local = new BlockPos(p.getX() - cockpitPos.getX(), p.getY() - cockpitPos.getY(),
                    p.getZ() - cockpitPos.getZ());
            structure.put(local, level.getBlockState(p));
            if (level.getBlockEntity(p) instanceof FuelTankBlockEntity tank) {
                structure.setFuel(local, tank.getFuelKg());
            }
        }

        for (BlockPos p : found) {
            level.removeBlockEntity(p);
        }
        for (BlockPos p : found) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), REMOVE_FLAGS);
        }

        // Block centres are at local + 0.5, so the world centre of mass is the cockpit corner plus the local centre.
        Vec3 com = structure.centerOfMassLocal();
        Vec3 comWorld = new Vec3(cockpitPos.getX() + com.x, cockpitPos.getY() + com.y, cockpitPos.getZ() + com.z);
        ShipEntity ship = ShipEntity.spawn(level, structure, comWorld, new Quaternionf());
        ShipOwnership.claim(ship, pilot);
        if (pilot != null) {
            ship.seatPilot(pilot);
        }
        return new Result(ship, null);
    }

    /** Places the ship back as blocks if it is landed and slow; returns an error message or null on success. */
    public static Component disassemble(ShipEntity ship) {
        if (!(ship.level() instanceof ServerLevel level)) {
            return Component.translatable("message.apoapsis.ship.not_landed");
        }
        if (!ship.isLanded() || ship.getVelocity().length() >= MAX_STOPPED_SPEED
                || ship.getAngularVelocity().length() >= MAX_STOPPED_SPIN) {
            return Component.translatable("message.apoapsis.ship.not_landed");
        }
        ShipGeometry.Snap snap = ShipGeometry.snap(ship.getOrientation());
        if (snap.angleDeg() > MAX_LEVEL_DEG) {
            return Component.translatable("message.apoapsis.ship.not_level");
        }

        ShipStructure structure = ship.getStructure();
        // A sideways ship would turn a horizontal facing (the cockpit's) up or down, which no block allows.
        for (BlockState state : structure.blocks().values()) {
            if (!ShipGeometry.canRotateFacing(state, snap)) {
                return Component.translatable("message.apoapsis.ship.not_level");
            }
        }

        Player pilot = ship.getPilot();
        if (pilot != null && !pilot.getAbilities().mayBuild) {
            return protectedError();
        }
        Vec3 com = ship.getCenterOfMassLocal();
        Vec3 base = ship.position();
        Vec3 origin = null;
        Map<BlockPos, BlockPos> targets = null;
        for (int lift = 0; lift <= MAX_LIFT && origin == null; lift++) {
            Vec3 candidate = base.add(0, lift, 0);
            Map<BlockPos, BlockPos> cells = targetCells(level, structure, snap, com, candidate);
            if (cells != null) {
                origin = candidate;
                targets = cells;
            }
        }
        if (origin == null || targets == null) {
            return Component.translatable("message.apoapsis.ship.no_room");
        }

        // Validate every block before placing any: protection for the pilot, then the state each block takes.
        List<Placement> plan = new ArrayList<>(targets.size());
        for (Map.Entry<BlockPos, BlockPos> e : targets.entrySet()) {
            BlockPos local = e.getKey();
            BlockPos cell = e.getValue();
            if (!mayPlace(level, cell, pilot)) {
                return protectedError();
            }
            plan.add(new Placement(cell, ShipGeometry.rotateFacing(structure.get(local), snap),
                    structure.getFuel(local)));
        }

        for (Placement p : plan) {
            level.setBlock(p.cell(), p.state(), Block.UPDATE_ALL);
            if (level.getBlockEntity(p.cell()) instanceof FuelTankBlockEntity tank) {
                tank.setFuelKg(p.fuelKg());
            }
        }

        // Move the entity to the placed pose so the pilot is put down beside the cockpit as it now is.
        ship.setOrientation(snap.quaternion());
        ship.setPos(origin);
        if (pilot != null) {
            pilot.stopRiding();
        }
        ship.remove(Entity.RemovalReason.DISCARDED);
        return null;
    }

    /**
     * Target cell of each block: floor(origin + snapped(p + 0.5 - com)). Null if any target is occupied by a
     * block that cannot be replaced, lies outside the loaded world, or is above or below the build height.
     */
    private static Map<BlockPos, BlockPos> targetCells(ServerLevel level, ShipStructure structure,
                                                       ShipGeometry.Snap snap, Vec3 com, Vec3 origin) {
        Map<BlockPos, BlockPos> cells = new LinkedHashMap<>();
        for (BlockPos local : structure.blocks().keySet()) {
            Vec3 offset = snap.apply(new Vec3(local.getX() + 0.5 - com.x, local.getY() + 0.5 - com.y,
                    local.getZ() + 0.5 - com.z));
            BlockPos cell = BlockPos.containing(origin.x + offset.x, origin.y + offset.y, origin.z + offset.z);
            if (!level.hasChunkAt(cell) || level.isOutsideBuildHeight(cell)
                    || !canReceiveBlock(level.getBlockState(cell))) {
                return null;
            }
            cells.put(local, cell);
        }
        return cells;
    }

    /** Air, or a replaceable plant or snow layer without fluid. Fluids and fire are never overwritten. */
    private static boolean canReceiveBlock(BlockState state) {
        if (state.isAir()) {
            return true;
        }
        return state.canBeReplaced() && state.getFluidState().isEmpty() && !state.is(BlockTags.FIRE);
    }

    /** Whether {@code player} may break {@code pos}: the level allows interaction and no listener cancels the break. */
    private static boolean mayBreak(ServerLevel level, Player player, BlockPos pos) {
        if (!level.mayInteract(player, pos)) {
            return false;
        }
        BreakBlockEvent event = new BreakBlockEvent(level, pos, level.getBlockState(pos), player);
        return !NeoForge.EVENT_BUS.post(event).isCanceled();
    }

    /**
     * Whether a ship block may be placed at {@code cell}. The pilot, if any, must be allowed to interact there, and the
     * place event must not be cancelled. The event is posted with no pilot too, with a null entity.
     */
    private static boolean mayPlace(ServerLevel level, BlockPos cell, Player pilot) {
        if (pilot != null && !level.mayInteract(pilot, cell)) {
            return false;
        }
        BlockEvent.EntityPlaceEvent event = new BlockEvent.EntityPlaceEvent(
                BlockSnapshot.create(level.dimension(), level, cell), level.getBlockState(cell.below()), pilot);
        return !NeoForge.EVENT_BUS.post(event).isCanceled();
    }

    private static Component protectedError() {
        return Component.translatable("message.apoapsis.ship.protected");
    }
}
