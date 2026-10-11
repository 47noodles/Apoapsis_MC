package dev.apoapsis.client.map3d;

import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Elements;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.Vec3d;
import dev.apoapsis.orbit.maneuver.ManeuverNode;
import dev.apoapsis.orbit.maneuver.Maneuvers;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The single planned burn of the 3D map and the maths around it: the six handles, the state and apsides after the
 * burn, the burn timing, how many m/s a dragged pixel is worth, and the time a click on the orbit line stands for. The
 * burn geometry and timing come from {@link Maneuvers}. Pure maths, no game state. Client only.
 */
public final class ManeuverPlan {
    /** The three local burn axes. */
    public enum Axis { PROGRADE, NORMAL, RADIAL }

    /** One handle: an axis and a sign, +1 for prograde, normal and radial-out, -1 for the opposite. */
    public record Handle(Axis axis, int sign) {}

    /** The six handles, in a fixed order: prograde, retrograde, normal, anti-normal, radial out, radial in. */
    public static final List<Handle> HANDLES = List.of(
            new Handle(Axis.PROGRADE, 1), new Handle(Axis.PROGRADE, -1),
            new Handle(Axis.NORMAL, 1), new Handle(Axis.NORMAL, -1),
            new Handle(Axis.RADIAL, 1), new Handle(Axis.RADIAL, -1));

    /** The apsides of an orbit, in metres above the body's radius. {@code apoapsisAltitude} is NaN when not bound. */
    public record Apsides(boolean bound, double apoapsisAltitude, double periapsisAltitude) {}

    /** A burn's duration, its ignition time, and the time from now to the ignition. */
    public record Timing(double burnSeconds, double startUt, double secondsToStart) {}

    /** A node must be at least this far in the future, s. */
    public static final double MIN_LEAD_S = 5.0;

    private @Nullable ManeuverNode node;

    /** The planned burn, or null when none is placed. */
    public @Nullable ManeuverNode node() {
        return node;
    }

    /** True when a burn is placed. */
    public boolean has() {
        return node != null;
    }

    /** Places a burn at {@code ut} with no delta-v; the caller keeps {@code ut} in the future. */
    public void place(double ut) {
        node = new ManeuverNode(ut, 0, 0, 0);
    }

    /** Moves the burn to {@code ut}, keeping its delta-v. Does nothing without a node. */
    public void moveTo(double ut) {
        ManeuverNode n = node;
        if (n != null) {
            node = new ManeuverNode(ut, n.prograde(), n.normal(), n.radial());
        }
    }

    /** Deletes the burn. */
    public void clear() {
        node = null;
    }

    /** Adds {@code dv} m/s along handle {@code h} (so a negative sign handle subtracts). Does nothing without a node. */
    public void addDv(Handle h, double dv) {
        ManeuverNode n = node;
        if (n == null) {
            return;
        }
        double d = h.sign() * dv;
        node = switch (h.axis()) {
            case PROGRADE -> new ManeuverNode(n.ut(), n.prograde() + d, n.normal(), n.radial());
            case NORMAL -> new ManeuverNode(n.ut(), n.prograde(), n.normal() + d, n.radial());
            case RADIAL -> new ManeuverNode(n.ut(), n.prograde(), n.normal(), n.radial() + d);
        };
    }

    /** The unit world vector the burn of handle {@code h} pushes toward, at state {@code atNode}. */
    public static Vec3d direction(StateVector atNode, Handle h) {
        ManeuverNode unit = switch (h.axis()) {
            case PROGRADE -> new ManeuverNode(0, 1, 0, 0);
            case NORMAL -> new ManeuverNode(0, 0, 1, 0);
            case RADIAL -> new ManeuverNode(0, 0, 0, 1);
        };
        return Maneuvers.worldDeltaV(atNode, unit).normalize().scale(h.sign());
    }

    /** The state just after the burn {@code n} at {@code atNode}. */
    public static StateVector afterBurn(StateVector atNode, ManeuverNode n) {
        return Maneuvers.applyAt(atNode, n);
    }

    /** The apsides of the orbit of state {@code s} around {@code body}. */
    public static Apsides apsides(CelestialBody body, StateVector s) {
        Elements e = Elements.of(body.mu, s);
        boolean bound = e.eccentricity() < 1 && Double.isFinite(e.apoapsisRadius());
        return new Apsides(bound, bound ? e.apoapsisRadius() - body.radius : Double.NaN,
                e.periapsisRadius() - body.radius);
    }

    /**
     * The burn's duration and ignition for a ship of {@code massKg} with engines of {@code ispS} and {@code thrustN},
     * seen at {@code nowUt}; null when the ship cannot burn (no thrust).
     */
    public static @Nullable Timing timing(ManeuverNode n, double nowUt, double massKg, double ispS, double thrustN) {
        double burn = Maneuvers.burnSeconds(n.deltaV(), massKg, ispS, thrustN);
        if (!Double.isFinite(burn)) {
            return null;
        }
        double start = Maneuvers.burnStartUt(n, massKg, ispS, thrustN);
        if (!Double.isFinite(start)) {
            return null;
        }
        return new Timing(burn, start, start - nowUt);
    }

    /** Delta-v per dragged pixel at orbital speed {@code speedMps}: coarse at speed, a tenth of it when {@code fine}. */
    public static double dvPerPixel(double speedMps, boolean fine) {
        double dv = Math.max(0.5, 0.003 * speedMps);
        return fine ? dv * 0.1 : dv;
    }

    /**
     * The earliest time at or after {@code nowUt + MIN_LEAD_S} that the point at {@code utOnLine} on a closed orbit of
     * period {@code periodS} is reached (it is reached again every period; an open orbit has {@code periodS} NaN and
     * only once). NaN when that time is later than {@code lastUt} or not finite.
     */
    public static double nextUt(double utOnLine, double nowUt, double periodS, double lastUt) {
        double earliest = nowUt + MIN_LEAD_S;
        double ut = utOnLine;
        if (ut < earliest) {
            if (!(periodS > 0) || !Double.isFinite(periodS)) {
                return Double.NaN;
            }
            ut += Math.ceil((earliest - ut) / periodS) * periodS;
        }
        return Double.isFinite(ut) && ut <= lastUt ? ut : Double.NaN;
    }
}
