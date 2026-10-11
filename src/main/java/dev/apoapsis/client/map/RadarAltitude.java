package dev.apoapsis.client.map;

import dev.apoapsis.ship.ShipEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Radar altitude: the height of the ship's lowest point above the terrain directly below its centre, not above sea
 * level. Client only.
 */
final class RadarAltitude {
    private RadarAltitude() {}

    /**
     * Metres from the ship's lowest point down to the top of the terrain under its centre, never below 0. NaN when the
     * chunk under the centre is not loaded on the client.
     */
    static double of(ClientLevel level, ShipEntity ship) {
        int bx = Mth.floor(ship.getX());
        int bz = Mth.floor(ship.getZ());
        if (!level.hasChunkAt(bx, bz)) {
            return Double.NaN;
        }
        double lowestY = ship.getY() - ship.lowestPointBelowCentreM();
        double groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
        return Math.max(0, lowestY - groundY);
    }
}
