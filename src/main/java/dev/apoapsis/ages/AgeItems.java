package dev.apoapsis.ages;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.flight.FlightDimensions;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Pale regolith sample. Pale's ground drops it now and then for a pilot at Age 4 or above, and bringing one home to
 * Verdance grants Age 5 (see {@link Firsts#sampleAgeDue}). Server side: no drop in creative.
 */
final class AgeItems {
    private AgeItems() {}

    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Apoapsis.MODID);

    static final DeferredItem<PaleSample> PALE_SAMPLE = ITEMS.registerItem("pale_sample", PaleSample::new);

    /** One break in this many on Pale's ground drops a sample. */
    static final int SAMPLE_ODDS = 6;
    /** Lowest Age that can collect a sample from Pale's ground. */
    static final int SAMPLE_MIN_AGE = 4;

    private static final Identifier REGOLITH = Identifier.fromNamespaceAndPath(Apoapsis.MODID, "regolith");
    /** The grid's creative tab, matched by id so this package has no compile dependency on the grid. */
    private static final ResourceKey<CreativeModeTab> GRID_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(Apoapsis.MODID, "apoapsis"));

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(AgeItems::onBuildTab);
        NeoForge.EVENT_BUS.addListener(AgeItems::onBreak);
    }

    /** Puts the sample in the grid's creative tab. */
    static void onBuildTab(BuildCreativeModeTabContentsEvent event) {
        if (GRID_TAB.equals(event.getTabKey())) {
            event.accept(new ItemStack(PALE_SAMPLE.get()));
        }
    }

    /** A pilot at Age 4 or above breaks Pale's regolith or basalt: one break in six also drops a sample. */
    static void onBreak(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !FlightDimensions.PALE.equals(level.dimension())) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()
                || Ages.getAge(player) < SAMPLE_MIN_AGE || !isPaleGround(event.getState())) {
            return;
        }
        if (level.getRandom().nextInt(SAMPLE_ODDS) == 0) {
            Block.popResource(level, event.getPos(), new ItemStack(PALE_SAMPLE.get()));
        }
    }

    private static boolean isPaleGround(BlockState state) {
        return state.is(Blocks.BASALT) || REGOLITH.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    /** A plain item with the one tooltip line. */
    static final class PaleSample extends Item {
        PaleSample(Item.Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                    Consumer<Component> builder, TooltipFlag flag) {
            builder.accept(Component.translatable("item.apoapsis.pale_sample.tooltip").withStyle(ChatFormatting.GRAY));
        }
    }
}
