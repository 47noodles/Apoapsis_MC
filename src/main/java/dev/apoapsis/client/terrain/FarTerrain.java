package dev.apoapsis.client.terrain;

import dev.apoapsis.flight.BodyId;
import dev.apoapsis.net.TerrainTilePayload;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The latest far-terrain tile of each level of detail for each body, as the server sent them. A new tile replaces the
 * old one of its level; meshes compare tiles by identity to know when to rebuild. Client only; render thread.
 */
public final class FarTerrain {
    private static final Map<String, TerrainTilePayload[]> TILES = new HashMap<>();

    private FarTerrain() {}

    /** Takes a tile sent by the server (the payload handler). A malformed tile or unknown body is ignored. */
    public static void accept(TerrainTilePayload tile) {
        if (!tile.valid() || bodyOf(tile.body()) == null) {
            return;
        }
        TILES.computeIfAbsent(tile.body(), k -> new TerrainTilePayload[TerrainTilePayload.LEVELS])[tile.level()] = tile;
        FarTerrainClient.onTileArrived(tile);
    }

    /** The latest tile of {@code level} for {@code body} ("verdance", "pale"), or null while none has arrived. */
    public static @Nullable TerrainTilePayload tile(String body, int level) {
        TerrainTilePayload[] tiles = TILES.get(body);
        return tiles == null ? null : tiles[level];
    }

    /** Forgets every tile (logout). */
    public static void clear() {
        TILES.clear();
    }

    static @Nullable BodyId bodyOf(String id) {
        for (BodyId b : BodyId.values()) {
            if (b.id.equals(id)) {
                return b;
            }
        }
        return null;
    }
}
