package dev.apoapsis.client.map3d;

import dev.apoapsis.flight.BodyId;
import dev.apoapsis.orbit.Vec3d;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The site markers of the 3D map: the home point and, for each body, the last place the player's ship landed, as
 * latitude and longitude in radians in the body's own frame (the inertial frame, which does not rotate). Also the
 * text form the client saves them in. Pure data, no game state. Client only.
 */
public final class MapSites {
    /** One shared instance for the client. */
    public static final MapSites CURRENT = new MapSites();

    /** Two landings closer than this are the same site, m. */
    private static final double SAME_SITE_M = 100;
    private static final double VERDANCE_RADIUS_M = 64000;
    private static final double PALE_RADIUS_M = 20000;

    /** A point on a body: latitude and longitude, radians. */
    public record Site(double lat, double lon) {}

    private @Nullable Site home;
    private final Map<BodyId, Site> landings = new EnumMap<>(BodyId.class);

    /** The site under an inertial position {@code r} relative to the body's centre. */
    public static Site fromInertial(Vec3d r) {
        double len = r.length();
        return new Site(Math.asin(Math.max(-1, Math.min(1, r.z() / len))), Math.atan2(r.y(), r.x()));
    }

    /** The site under a surface dimension's (x, z) on a body of {@code radius}: lon = x / R, lat = -z / R. */
    public static Site fromSurface(double x, double z, double radius) {
        return new Site(-z / radius, x / radius);
    }

    /** The unit vector from the body's centre to the site. */
    public static Vec3d unit(Site s) {
        double c = Math.cos(s.lat());
        return new Vec3d(c * Math.cos(s.lon()), c * Math.sin(s.lon()), Math.sin(s.lat()));
    }

    /** The great-circle distance between two sites on a body of {@code radius}, m. */
    public static double separation(Site a, Site b, double radius) {
        double dot = unit(a).dot(unit(b));
        return radius * Math.acos(Math.max(-1, Math.min(1, dot)));
    }

    private static double radiusOf(BodyId body) {
        return body == BodyId.PALE ? PALE_RADIUS_M : VERDANCE_RADIUS_M;
    }

    /** Sets the home point (Verdance). */
    public void setHome(Site s) {
        home = s;
    }

    /** The home point, or null when none is known. */
    public @Nullable Site home() {
        return home;
    }

    /**
     * Stores {@code s} as the last landing site on {@code body}. True when it replaced a different site (or there was
     * none), so the caller knows to save; a landing within 100 m of the stored one changes nothing and returns false.
     */
    public boolean recordLanding(BodyId body, Site s) {
        Site old = landings.get(body);
        if (old != null && separation(old, s, radiusOf(body)) <= SAME_SITE_M) {
            return false;
        }
        landings.put(body, s);
        return true;
    }

    /** The last landing site on {@code body}, or null. */
    public @Nullable Site landing(BodyId body) {
        return landings.get(body);
    }

    /** Forgets every site. */
    public void clear() {
        home = null;
        landings.clear();
    }

    /** Replaces every site with those read from {@code lines} (see {@link #fromLines}). */
    public void load(List<String> lines) {
        MapSites in = fromLines(lines);
        home = in.home;
        landings.clear();
        landings.putAll(in.landings);
    }

    /** One line per site: {@code home <lat> <lon>} and {@code landing <body> <lat> <lon>}. */
    public List<String> toLines() {
        List<String> out = new ArrayList<>();
        if (home != null) {
            out.add("home " + home.lat() + " " + home.lon());
        }
        for (Map.Entry<BodyId, Site> e : landings.entrySet()) {
            out.add("landing " + e.getKey().id + " " + e.getValue().lat() + " " + e.getValue().lon());
        }
        return out;
    }

    /** Reads {@link #toLines} back; blank, comment and malformed lines and unknown bodies are ignored. */
    public static MapSites fromLines(List<String> lines) {
        MapSites out = new MapSites();
        for (String line : lines) {
            String[] w = line.trim().split("\\s+");
            try {
                if (w.length == 3 && w[0].equals("home")) {
                    out.home = new Site(Double.parseDouble(w[1]), Double.parseDouble(w[2]));
                } else if (w.length == 4 && w[0].equals("landing")) {
                    for (BodyId b : BodyId.values()) {
                        if (b.id.equals(w[1])) {
                            out.landings.put(b, new Site(Double.parseDouble(w[2]), Double.parseDouble(w[3])));
                        }
                    }
                }
            } catch (NumberFormatException e) {
                // A malformed line is skipped.
            }
        }
        return out;
    }
}
