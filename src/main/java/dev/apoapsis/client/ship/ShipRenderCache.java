package dev.apoapsis.client.ship;

import dev.apoapsis.ship.ShipEntity;
import dev.apoapsis.ship.ShipStructure;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client render data per ship, keyed by entity id. Int keys hold no reference to the ship, so an entry never keeps its
 * entity alive. Entries whose id no longer names a ship in the level are purged once a second, and the whole cache is
 * cleared on logout and on level unload. Client only.
 */
public final class ShipRenderCache {
    private static final Int2ObjectOpenHashMap<Entry> ENTRIES = new Int2ObjectOpenHashMap<>();

    private ShipRenderCache() {}

    /** The entry for a ship id, created on first use. */
    public static Entry entry(int shipId) {
        Entry entry = ENTRIES.get(shipId);
        if (entry == null) {
            entry = new Entry();
            ENTRIES.put(shipId, entry);
        }
        return entry;
    }

    /**
     * Called once per client tick, after the tick has run. The orientation used to be sampled on these boundaries; it is
     * now smoothed by the ship itself ({@code ShipEntity#getRenderOrientation}), so there is nothing to do.
     */
    public static void advanceClientTick() {
    }

    /** Drops every entry whose id no longer names a ship in the level. */
    public static void purge(ClientLevel level) {
        IntIterator ids = ENTRIES.keySet().iterator();
        while (ids.hasNext()) {
            if (!(level.getEntity(ids.nextInt()) instanceof ShipEntity)) {
                ids.remove();
            }
        }
    }

    /** Forgets every ship. Called on logout and when a level unloads. */
    public static void clear() {
        ENTRIES.clear();
    }

    /** One ship's block lists. */
    public static final class Entry {
        /** Every block of the structure, in map order, for change detection. */
        private final List<ShipRenderState.ShipBlock> all = new ArrayList<>();
        /** The blocks that are drawn: every block except the fully enclosed ones. */
        private final List<ShipRenderState.ShipBlock> visible = new ArrayList<>();

        /**
         * The blocks to draw. Rebuilt only when the structure's blocks differ from the last frame's, and the fully
         * enclosed blocks are left out when it is rebuilt.
         */
        public List<ShipRenderState.ShipBlock> updateBlocks(ShipStructure structure) {
            if (!sameBlocks(structure)) {
                rebuild(structure);
            }
            return visible;
        }

        private boolean sameBlocks(ShipStructure structure) {
            Map<BlockPos, BlockState> blocks = structure.blocks();
            if (blocks.size() != all.size()) {
                return false;
            }
            int i = 0;
            for (Map.Entry<BlockPos, BlockState> e : blocks.entrySet()) {
                ShipRenderState.ShipBlock b = all.get(i++);
                if (b.state() != e.getValue() || !b.pos().equals(e.getKey())) {
                    return false;
                }
            }
            return true;
        }

        private void rebuild(ShipStructure structure) {
            all.clear();
            visible.clear();
            for (Map.Entry<BlockPos, BlockState> e : structure.blocks().entrySet()) {
                BlockPos pos = e.getKey();
                MovingBlockRenderState moving = new MovingBlockRenderState();
                moving.blockState = e.getValue();
                moving.randomSeedPos = pos;
                ShipRenderState.ShipBlock block = new ShipRenderState.ShipBlock(pos, e.getValue(), moving);
                all.add(block);
                if (!isEnclosed(structure, pos)) {
                    visible.add(block);
                }
            }
        }
    }

    /**
     * True when the block and all six neighbours are full, opaque cubes, so no face of the block can be seen. A block
     * that is not a full opaque cube (glass, the thruster bell, nose cones, fins) is never culled, and it stops its
     * neighbours from being culled, because it lets their faces show through. Air outside the ship is not solid.
     */
    static boolean isEnclosed(ShipStructure structure, BlockPos pos) {
        if (!isOpaqueCube(structure.get(pos))) {
            return false;
        }
        for (Direction d : Direction.values()) {
            if (!isOpaqueCube(structure.get(pos.relative(d)))) {
                return false;
            }
        }
        return true;
    }

    /** A model block whose occlusion shape is a full cube, so it hides what is behind it. */
    private static boolean isOpaqueCube(BlockState state) {
        return state.getRenderShape() == RenderShape.MODEL && state.isSolidRender();
    }
}
