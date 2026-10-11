package dev.apoapsis.flight;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.MeridianSystem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Dimension keys and the per-body constants of the KSP layer (contract "Bodies" and "Frames").
 * Data for the two dimensions lives in {@code data/apoapsis/dimension} and {@code dimension_type}.
 */
public final class FlightDimensions {
    /** Void dimension where ships in orbit live, one per slot along +X. */
    public static final ResourceKey<Level> ORBIT = key("orbit");
    /** Pale's flat regolith surface, sea level y = 64, no atmosphere. */
    public static final ResourceKey<Level> PALE = key("pale");

    /** Distance between orbit slots along X, metres. */
    public static final double SLOT_SPACING_M = 1024.0;
    /** Height of pinned ships in the orbit dimension, metres. */
    public static final double ORBIT_PIN_Y = 128.0;

    private FlightDimensions() {}

    private static ResourceKey<Level> key(String path) {
        return ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(Apoapsis.MODID, path));
    }

    /** The body a surface dimension belongs to. Any dimension that is not Pale counts as Verdance. */
    public static BodyId surfaceBody(ResourceKey<Level> dimension) {
        return PALE.equals(dimension) ? BodyId.PALE : BodyId.VERDANCE;
    }

    /** The surface dimension of a body. */
    public static ResourceKey<Level> surfaceDimension(BodyId body) {
        return body == BodyId.PALE ? PALE : Level.OVERWORLD;
    }

    /** Surface sea level (y of h = 0) for a surface dimension. */
    public static double seaLevel(ResourceKey<Level> dimension) {
        return PALE.equals(dimension) ? 64.0 : 63.0;
    }

    /** Surface sea level for a body. */
    public static double seaLevel(BodyId body) {
        return body == BodyId.PALE ? 64.0 : 63.0;
    }

    /** The orbit library's body for an id. */
    public static CelestialBody system(BodyId body) {
        return body == BodyId.PALE ? MeridianSystem.PALE : MeridianSystem.VERDANCE;
    }

    /** Altitude above which a surface ship goes up to orbit, and below which an orbiting ship comes down, metres. */
    public static double transitionAltitude(BodyId body) {
        return body == BodyId.PALE ? 1500.0 : 2000.0;
    }

    /** Pinned position of an orbit slot (centre of mass). */
    public static Vec3 slotPosition(int slot) {
        return new Vec3(SLOT_SPACING_M * slot, ORBIT_PIN_Y, 0.0);
    }
}
