package dev.apoapsis.grid;

import dev.apoapsis.Apoapsis;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers the ship blocks, their items, the fuel tank block entity and the creative tab.
 * Registry ids are fixed by docs/dev/prototype-contract.md. Owned by node BLK.
 */
public final class GridBlocks {
    private GridBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Apoapsis.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Apoapsis.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Apoapsis.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Apoapsis.MODID);

    /** Blocks that flood-fill assembly may include: our ship blocks plus the vanilla parts listed in the data tag. */
    public static final TagKey<Block> SHIP_BLOCKS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Apoapsis.MODID, "ship_blocks"));

    /** Iron-strength plating: iron block hardness and resistance, metal sound, stone tool or better. */
    public static final DeferredBlock<Block> HULL_PLATE = BLOCKS.registerSimpleBlock("hull_plate", GridBlocks::metal);

    /** Horizontal FACING: the canopy faces the placer's look direction; right-click to board. */
    public static final DeferredBlock<CockpitBlock> COCKPIT = BLOCKS.registerBlock("cockpit", CockpitBlock::new, GridBlocks::metal);

    public static final DeferredBlock<Block> GYROSCOPE = BLOCKS.registerSimpleBlock("gyroscope", GridBlocks::metal);

    /** Holds hydrolox in its block entity. */
    public static final DeferredBlock<FuelTankBlock> FUEL_TANK = BLOCKS.registerBlock("fuel_tank", FuelTankBlock::new, GridBlocks::metal);

    /** Turns water and furnace heat into hydrolox for the fuel tanks beside it. Lit while refining. */
    public static final DeferredBlock<FuelRefineryBlock> FUEL_REFINERY =
            BLOCKS.registerBlock("fuel_refinery", FuelRefineryBlock::new, GridBlocks::metal);

    /** FACING is the exhaust direction, all six directions. Not occluding: the bell is not a full cube, so it is never culled. */
    public static final DeferredBlock<SparkThrusterBlock> SPARK_THRUSTER =
            BLOCKS.registerBlock("spark_thruster", SparkThrusterBlock::new, p -> metal(p).noOcclusion());

    /** Tapered cap; FACING is the tip direction (up or down when placed). Not a full cube. */
    public static final DeferredBlock<NoseConeBlock> NOSE_CONE =
            BLOCKS.registerBlock("nose_cone", NoseConeBlock::new, p -> metal(p).noOcclusion());

    /** Thin swept fin; horizontal FACING. Not a full cube. */
    public static final DeferredBlock<FinBlock> FIN = BLOCKS.registerBlock("fin", FinBlock::new, p -> metal(p).noOcclusion());

    /** Hull block with a translucent glass porthole on its sides. Not occluding, so the glass shows the ship inside. */
    public static final DeferredBlock<Block> WINDOW = BLOCKS.registerSimpleBlock("window", p -> metal(p).noOcclusion());

    /** Pale ground: sand-like, light grey on maps. */
    public static final DeferredBlock<Block> REGOLITH = BLOCKS.registerSimpleBlock("regolith",
            p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.5f).sound(SoundType.SAND));

    public static final DeferredItem<BlockItem> HULL_PLATE_ITEM = ITEMS.registerSimpleBlockItem("hull_plate", HULL_PLATE);
    public static final DeferredItem<BlockItem> COCKPIT_ITEM = ITEMS.registerSimpleBlockItem("cockpit", COCKPIT);
    public static final DeferredItem<BlockItem> GYROSCOPE_ITEM = ITEMS.registerSimpleBlockItem("gyroscope", GYROSCOPE);
    public static final DeferredItem<BlockItem> FUEL_TANK_ITEM = ITEMS.registerSimpleBlockItem("fuel_tank", FUEL_TANK);
    public static final DeferredItem<BlockItem> FUEL_REFINERY_ITEM = ITEMS.registerSimpleBlockItem("fuel_refinery", FUEL_REFINERY);
    public static final DeferredItem<BlockItem> SPARK_THRUSTER_ITEM = ITEMS.registerSimpleBlockItem("spark_thruster", SPARK_THRUSTER);
    public static final DeferredItem<BlockItem> NOSE_CONE_ITEM = ITEMS.registerSimpleBlockItem("nose_cone", NOSE_CONE);
    public static final DeferredItem<BlockItem> FIN_ITEM = ITEMS.registerSimpleBlockItem("fin", FIN);
    public static final DeferredItem<BlockItem> WINDOW_ITEM = ITEMS.registerSimpleBlockItem("window", WINDOW);
    public static final DeferredItem<BlockItem> REGOLITH_ITEM = ITEMS.registerSimpleBlockItem("regolith", REGOLITH);

    /** Stacks to 16. Right-click a fuel tank to add 300 kg of hydrolox. */
    public static final DeferredItem<Item> HYDROLOX_CANISTER = ITEMS.registerSimpleItem("hydrolox_canister", p -> p.stacksTo(16));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FuelTankBlockEntity>> FUEL_TANK_BE = BLOCK_ENTITY_TYPES.register(
            "fuel_tank",
            () -> new BlockEntityType<FuelTankBlockEntity>(
                    (pos, state) -> new FuelTankBlockEntity(GridBlocks.FUEL_TANK_BE.get(), pos, state), FUEL_TANK.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FuelRefineryBlockEntity>> FUEL_REFINERY_BE = BLOCK_ENTITY_TYPES.register(
            "fuel_refinery",
            () -> new BlockEntityType<FuelRefineryBlockEntity>(
                    (pos, state) -> new FuelRefineryBlockEntity(GridBlocks.FUEL_REFINERY_BE.get(), pos, state), FUEL_REFINERY.get()));

    /** Creative tab with the grid items. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("apoapsis", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.apoapsis"))
            .icon(() -> new ItemStack(HULL_PLATE_ITEM.get()))
            .displayItems((parameters, output) -> {
                output.accept(HULL_PLATE_ITEM.get());
                output.accept(COCKPIT_ITEM.get());
                output.accept(GYROSCOPE_ITEM.get());
                output.accept(FUEL_TANK_ITEM.get());
                output.accept(FUEL_REFINERY_ITEM.get());
                output.accept(SPARK_THRUSTER_ITEM.get());
                output.accept(NOSE_CONE_ITEM.get());
                output.accept(FIN_ITEM.get());
                output.accept(WINDOW_ITEM.get());
                output.accept(REGOLITH_ITEM.get());
                output.accept(HYDROLOX_CANISTER.get());
            })
            .build());

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        TABS.register(modBus);
    }

    /** Metal properties shared by the ship blocks: iron strength, metal sound, stone tool needed for drops. */
    private static BlockBehaviour.Properties metal(BlockBehaviour.Properties p) {
        return p.mapColor(MapColor.METAL).strength(5.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }
}
