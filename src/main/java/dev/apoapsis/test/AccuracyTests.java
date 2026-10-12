package dev.apoapsis.test;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.planet.SurfaceColumns;
import dev.apoapsis.planet.SurfaceKind;
import dev.apoapsis.planet.TerrainSampler;
import dev.apoapsis.planet.terrain.CraterFeature;
import dev.apoapsis.planet.terrain.CraterField;
import dev.apoapsis.planet.terrain.PaleFeatures;
import dev.apoapsis.test.ApoapsisGameTest.ShipCleanup;
import dev.apoapsis.test.SurfaceClassTests.ClassReport;
import dev.apoapsis.test.TerrainProbe.Body;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jspecify.annotations.Nullable;

/**
 * Accuracy of the far terrain against real chunks: for chunks spread over a body, the ground height and flat colour the
 * far-terrain tiles would draw ({@link SurfaceColumns}, the same code the tile generator runs) against what the
 * world generator really built there, the top solid block's height and the colour of its texture.
 */
final class AccuracyTests {
    /** Texture averages of the vanilla top blocks, 8-bit RGB, measured from the 26.3 block textures. */
    private static final Map<String, Integer> VANILLA = Map.ofEntries(
            Map.entry("minecraft:basalt", 0x515156),
            Map.entry("minecraft:stone", 0x7E7E7E),
            Map.entry("minecraft:sand", 0xDBCFA3),
            Map.entry("minecraft:red_sand", 0xBF6721),
            Map.entry("minecraft:terracotta", 0x985E44),
            Map.entry("minecraft:gravel", 0x847F7F),
            Map.entry("minecraft:snow_block", 0xF9FEFE),
            Map.entry("minecraft:dirt", 0x866043),
            Map.entry("minecraft:coarse_dirt", 0x77563B),
            Map.entry("minecraft:sandstone", 0xE0D6AA),
            Map.entry("minecraft:red_sandstone", 0xB5621F),
            Map.entry("minecraft:white_terracotta", 0xD2B2A1),
            Map.entry("minecraft:orange_terracotta", 0xA25426),
            Map.entry("minecraft:yellow_terracotta", 0xBA8523),
            Map.entry("minecraft:brown_terracotta", 0x4D3324),
            Map.entry("minecraft:red_terracotta", 0x8F3D2F),
            Map.entry("minecraft:light_gray_terracotta", 0x876B62),
            Map.entry("minecraft:podzol", 0x5C3F18),
            Map.entry("minecraft:mycelium", 0x6F6365),
            Map.entry("minecraft:mud", 0x3C393D),
            Map.entry("minecraft:rooted_dirt", 0x90684D),
            Map.entry("minecraft:calcite", 0xDFE0DD),
            Map.entry("minecraft:tuff", 0x6C6D67),
            Map.entry("minecraft:deepslate", 0x575759),
            Map.entry("minecraft:andesite", 0x888889),
            Map.entry("minecraft:granite", 0x956756),
            Map.entry("minecraft:diorite", 0xBDBCBD),
            Map.entry("minecraft:snow", 0xF9FEFE),
            Map.entry("minecraft:powder_snow", 0xF8FDFD),
            Map.entry("minecraft:packed_ice", 0x8EB4FA),
            Map.entry("minecraft:blue_ice", 0x74A8FD));

    private AccuracyTests() {}

    /**
     * The real column: height of the top face of the top solid block, the block that shows on top (the ground, or the
     * snow layer or floating ice over it), whether water lies over the ground, and the class of what the player sees
     * from above (water, if the highest thing is a fluid).
     */
    record Real(int height, BlockState top, boolean underwater, SurfaceKind kind) {}

