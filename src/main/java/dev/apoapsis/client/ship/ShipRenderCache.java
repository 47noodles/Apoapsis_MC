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
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

/**
 * Client render data per ship, keyed by entity id. Int keys hold no reference to the ship, so an entry never keeps its
 * entity alive. Entries whose id no longer names a ship in the level are purged once a second, and the whole cache is
 * cleared on logout and on level unload. Client only.
 */
public final class ShipRenderCache {
    private static final Int2ObjectOpenHashMap<Entry> ENTRIES = new Int2ObjectOpenHashMap<>();
    /** Client ticks so far. Orientation samples are taken on these boundaries. */
    private static long clientTick;

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

    /** Called once per client tick, after the tick has run. */
    public static void advanceClientTick() {
        clientTick++;
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

    /** One ship's block lists and its last two orientation samples. */
    public static final class Entry {
        /** Every block of the structure, in map order, for change detection. */
        private final List<ShipRenderState.ShipBlock> all = new ArrayList<>();
        /** The blocks that are drawn: every block except the fully enclosed ones. */
        private final List<ShipRenderState.ShipBlock> visible = new ArrayList<>();
        /** Orientation at the last client tick boundary, and the latest synced orientation. */
        private final Quaternionf previous = new Quaternionf();
        private final Quaternionf current = new Quaternionf();
        private final Quaternionf shortest = new Quaternionf();
        private long sampleTick;
        private boolean sampled;

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

        /**
         * The ship's orientation {@code partialTicks} of the way from its previous client-tick sample to the latest
         * synced value: {@code slerp(previous, current, t)}. Each client tick the previous sample moves to the last
         * synced value seen, and the current sample follows the synced value as it arrives. A ship that stops turning
         * therefore settles on its last value, without snapping back to an older sample.
         *
         * @param synced        the synced body-to-dimension orientation
         * @param partialTicks  the frame's fraction of the way through the client tick, 0 to 1
         * @return a new quaternion, the interpolated orientation
         */
        public Quaternionf orientation(Quaternionfc synced, float partialTicks) {
            long tick = clientTick;
            if (!sampled) {
                previous.set(synced);
                sampled = true;
            } else if (tick != sampleTick) {
                previous.set(current);
            }
            sampleTick = tick;
            current.set(synced);
            // q and -q are the same rotation. Choose the sign that takes the short arc.
            if (previous.dot(current) < 0.0F) {
                shortest.set(-previous.x, -previous.y, -previous.z, -previous.w);
            } else {
                shortest.set(previous);
            }
            return shortest.slerp(current, Mth.clamp(partialTicks, 0.0F, 1.0F), new Quaternionf());
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
     * True when all six neighbours are full, opaque, drawn blocks of the ship, so no face of the block can be seen.
     * {@link BlockState#isSolidRender()} is true only for a full-cube occlusion shape, which excludes glass and the
     * partial models (cockpit, thrusters, fuel tank). Air outside the ship is not solid.
     */
    static boolean isEnclosed(ShipStructure structure, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockState neighbour = structure.get(pos.relative(d));
            if (neighbour.getRenderShape() != RenderShape.MODEL || !neighbour.isSolidRender()) {
                return false;
            }
        }
        return true;
    }
}
