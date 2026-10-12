package dev.apoapsis.planet.map;

import dev.apoapsis.flight.BodyId;
import dev.apoapsis.net.PlanetMapTilePayload;
import java.util.List;

/**
 * The refinement ladder of each body's planet map. Level 0 is the coarse whole map that every client gets at login
 * (see {@code PlanetMaps}); levels 1 and up are generated in the background after the world loads, cached in the save,
 * and streamed to pilots as {@link PlanetMapTilePayload} tiles. Every level is equirectangular, a whole number of
 * tiles wide and high, and larger than the one before.
 */
public final class MapLevels {
    /** Tile edge, texels. */
    public static final int TILE = PlanetMapTilePayload.TILE;

    private MapLevels() {}

    /**
     * One level of one body's map: its size in texels and the samples per texel along each axis (the supersample).
     * Level 0 is the coarse map, which has no tiles of its own.
     */
    public record Level(int index, int width, int height, int supersample) {
        /** Tiles along the longitude. */
        public int tilesX() {
            return width / TILE;
        }

        /** Tiles along the latitude. */
        public int tilesY() {
            return height / TILE;
        }

        /** Number of tiles; tile (tx, ty) is number {@code ty * tilesX() + tx}. */
        public int tileCount() {
            return tilesX() * tilesY();
        }
    }

    private static final List<Level> VERDANCE = List.of(
            new Level(0, 512, 256, 3),
            new Level(1, 2048, 1024, 2),
            new Level(2, 4096, 2048, 1));
    private static final List<Level> PALE = List.of(
            new Level(0, 512, 256, 3),
            new Level(1, 1024, 512, 2),
            new Level(2, 2048, 1024, 2));

    /** Every level of {@code body}, coarsest first; the list index is the level. */
    public static List<Level> of(BodyId body) {
        return body == BodyId.PALE ? PALE : VERDANCE;
    }

    /** The finest level index of {@code body}. */
    public static int maxLevel(BodyId body) {
        return of(body).size() - 1;
    }
}
