package dev.apoapsis.client.map3d;

import com.mojang.logging.LogUtils;
import dev.apoapsis.client.ClientState;
import dev.apoapsis.client.map.Labels;
import dev.apoapsis.client.map.Palette;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.FlightRegime;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.Vec3d;
import dev.apoapsis.ship.ShipEntity;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Keeps {@link MapSites} up to date and draws its markers on the 3D map: a home marker at Verdance's world spawn, and
 * on each body a landing marker where the player's ship last settled on the ground. In a single-player world the sites
 * are saved in the world's data folder, so they survive a restart; elsewhere they last for the session. Client only.
 */
public final class SiteMarkers {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String FILE = "data/apoapsis/map_sites.txt";
    /** Below this on-screen radius a body is too small to show where on it a site is, px. */
    private static final double MIN_DISC_PX = 14;
    private static final int LABEL_H = 9;
    private static final int HOME_COLOUR = Palette.INK;
    private static final int LANDING_COLOUR = Palette.GO;

    /** The save file the sites in memory were read from; null for none (not a single-player world). */
    private static @Nullable Path loadedFrom;
    private static boolean loaded;

    private SiteMarkers() {}

    /** Forgets the sites in memory (logout or a new world). */
    public static void forget() {
        MapSites.CURRENT.clear();
        loadedFrom = null;
        loaded = false;
    }

    /** One client tick: loads the saved sites once, takes the spawn as home, and records a landing of the player's ship. */
    public static void observe(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            return;
        }
        Path file = saveFile(mc);
        if (!loaded || !java.util.Objects.equals(file, loadedFrom)) {
            MapSites.CURRENT.clear();
            loadedFrom = file;
            loaded = true;
            if (file != null && Files.isRegularFile(file)) {
                try {
                    MapSites.CURRENT.load(Files.readAllLines(file, StandardCharsets.UTF_8));
                } catch (IOException e) {
                    LOG.warn("Could not read the map sites", e);
                }
            }
        }
        boolean changed = false;
        var respawn = mc.level.getRespawnData().globalPos();
        if (Level.OVERWORLD.equals(respawn.dimension())) {
            MapSites.Site spawn = MapSites.fromSurface(respawn.pos().getX() + 0.5, respawn.pos().getZ() + 0.5,
                    MeridianSystem.VERDANCE.radius);
            MapSites.Site old = MapSites.CURRENT.home();
            if (old == null || MapSites.separation(old, spawn, MeridianSystem.VERDANCE.radius) > 1) {
                MapSites.CURRENT.setHome(spawn);
                changed = true;
            }
        }
        OrbitStatePayload p = ClientState.orbit;
        if (p != null && FlightRegime.byOrdinal(p.regime()) == FlightRegime.SURFACE
                && mc.player.getVehicle() instanceof ShipEntity ship && ship.isLanded()) {
            changed |= MapSites.CURRENT.recordLanding(BodyId.byOrdinal(p.body()),
                    MapSites.fromInertial(new Vec3d(p.rx(), p.ry(), p.rz())));
        }
        if (changed && file != null) {
            try {
                Files.createDirectories(file.getParent());
                Files.write(file, MapSites.CURRENT.toLines(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOG.warn("Could not save the map sites", e);
            }
        }
    }

    private static @Nullable Path saveFile(Minecraft mc) {
        MinecraftServer server = mc.getSingleplayerServer();
        return server == null ? null : server.getWorldPath(LevelResource.ROOT).resolve(FILE);
    }

    /** Draws the markers of the bodies big enough on screen, each with its label, clear of labels already placed. */
    static void draw(GuiGraphicsExtractor g, Font font, Labels labels, int viewW, int viewH, double nowUt,
            OrbitCamera camera, Vec3d focus, ManeuverEditor.Projector projector) {
        double focal = OrbitCamera.focal(viewH);
        Vec3d eye = focus.add(camera.eye());
        MapSites.Site home = MapSites.CURRENT.home();
        if (home != null) {
            marker(g, font, labels, viewW, viewH, nowUt, eye, projector, focal, MeridianSystem.VERDANCE, home, true);
        }
        for (BodyId b : BodyId.values()) {
            MapSites.Site s = MapSites.CURRENT.landing(b);
            if (s != null) {
                marker(g, font, labels, viewW, viewH, nowUt, eye, projector, focal, MeridianSystem.byId(b.id), s, false);
            }
        }
    }

    private static void marker(GuiGraphicsExtractor g, Font font, Labels labels, int viewW, int viewH, double nowUt,
            Vec3d eye, ManeuverEditor.Projector projector, double focal, CelestialBody body, MapSites.Site site,
            boolean home) {
        Vec3d centre = OrbitPlot.rootPosition(body, nowUt);
        Vec3d up = MapSites.unit(site);
        // On the near side of the body: the point is seen when the eye is beyond its tangent plane.
        if (up.dot(eye.sub(centre)) <= body.radius * 1.001) {
            return;
        }
        double[] s = projector.screenOf(centre.add(up.scale(body.radius)));
        if (s == null || focal * body.radius / s[2] < MIN_DISC_PX) {
            return;
        }
        int x = (int) Math.round(s[0]);
        int y = (int) Math.round(s[1]);
        int colour = home ? HOME_COLOUR : LANDING_COLOUR;
        shape(g, x, y, colour, home);
        Component text = Component.translatable(home ? "apoapsis.map3d.site.home" : "apoapsis.map3d.site.landing");
        int w = font.width(text);
        // Beside the mark, else the other side, else above or below it: the first spot clear of other labels.
        int[][] spots = {{x + 8, y - 4}, {x - 8 - w, y - 4}, {x - w / 2, y - 16}, {x - w / 2, y + 8}};
        for (int[] spot : spots) {
            if (plate(g, font, labels, viewW, viewH, text, spot[0], spot[1], colour)) {
                return;
            }
        }
    }

    /** A label on a dark plate, so it reads over a bright map; false when off screen or on another label. */
    private static boolean plate(GuiGraphicsExtractor g, Font font, Labels labels, int viewW, int viewH,
            Component text, int x, int y, int argb) {
        int w = font.width(text);
        if (x + w < 0 || y + LABEL_H < 0 || x > viewW || y > viewH || !labels.claim(x - 3, y - 2, x + w + 3,
                y + LABEL_H + 2)) {
            return false;
        }
        g.fill(x - 2, y - 1, x + w + 2, y + LABEL_H + 1, Palette.alpha(Palette.STEEL_950, 0xB0));
        g.text(font, text, x, y, argb, false);
        return true;
    }

    /** A solid diamond (home) or up-pointing triangle (landing) in {@code argb}, with a dark rim, centred on (x, y). */
    private static void shape(GuiGraphicsExtractor g, int x, int y, int argb, boolean diamond) {
        for (int pass = 0; pass < 2; pass++) {
            int r = pass == 0 ? 5 : 4;
            int colour = pass == 0 ? Palette.STEEL_950 : argb;
            for (int dy = -r; dy <= r; dy++) {
                int half = diamond ? r - Math.abs(dy) : (dy + r) / 2;
                g.fill(x - half, y + dy, x + half + 1, y + dy + 1, colour);
            }
        }
    }
}
