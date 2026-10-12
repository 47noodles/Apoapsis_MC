package dev.apoapsis.test;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.planet.SurfaceKind;
import dev.apoapsis.planet.TerrainSampler;
import dev.apoapsis.test.AccuracyTests.Stats;
import dev.apoapsis.test.TerrainProbe.Body;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The classified half of the accuracy report on Verdance: which kind of ground the world generator really put on top of
 * each column (grass and dirt, stone, sand, red rock, gravel, snow and ice, water) against the kind the far terrain
 * draws, as a confusion matrix, with the colour error of the land columns by kind and the real top blocks by biome. Also
 * the biome search that picks the chunks to compare: a few of each biome that matters for colour, found by scanning the
 * body on the seed, so mountains, deserts, badlands, beaches, rivers, oceans and snowy land are all in the sample.
 */
final class SurfaceClassTests {
    /** The classes the report tabulates, in table order. */
    static final SurfaceKind[] KINDS = {SurfaceKind.GRASS, SurfaceKind.STONE, SurfaceKind.SAND, SurfaceKind.RED_ROCK,
            SurfaceKind.GRAVEL, SurfaceKind.SNOW, SurfaceKind.WATER};

    /** Mean of the grass block's top texture, a grey: the biome tint multiplies it. */
    static final int GRASS_TEXTURE = 0x939393;

    /**
     * The grass colour the vanilla colour map gives each biome's temperature and downfall, measured from the 26.3
     * colour map texture (a biome with its own grass colour or a colour modifier is handled in {@link #grassTint}).
     */
    private static final Map<String, Integer> GRASS_MAP = Map.ofEntries(
            Map.entry("minecraft:badlands", 0xBFB755),
            Map.entry("minecraft:bamboo_jungle", 0x59C93C),
            Map.entry("minecraft:beach", 0x91BD59),
            Map.entry("minecraft:birch_forest", 0x88BB67),
            Map.entry("minecraft:cherry_grove", 0x83BB6D),
            Map.entry("minecraft:cold_ocean", 0x8EB971),
            Map.entry("minecraft:dappled_forest", 0x88BB67),
            Map.entry("minecraft:dark_forest", 0x79C05A),
            Map.entry("minecraft:deep_cold_ocean", 0x8EB971),
            Map.entry("minecraft:deep_frozen_ocean", 0x8EB971),
            Map.entry("minecraft:deep_lukewarm_ocean", 0x8EB971),
            Map.entry("minecraft:deep_ocean", 0x8EB971),
            Map.entry("minecraft:desert", 0xBFB755),
            Map.entry("minecraft:eroded_badlands", 0xBFB755),
            Map.entry("minecraft:flower_forest", 0x79C05A),
            Map.entry("minecraft:forest", 0x79C05A),
            Map.entry("minecraft:frozen_ocean", 0x80B497),
            Map.entry("minecraft:frozen_peaks", 0x80B497),
            Map.entry("minecraft:frozen_river", 0x80B497),
            Map.entry("minecraft:grove", 0x80B497),
            Map.entry("minecraft:ice_spikes", 0x80B497),
            Map.entry("minecraft:jagged_peaks", 0x80B497),
            Map.entry("minecraft:jungle", 0x59C93C),
            Map.entry("minecraft:lukewarm_ocean", 0x8EB971),
            Map.entry("minecraft:mangrove_swamp", 0x6AC44E),
            Map.entry("minecraft:meadow", 0x83BB6D),
            Map.entry("minecraft:mushroom_fields", 0x55C93F),
            Map.entry("minecraft:ocean", 0x8EB971),
            Map.entry("minecraft:old_growth_birch_forest", 0x88BB67),
            Map.entry("minecraft:old_growth_pine_taiga", 0x86B87F),
            Map.entry("minecraft:old_growth_spruce_taiga", 0x86B783),
            Map.entry("minecraft:pale_garden", 0x79C05A),
            Map.entry("minecraft:plains", 0x91BD59),
            Map.entry("minecraft:river", 0x8EB971),
            Map.entry("minecraft:savanna", 0xBFB755),
            Map.entry("minecraft:savanna_plateau", 0xBFB755),
            Map.entry("minecraft:snowy_beach", 0x83B593),
            Map.entry("minecraft:snowy_plains", 0x80B497),
            Map.entry("minecraft:snowy_slopes", 0x80B497),
            Map.entry("minecraft:snowy_taiga", 0x80B497),
            Map.entry("minecraft:sparse_jungle", 0x64C73F),
            Map.entry("minecraft:stony_peaks", 0x9ABE4B),
            Map.entry("minecraft:stony_shore", 0x8AB689),
            Map.entry("minecraft:sunflower_plains", 0x91BD59),
            Map.entry("minecraft:swamp", 0x6AC44E),
            Map.entry("minecraft:taiga", 0x86B783),
            Map.entry("minecraft:warm_ocean", 0x8EB971),
            Map.entry("minecraft:windswept_forest", 0x8AB689),
            Map.entry("minecraft:windswept_gravelly_hills", 0x8AB689),
            Map.entry("minecraft:windswept_hills", 0x8AB689),
            Map.entry("minecraft:windswept_savanna", 0xBFB755),
            Map.entry("minecraft:wooded_badlands", 0xBFB755));

