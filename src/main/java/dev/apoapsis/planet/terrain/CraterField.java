package dev.apoapsis.planet.terrain;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

/**
 * Where Pale's craters are and what each does to a column, as pure functions of the world seed, so the world generator
 * ({@link CraterFeature}) and the far-terrain sampler ({@code PaleSurface}) draw the same craters from one source.
 *
 * <p>Craters come from a fixed grid of {@link #CELL} blocks. Each cell draws, from the seed and its own coordinates
 * alone, whether it holds a crater, where its centre is and how large it is. The chance that a cell holds one depends
 * on the biome at the crater's centre (maria are sparser than highlands), so a crater is the same whichever chunk, or
 * which sampler, looks at it.
 */
public final class CraterField {
    /** Grid cell size, blocks. Larger than twice the ejecta reach, so a block has at most two cells either way within reach. */
    public static final int CELL = 128;
    /** Farthest ejecta reaches from a crater centre, blocks: 1.5 times the largest radius. */
    public static final int REACH = 60;
    public static final int MIN_RADIUS = 4;
    public static final int MAX_RADIUS = 40;

    /** What the biome under a block is, as far as craters care: true for maria. */
    @FunctionalInterface
    public interface MariaAt {
        boolean at(int x, int z);
    }

    /** One crater: centre in block coordinates and radius in blocks. */
    public record Crater(int x, int z, int radius) {}

    /** What a crater does to a column: the bowl's basalt floor, the raised rim, or the regolith ejecta over the top. */
    public enum Kind { FLOOR, RIM, EJECTA }

    /**
     * The change a crater makes to a column. {@link Kind#FLOOR}: {@code blocks} is the depth dug out, and the new top is
     * basalt. {@link Kind#RIM}: {@code blocks} is the regolith raised on top. {@link Kind#EJECTA}: the top block becomes
     * regolith, {@code blocks} is 0.
     */
    public record Effect(Kind kind, int blocks) {
        private static final Effect EJECTA = new Effect(Kind.EJECTA, 0);
    }

    private final long seed;
    private final float highlandsChance;
    private final float mariaChance;
    private final MariaAt maria;

    public CraterField(long seed, CraterFeature config, MariaAt maria) {
        this.seed = seed;
        this.highlandsChance = config.chance();
        this.mariaChance = config.mariaChance();
        this.maria = maria;
    }

    /** The crater of grid cell ({@code gx}, {@code gz}), or null if the cell has none. */
    public @Nullable Crater cell(int gx, int gz) {
        RandomSource r = RandomSource.create(seed ^ (gx * 0x9E3779B97F4A7C15L) ^ (gz * 0xC2B2AE3D27D4EB4FL));
        float draw = r.nextFloat();
        int cx = gx * CELL + r.nextInt(CELL);
        int cz = gz * CELL + r.nextInt(CELL);
        float u = r.nextFloat();
        // Cubing the uniform draw makes small craters common and large ones rare.
        int radius = MIN_RADIUS + Math.round((MAX_RADIUS - MIN_RADIUS) * u * u * u);
        return draw < (maria.at(cx, cz) ? mariaChance : highlandsChance) ? new Crater(cx, cz, radius) : null;
    }

    /**
     * The craters of the grid cells that overlap the block rectangle, in the order {@link #nearest} breaks ties by. To
     * get every crater that reaches a set of columns, pass their bounds widened by {@link #REACH}.
     */
    public List<Crater> reaching(int minX, int minZ, int maxX, int maxZ) {
        List<Crater> out = new ArrayList<>();
        for (int gx = Math.floorDiv(minX, CELL); gx <= Math.floorDiv(maxX, CELL); gx++) {
            for (int gz = Math.floorDiv(minZ, CELL); gz <= Math.floorDiv(maxZ, CELL); gz++) {
                Crater c = cell(gx, gz);
                if (c != null) {
                    out.add(c);
                }
            }
        }
        return out;
    }

    /** The crater whose centre is nearest in units of its own radius, so overlapping craters meet at their rims. */
    public static @Nullable Crater nearest(List<Crater> craters, int x, int z) {
        Crater best = null;
        double bestRatio = Double.MAX_VALUE;
        for (Crater c : craters) {
            double ratio = Math.hypot(x + 0.5 - c.x(), z + 0.5 - c.z()) / c.radius();
            if (ratio < bestRatio) {
                bestRatio = ratio;
                best = c;
            }
        }
        return best;
    }

    /** What {@code crater} does to the column ({@code x}, {@code z}), or null if it leaves it alone. */
    public static @Nullable Effect effect(@Nullable Crater crater, int x, int z) {
        if (crater == null) {
            return null;
        }
        double d = Math.hypot(x + 0.5 - crater.x(), z + 0.5 - crater.z());
        double r = crater.radius();
        if (d >= 1.5 * r) {
            return null;
        }
        double rimWidth = Math.max(2.0, 0.15 * r);
        if (d < r) {
            int depth = (int) Math.round(0.2 * r * (1.0 - (d / r) * (d / r)));
            return depth > 0 ? new Effect(Kind.FLOOR, depth) : null;
        } else if (d < r + rimWidth) {
            int rise = (int) Math.round(Math.max(1.0, 0.1 * r) * (1.0 - (d - r) / rimWidth));
            return rise > 0 ? new Effect(Kind.RIM, rise) : null;
        }
        return Effect.EJECTA;
    }
}
