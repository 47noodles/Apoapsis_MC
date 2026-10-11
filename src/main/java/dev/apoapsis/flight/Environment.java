package dev.apoapsis.flight;

import net.minecraft.world.phys.Vec3;

/**
 * What the world does to a ship this tick, in the ship's current dimension frame.
 *
 * @param gravity          acceleration in m/s^2 (surface: (0, -g(h), 0); orbit dimension: zero, gravity is in the orbit)
 * @param airDensity       kg/m^3 at the ship (0 in vacuum)
 * @param pressureFraction p / p0 at the ship, 0..1 (used for Isp interpolation)
 * @param terrainCollision whether the ship collides with blocks (false in the orbit dimension)
 * @param orbital          true when translation is owned by the orbit model (ShipEntity integrates rotation only)
 */
public record Environment(Vec3 gravity, double airDensity, double pressureFraction, boolean terrainCollision,
                          boolean orbital) {
}