    private SurfaceClassTests() {}

    /** The class of a block that can lie on top of the ground. */
    static SurfaceKind kindOf(BlockState state) {
        if (state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.SNOW) || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)) {
            return SurfaceKind.SNOW;
        }
        if (state.is(Blocks.GRAVEL)) {
            return SurfaceKind.GRAVEL;
        }
        if (state.is(Blocks.RED_SAND) || state.is(Blocks.RED_SANDSTONE) || state.is(BlockTags.TERRACOTTA)) {
            return SurfaceKind.RED_ROCK;
        }
        if (state.is(Blocks.SAND) || state.is(Blocks.SANDSTONE)) {
            return SurfaceKind.SAND;
        }
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM) || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.MUD) || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.DIRT_PATH)) {
            return SurfaceKind.GRASS;
        }
        return SurfaceKind.STONE;
    }

    /** The colour a grass block's top shows in a biome at a column: the biome's grass colour times the texture. */
    static int grassColour(Biome biome, String biomeId, int x, int z) {
        Integer fromMap = GRASS_MAP.get(biomeId);
        int base = biome.getSpecialEffects().grassColorOverride().orElse(fromMap == null ? 0x91BD59 : fromMap);
        int tint = biome.getSpecialEffects().grassColorModifier().modifyColor(x, z, base);
        int out = 0;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int channel = (tint >> shift) & 0xFF;
            out |= Math.round(channel * ((GRASS_TEXTURE >> shift) & 0xFF) / 255f) << shift;
        }
        return out;
    }

    // ---- the biome search -------------------------------------------------------------------------------------------

    /**
     * A group of biomes the sample wants some of, with the ground heights that count for it; {@code high} takes the
     * highest ground first, for the mountains.
     */
    private record Category(String label, boolean high, int minGround, int maxGround, Predicate<Holder<Biome>> match) {}

    @SafeVarargs
    private static Category category(String label, boolean high, ResourceKey<Biome>... keys) {
        return category(label, high, Integer.MIN_VALUE, Integer.MAX_VALUE, keys);
    }

    @SafeVarargs
    private static Category category(String label, boolean high, int minGround, int maxGround,
            ResourceKey<Biome>... keys) {
        return new Category(label, high, minGround, maxGround, holder -> {
            for (ResourceKey<Biome> key : keys) {
                if (holder.is(key)) {
                    return true;
                }
            }
            return false;
        });
    }

    private static final List<Category> CATEGORIES = List.of(
            category("stony peaks", true, Biomes.STONY_PEAKS),
            category("jagged peaks", true, Biomes.JAGGED_PEAKS),
            category("frozen peaks", true, Biomes.FROZEN_PEAKS),
            category("snowy slopes and grove", true, Biomes.SNOWY_SLOPES, Biomes.GROVE),
            category("windswept hills", true, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_FOREST),
            category("windswept gravelly hills", true, Biomes.WINDSWEPT_GRAVELLY_HILLS),
            category("windswept savanna", false, Biomes.WINDSWEPT_SAVANNA),
            category("meadow and cherry grove", true, Biomes.MEADOW, Biomes.CHERRY_GROVE),
            category("desert", false, Biomes.DESERT),
            category("low badlands", false, 63, 76, Biomes.BADLANDS, Biomes.ERODED_BADLANDS),
            category("high badlands", false, 82, Integer.MAX_VALUE, Biomes.BADLANDS, Biomes.ERODED_BADLANDS),
            category("wooded badlands", false, Biomes.WOODED_BADLANDS),
            category("beach", false, Biomes.BEACH, Biomes.SNOWY_BEACH),
            category("stony shore", false, Biomes.STONY_SHORE),
            category("river", false, Biomes.RIVER),
            category("frozen river", false, Biomes.FROZEN_RIVER),
            category("ocean", false, Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.LUKEWARM_OCEAN,
                    Biomes.DEEP_LUKEWARM_OCEAN, Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN),
            category("warm ocean", false, Biomes.WARM_OCEAN),
            category("frozen ocean", false, Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN),
            category("snowy plains", false, Biomes.SNOWY_PLAINS),
            category("ice spikes", false, Biomes.ICE_SPIKES),
            category("snowy taiga", false, Biomes.SNOWY_TAIGA),
            category("swamp", false, Biomes.SWAMP, Biomes.MANGROVE_SWAMP),
            category("jungle", false, Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE),
            category("savanna", false, Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU),
            category("taiga", false, Biomes.TAIGA),
            category("old growth taiga", false, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA),
            category("forest", false, Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST,
                    Biomes.OLD_GROWTH_BIRCH_FOREST),
            category("dark forest", false, Biomes.DARK_FOREST, Biomes.DAPPLED_FOREST),
            category("plains", false, Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS),
            category("mushroom fields", false, Biomes.MUSHROOM_FIELDS));

    /** Scan step of the biome search, blocks: fine enough to land on a river or a beach now and then. */
    private static final int SCAN_STEP = 96;
    /** The search covers this far from the origin in x and z, blocks. */
    private static final int SCAN_RANGE = 24_000;
    /** Two picks of one category are at least this far apart, blocks. */
    private static final int MIN_SEPARATION = 1_500;

    /** A column found by the biome search: its category and where it is. */
    record Pick(String category, int x, int z, int ground, String biome) {
        ChunkPos chunk() {
            return new ChunkPos(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
        }
    }

    /**
     * Scans the body on a grid for the biome at the ground of every cell and picks {@code perCategory} cells of each
     * category, far apart: the highest ground first for the mountain categories, a seeded shuffle for the rest.
     * Deterministic for the seed. Categories the scan does not find are logged and left out.
     */
    static List<Pick> spread(Body body, int perCategory, long seed) {
        BiomeResolver biomes = body.generator().getBiomeSource().createCachingResolver(body.randomState());
        TerrainSampler.Worker worker = body.sampler().worker();
        Map<String, List<Pick>> found = new TreeMap<>();
        long t0 = System.nanoTime();
        for (int z = -SCAN_RANGE; z <= SCAN_RANGE; z += SCAN_STEP) {
            for (int x = -SCAN_RANGE; x <= SCAN_RANGE; x += SCAN_STEP) {
                int ground = Math.round(worker.ground(x, z));
                Holder<Biome> biome = biomes.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(ground),
                        QuartPos.fromBlock(z));
                for (Category c : CATEGORIES) {
                    if (ground >= c.minGround() && ground <= c.maxGround() && c.match().test(biome)) {
                        found.computeIfAbsent(c.label(), k -> new ArrayList<>())
                                .add(new Pick(c.label(), x, z, ground, biome.getRegisteredName()));
                    }
                }
            }
        }
        Random rnd = new Random(seed);
        List<Pick> out = new ArrayList<>();
        StringBuilder log = new StringBuilder();
        for (Category c : CATEGORIES) {
            List<Pick> cells = found.getOrDefault(c.label(), new ArrayList<>());
            log.append(String.format("%s %d/%d, ", c.label(), Math.min(perCategory, cells.size()), cells.size()));
            if (c.high()) {
                cells.sort((a, b) -> Integer.compare(b.ground(), a.ground()));
            } else {
                Collections.shuffle(cells, rnd);
            }
            List<Pick> picked = new ArrayList<>();
            for (Pick p : cells) {
                if (picked.size() >= perCategory) {
                    break;
                }
                boolean apart = true;
                for (Pick q : picked) {
                    if (Math.abs(p.x() - q.x()) < MIN_SEPARATION && Math.abs(p.z() - q.z()) < MIN_SEPARATION) {
                        apart = false;
                        break;
                    }
                }
                if (apart) {
                    picked.add(p);
                }
            }
            out.addAll(picked);
            for (Pick p : picked) {
                Apoapsis.LOG.info("[terrain-accuracy]   pick {} at {} {} (ground {}, {})", p.category(), p.x(), p.z(),
                        p.ground(), p.biome());
            }
        }
        Apoapsis.LOG.info("[terrain-accuracy] biome search over {} cells took {} ms; picked/found: {}",
                (2 * SCAN_RANGE / SCAN_STEP + 1) * (2 * SCAN_RANGE / SCAN_STEP + 1), (System.nanoTime() - t0) / 1_000_000,
                log);
        return out;
    }

    // ---- the report -------------------------------------------------------------------------------------------------

    /** What one body's sample says about the classes: the confusion matrix, colour error by kind and biome contents. */
    static final class ClassReport {
        final String name;
        final long[][] matrix = new long[KINDS.length][KINDS.length];
        final Map<SurfaceKind, Stats> colourByKind = new EnumMap<>(SurfaceKind.class);
        final Stats colour = new Stats("land colour");
        final Map<String, long[]> biomeCounts = new TreeMap<>();
        final Map<String, Map<String, Integer>> biomeBlocks = new TreeMap<>();
        final Map<String, Map<String, Integer>> biomeMisses = new TreeMap<>();
        long steepColumns;
        long columns;
        /** Columns of icebergs, which the colour error leaves out. */
        long icebergs;

        ClassReport(String name) {
            this.name = name;
        }

        private static int index(SurfaceKind kind) {
            for (int i = 0; i < KINDS.length; i++) {
                if (KINDS[i] == kind) {
                    return i;
                }
            }
            return -1;
        }

        /** Counts one column: the real class and top block against the class the far terrain drew. */
        void add(String biome, String topBlock, boolean steep, SurfaceKind real, SurfaceKind drawn) {
            int r = index(real);
            int d = index(drawn);
            if (r < 0 || d < 0) {
                return;
            }
            matrix[r][d]++;
            columns++;
            if (steep) {
                steepColumns++;
            }
            long[] row = biomeCounts.computeIfAbsent(biome, k -> new long[KINDS.length + 1]);
            row[r]++;
            row[KINDS.length] += steep ? 1 : 0;
            biomeBlocks.computeIfAbsent(biome, k -> new TreeMap<>()).merge(topBlock, 1, Integer::sum);
            if (real != drawn) {
                biomeMisses.computeIfAbsent(biome, k -> new TreeMap<>()).merge(real + " drawn as " + drawn, 1,
                        Integer::sum);
            }
        }

        /** A land column's colour error against the real top block's texture colour. */
        void addColour(SurfaceKind real, double error) {
            colour.add(error);
            colourByKind.computeIfAbsent(real, k -> new Stats(k.name())).add(error);
        }

        long rowTotal(SurfaceKind kind) {
            long n = 0;
            for (long v : matrix[index(kind)]) {
                n += v;
            }
            return n;
        }

        /** Fraction of the real columns of {@code kind} that the far terrain also drew as {@code kind}. */
        double recall(SurfaceKind kind) {
            long n = rowTotal(kind);
            return n == 0 ? 1 : (double) matrix[index(kind)][index(kind)] / n;
        }

        /** Fraction of the real columns of any of the given kinds that were drawn as their own kind. */
        double recall(SurfaceKind... kinds) {
            long hit = 0;
            long n = 0;
            for (SurfaceKind k : kinds) {
                hit += matrix[index(k)][index(k)];
                n += rowTotal(k);
            }
            return n == 0 ? 1 : (double) hit / n;
        }

        /** Fraction of all columns drawn as their real class. */
        double accuracy() {
            long hit = 0;
            for (int i = 0; i < KINDS.length; i++) {
                hit += matrix[i][i];
            }
            return columns == 0 ? 1 : (double) hit / columns;
        }

        void log() {
            Apoapsis.LOG.info("[terrain-accuracy] {}: {} columns, {} steep by the vanilla rule; class match {}%",
                    name, columns, steepColumns, Math.round(100 * accuracy()));
            StringBuilder header = new StringBuilder(String.format("%-9s %7s |", "real\\drawn", "n"));
            for (SurfaceKind k : KINDS) {
                header.append(String.format(" %8s", k.name()));
            }
            Apoapsis.LOG.info("[terrain-accuracy]   {}", header);
            for (SurfaceKind real : KINDS) {
                long n = rowTotal(real);
                StringBuilder row = new StringBuilder(String.format("%-9s %7d |", real.name(), n));
                for (SurfaceKind drawn : KINDS) {
                    row.append(String.format(" %7d%%", n == 0 ? 0 : Math.round(100.0 * matrix[index(real)][index(drawn)] / n)));
                }
                Apoapsis.LOG.info("[terrain-accuracy]   {}", row);
            }
            Apoapsis.LOG.info("[terrain-accuracy]   stone, sand and gravel columns drawn as their own class: {}%;"
                    + " {} iceberg columns are left out of the table and the colour error", Math.round(100
                    * recall(SurfaceKind.STONE, SurfaceKind.SAND, SurfaceKind.GRAVEL)), icebergs);
            if (colour.n() > 0) {
                Apoapsis.LOG.info("[terrain-accuracy]   {}; {}% within 3 percent, {}% within 5 percent", colour.line(),
                        Math.round(100 * colour.fractionWithin(0.03)), Math.round(100 * colour.fractionWithin(0.05)));
            }
            for (Stats s : colourByKind.values()) {
                Apoapsis.LOG.info("[terrain-accuracy]   colour of real {}: n={} rms {} p90 {}; {}% within 5 percent",
                        s.label, s.n(), String.format("%.3f", s.rms()), String.format("%.3f", s.within(0.9)),
                        Math.round(100 * s.fractionWithin(0.05)));
            }
            for (Map.Entry<String, long[]> e : biomeCounts.entrySet()) {
                long n = 0;
                for (int i = 0; i < KINDS.length; i++) {
                    n += e.getValue()[i];
                }
                if (n < 120) {
                    continue;
                }
                final long total = n;
                StringBuilder line = new StringBuilder(String.format("%-32s n=%5d steep %3d%% real:", e.getKey(), n,
                        Math.round(100.0 * e.getValue()[KINDS.length] / n)));
                for (int i = 0; i < KINDS.length; i++) {
                    if (e.getValue()[i] * 20 >= n) {
                        line.append(String.format(" %s %d%%", KINDS[i].name(), Math.round(100.0 * e.getValue()[i] / n)));
                    }
                }
                line.append("; top blocks:");
                Map<String, Integer> blocks = biomeBlocks.get(e.getKey());
                blocks.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())).limit(4)
                        .forEach(b -> line.append(String.format(" %s %d%%", b.getKey().replace("minecraft:", ""),
                                Math.round(100.0 * b.getValue() / total))));
                Map<String, Integer> misses = biomeMisses.get(e.getKey());
                if (misses != null) {
                    line.append("; wrong:");
                    misses.entrySet().stream().filter(m -> m.getValue() * 50L >= total)
                            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())).limit(3)
                            .forEach(m -> line.append(String.format(" %s %d%%", m.getKey(),
                                    Math.round(100.0 * m.getValue() / total))));
                }
                Apoapsis.LOG.info("[terrain-accuracy]   {}", line);
            }
        }
    }

    /** The registry id of a block, for the report. */
    static String id(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }
}
