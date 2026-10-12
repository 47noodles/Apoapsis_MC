package dev.apoapsis.client.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jspecify.annotations.Nullable;

/**
 * A stand-in light engine that reports full sky light and no block light everywhere. A ship that has just crossed into
 * the surface dimension (the 2 km regime change) has no light data under it yet: the client has not been sent the chunk
 * and its light, so every block reads light 0 and the cockpit interior is drawn black for about a second. While that
 * lasts the renderer lights the ship with this engine instead ({@link ShipRenderer}). It never runs updates.
 */
final class OpenSkyLight extends LevelLightEngine {
    private static final LayerLightEventListener SKY = new Fixed(15);
    private static final LayerLightEventListener BLOCK = new Fixed(0);

    OpenSkyLight(LightChunkGetter chunkSource) {
        super(chunkSource, false, false);
    }

    @Override
    public LayerLightEventListener getLayerListener(LightLayer layer) {
        return layer == LightLayer.SKY ? SKY : BLOCK;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int skyDampen) {
        return 15 - skyDampen;
    }

    /** One layer with the same light value at every position. */
    private record Fixed(int value) implements LayerLightEventListener {
        @Override
        public @Nullable DataLayer getDataLayerData(SectionPos pos) {
            return null;
        }

        @Override
        public int getLightValue(BlockPos pos) {
            return value;
        }

        @Override
        public void checkBlock(BlockPos pos) {}

        @Override
        public boolean hasLightWork() {
            return false;
        }

        @Override
        public int runLightUpdates() {
            return 0;
        }

        @Override
        public void updateSectionStatus(SectionPos pos, boolean sectionEmpty) {}

        @Override
        public void setLightEnabled(ChunkPos pos, boolean enable) {}

        @Override
        public void propagateLightSources(ChunkPos pos) {}
    }
}
