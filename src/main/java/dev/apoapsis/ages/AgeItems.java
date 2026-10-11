package dev.apoapsis.ages;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.flight.FlightDimensions;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Pale regolith sample. When Pale's ground breaks it can drop one with the block, for a pilot at Age 4 or above and at
 * most once per 60 s per pilot. Bringing one home to Verdance grants Age 5 (see {@link Firsts#sampleAgeDue}). Server side:
 * no drop in creative.
 */
final class AgeItems {
    private AgeItems() {}

    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Apoapsis.MODID);

    static final DeferredItem<PaleSample> PALE_SAMPLE = ITEMS.registerItem("pale_sample", PaleSample::new);

    /** One break in this many on Pale's ground drops a sample. */
    static final int SAMPLE_ODDS = 6;
    /** Lowest Age that can collect a sample from Pale's ground. */
    static final int SAMPLE_MIN_AGE = 4;
    /** A pilot can roll for a sample again this long (60 s) after one dropped: blocks do not record who placed them. */
    static final int SAMPLE_COOLDOWN_TICKS = 60 * 20;
    /** Game time at which each pilot may roll again; server thread only, cleared when the server stops. */
    private static final Map<UUID, Long> SAMPLE_READY = new HashMap<>();

    private static final Identifier REGOLITH = Identifier.fromNamespaceAndPath(Apoapsis.MODID, "regolith");
    /** The grid's creative tab, matched by id so this package has no compile dependency on the grid. */
    private static final ResourceKey<CreativeModeTab> GRID_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(Apoapsis.MODID, "apoapsis"));

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(AgeItems::onBuildTab);
        NeoForge.EVENT_BUS.addListener(AgeItems::onDrops);
        NeoForge.EVENT_BUS.addListener(AgeItems::onServerStopped);
    }

    /** Puts the sample in the grid's creative tab. */
    static void onBuildTab(BuildCreativeModeTabContentsEvent event) {
        if (GRID_TAB.equals(event.getTabKey())) {
            event.accept(new ItemStack(PALE_SAMPLE.get()));
        }
    }

    /**
     * A pilot at Age 4 or above breaks Pale's regolith or basalt and the block drops: one break in six also drops a
     * sample, at most one per 60 s per pilot (the game does not record who placed a block, so this stops farming).
     */
    static void onDrops(BlockDropsEvent event) {
        ServerLevel level = event.getLevel();
        if (!FlightDimensions.PALE.equals(level.dimension())) {
            return;
        }
        if (!(event.getBreaker() instanceof ServerPlayer player) || player.isCreative()
                || Ages.getAge(player) < SAMPLE_MIN_AGE || !isPaleGround(event.getState())) {
            return;
        }
        long now = level.getGameTime();
        Long ready = SAMPLE_READY.get(player.getUUID());
        if (ready != null && now < ready) {
            return;
        }
        if (level.getRandom().nextInt(SAMPLE_ODDS) == 0) {
            SAMPLE_READY.put(player.getUUID(), now + SAMPLE_COOLDOWN_TICKS);
            BlockPos pos = event.getPos();
            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    new ItemStack(PALE_SAMPLE.get()));
            drop.setDefaultPickUpDelay();
            event.getDrops().add(drop);
        }
    }

    static void onServerStopped(ServerStoppedEvent event) {
        SAMPLE_READY.clear();
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
