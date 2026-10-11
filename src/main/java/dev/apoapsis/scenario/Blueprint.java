package dev.apoapsis.scenario;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.apoapsis.Apoapsis;
import dev.apoapsis.grid.FuelTankBlockEntity;
import dev.apoapsis.grid.ShipParts;
import dev.apoapsis.ship.ShipStructure;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A ship design read from {@code data/apoapsis/blueprint/<name>.json}: block-state strings at positions relative to
 * the cockpit (the cockpit is at the origin, the rest of the ship is below it).
 */
public record Blueprint(String name, List<Entry> entries) {
    /** One block of the design. */
    public record Entry(BlockPos pos, BlockState state) {
    }

    /** Loads a blueprint from the server's resource manager. Throws IllegalStateException if it is missing or malformed. */
    public static Blueprint load(MinecraftServer server, String name) {
        Identifier id = FileToIdConverter.json("blueprint").idToFile(Identifier.fromNamespaceAndPath(Apoapsis.MODID, name));
        try {
            Resource resource = server.getResourceManager().getResource(id)
                    .orElseThrow(() -> new IllegalStateException("missing " + id));
            JsonObject root;
            try (Reader reader = resource.openAsReader()) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }
            HolderLookup<Block> blocks = server.registryAccess().lookupOrThrow(Registries.BLOCK);
            List<Entry> entries = new ArrayList<>();
            JsonArray list = root.getAsJsonArray("blocks");
            for (JsonElement element : list) {
                JsonObject obj = element.getAsJsonObject();
                JsonArray p = obj.getAsJsonArray("pos");
                BlockPos pos = new BlockPos(p.get(0).getAsInt(), p.get(1).getAsInt(), p.get(2).getAsInt());
                BlockState state = BlockStateParser.parseForBlock(blocks, obj.get("state").getAsString(), false).blockState();
                entries.add(new Entry(pos, state));
            }
            return new Blueprint(root.has("name") ? root.get("name").getAsString() : name, List.copyOf(entries));
        } catch (IOException | CommandSyntaxException | RuntimeException e) {
            Apoapsis.LOG.error("Could not read blueprint {}", id, e);
            throw new IllegalStateException("Could not read blueprint " + id, e);
        }
    }

    /** The design as a ship with every fuel tank full. */
    public ShipStructure toStructure() {
        ShipStructure structure = new ShipStructure();
        for (Entry e : entries) {
            structure.put(e.pos(), e.state());
            if (ShipParts.tankCapacityKg(e.state()) > 0) {
                structure.setFuel(e.pos(), FuelTankBlockEntity.CAPACITY_KG);
            }
        }
        return structure;
    }
}
