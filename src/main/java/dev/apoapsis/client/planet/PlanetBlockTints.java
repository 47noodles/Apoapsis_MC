package dev.apoapsis.client.planet;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.grid.GridBlocks;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

import java.util.List;

/** Block tints for the planets: Verdance's greens, leaves and water, and Pale's regolith. Client only. */
@EventBusSubscriber(modid = Apoapsis.MODID, value = Dist.CLIENT)
public final class PlanetBlockTints {
    private static final List<Block> GRASS_TINTED = List.of(
            Blocks.GRASS_BLOCK, Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN, Blocks.SUGAR_CANE);
    private static final List<Block> FOLIAGE_TINTED = List.of(
            Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES, Blocks.JUNGLE_LEAVES,
            Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.VINE);

    private PlanetBlockTints() {}

    @SubscribeEvent
    static void onBlockTints(RegisterColorHandlersEvent.BlockTintSources event) {
        BlockColors colors = event.getBlockColors();
        for (Block block : GRASS_TINTED) {
            wrap(event, colors, block, PlanetTints.VERDANCE_GRASS, PlanetTints.VERDANCE_GRASS_AMOUNT);
        }
        for (Block block : FOLIAGE_TINTED) {
            wrap(event, colors, block, PlanetTints.VERDANCE_FOLIAGE, PlanetTints.VERDANCE_FOLIAGE_AMOUNT);
        }
        wrap(event, colors, Blocks.WATER, PlanetTints.VERDANCE_WATER, PlanetTints.VERDANCE_WATER_AMOUNT);
        wrap(event, colors, Blocks.WATER_CAULDRON, PlanetTints.VERDANCE_WATER, PlanetTints.VERDANCE_WATER_AMOUNT);

        event.register(List.of(new BlockTintSource() {
            @Override
            public int color(BlockState state) {
                return -1;
            }

            @Override
            public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
                return PlanetTints.isPale(level) ? PlanetTints.PALE_REGOLITH : -1;
            }
        }), GridBlocks.REGOLITH.get());
    }

    /** Re-registers a block with its vanilla tint source wrapped in a Verdance shift. */
    private static void wrap(RegisterColorHandlersEvent.BlockTintSources event, BlockColors colors, Block block, int target, float amount) {
        BlockTintSource base = colors.getTintSource(block.defaultBlockState(), 0);
        if (base != null) {
            event.register(List.of(PlanetTints.verdanceShift(base, target, amount)), block);
        }
    }
}