    /**
     * Scans a chunk column from the top down for the first block that is neither air nor fluid. An iceberg (ice that
     * floats on water: frozen oceans grow them in the surface step) is not ground: the scan goes on down to the sea
     * floor under it, as the far terrain draws the floor and not the berg. A snow layer is not ground either: the height
     * is the ground's, and the column counts as snow.
     */
    static Real real(ChunkAccess chunk, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, 0, z);
        boolean water = false;
        boolean berg = false;
        SurfaceKind seen = null;
        BlockState visible = null;
        for (int y = chunk.getMaxY(); y >= chunk.getMinY(); y--) {
            BlockState state = chunk.getBlockState(pos.setY(y));
            if (!state.getFluidState().isEmpty()) {
                if (seen == null) {
                    seen = SurfaceKind.WATER;
                }
                water = true;
            } else if (!state.isAir()) {
                if (state.is(Blocks.SNOW)) {
                    seen = seen == null ? SurfaceKind.SNOW : seen;
                    visible = visible == null ? state : visible;
                } else if (isIce(state) && (berg || !water && floatsOnWater(chunk, pos, y))) {
                    berg = true;
                    seen = seen == null ? SurfaceKind.SNOW : seen;
                    visible = visible == null ? state : visible;
                } else {
                    return new Real(y + 1, visible != null ? visible : state, water,
                            seen != null ? seen : SurfaceClassTests.kindOf(state));
                }
            }
        }
        return new Real(chunk.getMinY(), Blocks.AIR.defaultBlockState(), water, SurfaceKind.WATER);
    }

    private static boolean isIce(BlockState state) {
        return state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.SNOW_BLOCK);
    }

    /** True when the run of ice from {@code startY} down ends over water: an iceberg, not a glacier. */
    private static boolean floatsOnWater(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int startY) {
        for (int y = startY; y >= chunk.getMinY(); y--) {
            BlockState state = chunk.getBlockState(pos.setY(y));
            if (!state.getFluidState().isEmpty()) {
                return true;
            }
            if (!isIce(state) && !state.isAir()) {
                return false;
            }
        }
        return false;
    }

    /** The texture-average colour of a top block, or null for a block the tests have no reference for. */
    static @Nullable Integer reference(BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        if (id.equals("apoapsis:regolith")) {
            return Regolith.COLOUR;
        }
        return VANILLA.get(id);
    }

    /** The mod's own regolith texture, averaged: read from the classpath so it follows the art. */
    private static final class Regolith {
        static final int COLOUR = average("/assets/apoapsis/textures/block/regolith.png");

        private static int average(String resource) {
            try (InputStream in = AccuracyTests.class.getResourceAsStream(resource)) {
                if (in == null) {
                    throw new IllegalStateException(resource + " is not on the classpath");
                }
                java.awt.image.BufferedImage image = ImageIO.read(in);
                long r = 0, g = 0, b = 0;
                int n = image.getWidth() * image.getHeight();
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int argb = image.getRGB(x, y);
                        r += (argb >> 16) & 0xFF;
                        g += (argb >> 8) & 0xFF;
                        b += argb & 0xFF;
                    }
                }
                return (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
            } catch (IOException e) {
                throw new IllegalStateException("cannot read " + resource, e);
            }
        }
    }

    /** Largest difference of any colour channel, as a fraction of the full 0..255 range. */
    static double colourError(int a, int b) {
        int dr = Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF));
        int dg = Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF));
        int db = Math.abs((a & 0xFF) - (b & 0xFF));
        return Math.max(dr, Math.max(dg, db)) / 255.0;
    }

    /** Running statistics of a signed error, in blocks or as a fraction. */
    static final class Stats {
        final String label;
        final DoubleArrayList values = new DoubleArrayList();

        Stats(String label) {
            this.label = label;
        }

        void add(double v) {
            values.add(v);
        }

        int n() {
            return values.size();
        }

        double bias() {
            return n() == 0 ? 0 : values.doubleStream().sum() / n();
        }

        double rms() {
            return n() == 0 ? 0 : Math.sqrt(values.doubleStream().map(v -> v * v).sum() / n());
        }

        /** The smallest bound that {@code fraction} of the samples are within (absolute value). */
        double within(double fraction) {
            if (n() == 0) {
                return 0;
            }
            double[] abs = values.doubleStream().map(Math::abs).sorted().toArray();
            return abs[Math.min(abs.length - 1, (int) Math.ceil(abs.length * fraction) - 1)];
        }

        double max() {
            return values.doubleStream().map(Math::abs).max().orElse(0);
        }

        /** Fraction of the samples whose absolute error is at most {@code bound}. */
        double fractionWithin(double bound) {
            return n() == 0 ? 1 : (double) values.doubleStream().filter(v -> Math.abs(v) <= bound).count() / n();
        }

        String line() {
            return String.format("%s n=%d bias %.3f rms %.3f p50 %.3f p90 %.3f p95 %.3f p99 %.3f max %.3f", label, n(),
                    bias(), rms(), within(0.5), within(0.9), within(0.95), within(0.99), max());
        }
    }

    /** The result of comparing one body with its real chunks. */
    record Result(Stats height, Stats terrainHeight, Stats craterHeight, Stats colour, Stats craterColour,
            Stats exactHeight, Stats agreeing, Map<String, Stats> colourByBlock, Map<String, Stats> heightByBlock) {}

    /** Seeded random chunk positions within a body's radius. */
    static List<ChunkPos> randomChunks(Body b, int count, long seed) {
        Random rnd = new Random(seed);
        int span = b.radius() / 16;
        List<ChunkPos> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            out.add(new ChunkPos(rnd.nextInt(2 * span) - span, rnd.nextInt(span) - span / 2));
        }
        return out;
    }

    /**
     * True for a column of an iceberg: packed ice or a snow block standing over water. The icebergs of frozen oceans
     * come out of the surface step as terrain, tall and in noise patches the far terrain does not model: it draws their
     * columns as flat ice or water, so the report counts them apart and leaves them out of the colour error.
     */
    static boolean iceberg(Real truth) {
        return truth.kind() == SurfaceKind.SNOW && truth.underwater()
                && (truth.top().is(Blocks.PACKED_ICE) || truth.top().is(Blocks.SNOW_BLOCK));
    }

    /**
     * The world generator's own steepness test for a column of a chunk (the surface rules' "steep" condition): the
     * ground a block south is 4 or more above the ground a block north, or the ground west 4 or more above the east.
     */
    static boolean steep(Real[] column, int lx, int lz) {
        int north = column[Math.max(lz - 1, 0) * 16 + lx].height();
        int south = column[Math.min(lz + 1, 15) * 16 + lx].height();
        if (south >= north + 4) {
            return true;
        }
        int west = column[lz * 16 + Math.max(lx - 1, 0)].height();
        int east = column[lz * 16 + Math.min(lx + 1, 15)].height();
        return west >= east + 4;
    }

    /**
     * Compares the columns of {@code chunks} with the real ones. {@code pale} runs the biome decoration on the real
     * chunks (Pale's craters) and also builds each chunk without it, to tell crater columns from plain ones. A non-null
     * {@code report} also gets every column's class (the real chunks then carry the snow and ice that decoration lays
     * over cold ground and water, and are measured the way the player sees them).
     */
    static Result compare(GameTestHelper helper, Body body, boolean pale, List<ChunkPos> chunks,
            @Nullable ClassReport report) {
        RealChunks real = new RealChunks(helper.getLevel(), body, pale, report != null);
        RealChunks plain = pale ? new RealChunks(helper.getLevel(), body, false) : null;
        SurfaceColumns columns = SurfaceColumns.create(body.sampler(), body.generator(), body.randomState(), pale, true);
        TerrainSampler.Worker worker = body.sampler().worker();
        Result r = new Result(new Stats("height"), new Stats("terrain height"), new Stats("height in craters"),
                new Stats("colour"), new Stats("colour in craters"), new Stats("getBaseHeight"),
                new Stats("colour where the sampler picks the block's own colour"), new TreeMap<>(), new TreeMap<>());
        int loggedMisses = 0;
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        Real[] column = new Real[256];
        for (ChunkPos pos : chunks) {
            ChunkAccess chunk = real.chunk(pos.x(), pos.z());
            ChunkAccess flat = plain == null ? null : plain.chunk(pos.x(), pos.z());
            for (int i = 0; i < 256; i++) {
                column[i] = real(chunk, pos.getMinBlockX() + (i & 15), pos.getMinBlockZ() + (i >> 4));
            }
            for (int lz = 0; lz < 16; lz++) {
                for (int lx = 0; lx < 16; lx++) {
                    int x = pos.getMinBlockX() + lx;
                    int z = pos.getMinBlockZ() + lz;
                    Real truth = column[lz * 16 + lx];
                    Real untouched = flat == null ? truth : real(flat, x, z);
                    boolean crater = untouched.height() != truth.height() || untouched.top() != truth.top();
                    columns.at(x, z);
                    double dh = columns.height() - truth.height();
                    (crater ? r.craterHeight() : r.height()).add(dh);
                    String topId = (truth.underwater() ? "water over " : "")
                            + BuiltInRegistries.BLOCK.getKey(truth.top().getBlock());
                    r.heightByBlock().computeIfAbsent(topId, Stats::new).add(dh);
                    if (pale) {
                        r.terrainHeight().add(Math.round(worker.ground(x, z)) - untouched.height());
                    } else if (lx % 4 == 0 && lz % 4 == 0) {
                        r.exactHeight().add(body.sampler().exact(x, z) - truth.height());
                    }
                    Holder<Biome> biome = report == null ? null : real.biomeAt(probe.set(x, truth.height(), z));
                    Integer blockRef = truth.underwater() && truth.kind() != SurfaceKind.SNOW
                            ? null : reference(truth.top());
                    Integer ref = blockRef;
                    if (ref == null && report != null && truth.top().is(Blocks.GRASS_BLOCK)) {
                        ref = SurfaceClassTests.grassColour(biome.value(), biome.getRegisteredName(), x, z);
                    }
                    if (report != null) {
                        if (iceberg(truth)) {
                            report.icebergs++;
                        } else {
                            report.add(biome.getRegisteredName().replace("minecraft:", ""),
                                    SurfaceClassTests.id(truth.top())
                                    + (truth.underwater() && truth.kind() == SurfaceKind.WATER ? " (under water)" : ""),
                                    steep(column, lx, lz), truth.kind(), columns.kind());
                            if (ref != null && truth.kind() != SurfaceKind.WATER) {
                                report.addColour(truth.kind(), colourError(columns.colour(), ref));
                            }
                        }
                    }
                    if (blockRef != null) {
                        double ce = colourError(columns.colour(), blockRef);
                        (crater ? r.craterColour() : r.colour()).add(ce);
                        if (ce < 0.15) {
                            r.agreeing().add(ce);
                        }
                        if (pale && ce > 0.03 && loggedMisses++ < 6) {
                            Apoapsis.LOG.info("[terrain-accuracy]   colour miss at {} {}: real {} (crater {}), sampler {}",
                                    x, z, BuiltInRegistries.BLOCK.getKey(truth.top().getBlock()), crater,
                                    Integer.toHexString(columns.colour()));
                        }
                        String id = BuiltInRegistries.BLOCK.getKey(truth.top().getBlock()).toString();
                        r.colourByBlock().computeIfAbsent(id, Stats::new).add(ce);
                    }
                }
            }
        }
        Apoapsis.LOG.info("[terrain-accuracy] {} over {} chunks", body.name(), chunks.size());
        for (Stats s : new Stats[] {r.height(), r.terrainHeight(), r.craterHeight(), r.exactHeight()}) {
            if (s.n() > 0) {
                Apoapsis.LOG.info("[terrain-accuracy]   {}; {}% within 1 block", s.line(),
                        Math.round(100 * s.fractionWithin(1)));
            }
        }
        for (Stats s : new Stats[] {r.colour(), r.craterColour(), r.agreeing()}) {
            if (s.n() > 0) {
                Apoapsis.LOG.info("[terrain-accuracy]   {}; {}% within 3 percent", s.line(),
                        Math.round(100 * s.fractionWithin(0.03)));
            }
        }
        for (Stats s : r.heightByBlock().values()) {
            Apoapsis.LOG.info("[terrain-accuracy]   height error by top block: {}; {}% within 1 block", s.line(),
                    Math.round(100 * s.fractionWithin(1)));
        }
        for (Stats s : r.colourByBlock().values()) {
            Apoapsis.LOG.info("[terrain-accuracy]   colour error by top block: {}; {}% within 3 percent", s.line(),
                    Math.round(100 * s.fractionWithin(0.03)));
        }
        return r;
    }

    /**
     * Chunks that hold the floor, rim and ejecta of craters near the origin: for each of a few craters (some in maria,
     * most in highlands), the chunks of its centre, of a point just outside its rim to the east, one beyond the rim to the
     * south and one in the ejecta to the west.
     */
    static List<ChunkPos> craterChunks(GameTestHelper helper, Body pale, int maria, int highlands) {
        RegistryAccess access = helper.getLevel().getServer().registryAccess();
        CraterFeature config = (CraterFeature) access.lookupOrThrow(Registries.FEATURE)
                .getOrThrow(PaleFeatures.CRATER).value();
        BiomeResolver biomes = pale.generator().getBiomeSource().createUncachedResolver(pale.randomState());
        CraterField field = new CraterField(pale.randomState().seed(), config,
                (x, z) -> biomes.getNoiseBiome(QuartPos.fromBlock(x), 0, QuartPos.fromBlock(z)).is(PaleFeatures.MARIA));
        Set<ChunkPos> out = new LinkedHashSet<>();
        int mariaLeft = maria;
        int highlandsLeft = highlands;
        for (int gx = -8; gx <= 8 && (mariaLeft > 0 || highlandsLeft > 0); gx++) {
            for (int gz = -8; gz <= 8; gz++) {
                CraterField.Crater c = field.cell(gx, gz);
                if (c == null || c.radius() < 10) {
                    continue;
                }
                boolean isMaria = biomes.getNoiseBiome(QuartPos.fromBlock(c.x()), 0, QuartPos.fromBlock(c.z()))
                        .is(PaleFeatures.MARIA);
                if (isMaria ? mariaLeft-- <= 0 : highlandsLeft-- <= 0) {
                    continue;
                }
                int r = c.radius();
                out.add(chunkAt(c.x(), c.z()));
                out.add(chunkAt(c.x() + r + 1, c.z()));
                out.add(chunkAt(c.x(), c.z() + r + 2));
                out.add(chunkAt(c.x() - Math.round(1.3f * r), c.z()));
            }
        }
        return new ArrayList<>(out);
    }

    private static ChunkPos chunkAt(int x, int z) {
        return new ChunkPos(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
    }

    /** Pale: heights and colours against real chunks, craters included. */
    static void pale(GameTestHelper helper, ShipCleanup ships) {
        Body pale = TerrainProbe.pale(helper.getLevel().getServer().registryAccess(), TerrainProbe.SEED);
        List<ChunkPos> chunks = randomChunks(pale, 48, 11L);
        chunks.addAll(craterChunks(helper, pale, 4, 8));
        Result r = compare(helper, pale, true, chunks, null);
        helper.assertTrue(r.craterHeight().n() >= 2000,
                "the sample holds too few crater columns to mean anything: " + r.craterHeight().n());
        // Heights: the terrain exactly, and at least 95 percent of columns within a block with the craters in.
        helper.assertTrue(r.terrainHeight().fractionWithin(0) >= 0.99,
                "Pale terrain heights off the real ground: " + r.terrainHeight().line());
        helper.assertTrue(r.height().fractionWithin(1) >= 0.95, "Pale heights: " + r.height().line());
        helper.assertTrue(r.craterHeight().fractionWithin(1) >= 0.95, "Pale heights in craters: " + r.craterHeight().line());
        // Colours: within 3 percent of the block texture's colour, at 95 percent of columns, in craters as outside.
        helper.assertTrue(r.colour().fractionWithin(0.03) >= 0.95, "Pale colours: " + r.colour().line());
        helper.assertTrue(r.craterColour().fractionWithin(0.03) >= 0.95,
                "Pale colours in craters: " + r.craterColour().line());
        helper.succeed();
    }

    /** Verdance, the control: terrain heights and colours of rock-like blocks against real chunks. */
    static void verdance(GameTestHelper helper, ShipCleanup ships) {
        Body verdance = TerrainProbe.verdance(helper.getLevel().getServer().registryAccess(), TerrainProbe.SEED);
        ClassReport random = new ClassReport("Verdance, 24 random chunks");
        Result r = compare(helper, verdance, false, randomChunks(verdance, 24, 12L), random);
        random.log();
        List<SurfaceClassTests.Pick> picks = SurfaceClassTests.spread(verdance, 2, 5L);
        ClassReport spread = new ClassReport("Verdance, chunks picked in each biome");
        compare(helper, verdance, false, picks.stream().map(SurfaceClassTests.Pick::chunk).distinct().toList(), spread);
        spread.log();
        // The fast sampler stands in for the full density, which carvers and aquifers then shape: heights come out
        // within a block for most columns and within 4 for nearly all.
        helper.assertTrue(r.height().rms() <= 2.5 && r.height().within(0.95) <= 4,
                "Verdance heights: " + r.height().line());
        helper.assertTrue(r.height().fractionWithin(1) >= 0.85, "Verdance heights: " + r.height().line());
        // Colours: where the sampler picks a block's own colour (stone, sand), it is within 3 percent of the texture.
        helper.assertTrue(r.agreeing().n() >= 150,
                "too few Verdance columns where the colours agree on the block: " + r.agreeing().n());
        helper.assertTrue(r.agreeing().fractionWithin(0.03) >= 0.95, "Verdance rock colours: " + r.agreeing().line());
        // Classes, against the block the generator really put on top (with snow and ice as the player sees them). The
        // sample must hold enough bare rock, sand, gravel and badlands to mean anything.
        helper.assertTrue(spread.columns >= 12_000 && spread.rowTotal(SurfaceKind.STONE) >= 500
                && spread.rowTotal(SurfaceKind.SAND) >= 500 && spread.rowTotal(SurfaceKind.GRAVEL) >= 100
                && spread.rowTotal(SurfaceKind.RED_ROCK) >= 500 && spread.rowTotal(SurfaceKind.SNOW) >= 2_000,
                "the biome-picked sample holds too few columns of some class: " + spread.columns);
        helper.assertTrue(spread.accuracy() >= 0.93, "Verdance class match, picked chunks: " + spread.accuracy());
        helper.assertTrue(random.accuracy() >= 0.88, "Verdance class match, random chunks: " + random.accuracy());
        helper.assertTrue(spread.recall(SurfaceKind.STONE, SurfaceKind.SAND, SurfaceKind.GRAVEL) >= 0.90,
                "stone, sand and gravel columns drawn as their class, picked chunks: "
                + spread.recall(SurfaceKind.STONE, SurfaceKind.SAND, SurfaceKind.GRAVEL));
        helper.assertTrue(random.recall(SurfaceKind.STONE, SurfaceKind.SAND, SurfaceKind.GRAVEL) >= 0.90,
                "stone, sand and gravel columns drawn as their class, random chunks: "
                + random.recall(SurfaceKind.STONE, SurfaceKind.SAND, SurfaceKind.GRAVEL));
        for (SurfaceKind kind : SurfaceClassTests.KINDS) {
            double floor = switch (kind) {
                case GRAVEL -> 0.85;
                case WATER -> 0.88;
                case STONE, SNOW -> 0.90;
                default -> 0.95;
            };
            helper.assertTrue(spread.recall(kind) >= floor,
                    "Verdance " + kind + " columns drawn as " + kind + ": " + spread.recall(kind));
        }
        // Colours of the land columns against the texture of the block on top: within 5 percent at 90 percent of them.
        helper.assertTrue(spread.colour.fractionWithin(0.05) >= 0.90, "Verdance land colours, picked chunks: "
                + spread.colour.line());
        helper.assertTrue(random.colour.fractionWithin(0.05) >= 0.95, "Verdance land colours, random chunks: "
                + random.colour.line());
        helper.succeed();
    }
}
