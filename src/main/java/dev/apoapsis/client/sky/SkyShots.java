package dev.apoapsis.client.sky;

import com.mojang.logging.LogUtils;
import dev.apoapsis.client.ClientState;
import dev.apoapsis.client.OrbitNow;
import dev.apoapsis.client.sky.SkyMath.Axes;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.Vec3d;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Scripted sky screenshots for checking the space sky, off unless the environment variable
 * {@code APOAPSIS_SKY_SHOTS} is {@code orbit} or {@code pale}. In that dimension, after a short wait, the camera is
 * pointed at a list of directions (deep sky, the sun, near the sun, the planet, its limb) and a screenshot named
 * {@code <APOAPSIS_SKY_PREFIX>-<view>.png} is saved for each; with {@code APOAPSIS_SKY_QUIT} set the game then stops.
 * The look overrides the camera angles after the cockpit camera has set them. Client only; a test aid, no gameplay.
 */
public final class SkyShots {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String MODE = System.getenv("APOAPSIS_SKY_SHOTS");
    private static final String PREFIX = System.getenv("APOAPSIS_SKY_PREFIX") == null ? "imm-sky"
            : System.getenv("APOAPSIS_SKY_PREFIX");
    /** A look name to hold for {@link #HOLD_TICKS} ticks with vsync off, for frame-time runs. */
    private static final String HOLD = System.getenv("APOAPSIS_SKY_HOLD");
    private static final int HOLD_TICKS = 400;
    private static final int WAIT_TICKS = 120;
    private static final Vec3d SUN = new Vec3d(1, 0, 0);

    private static int ticks;
    private static int step;
    private static int phase;
    private static int settle = 30;
    private static String tag = "";
    private static int stepTick;
    private static boolean forced;
    private static float yaw;
    private static float pitch;
    private static boolean done;

    private SkyShots() {}

    /** Hooks the scripted look, only when the script is switched on. */
    public static void register() {
        if (MODE == null) {
            return;
        }
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ClientTickEvent.Post.class, e -> tick());
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ViewportEvent.ComputeCameraAngles.class, e -> {
            if (forced) {
                e.setYaw(yaw);
                e.setPitch(pitch);
                e.setRoll(0);
            }
        });
    }

    /** True while piloting over the sunlit side (the sun is +X inertial), so the planet below is shown lit. */
    private static boolean daySide() {
        OrbitStatePayload o = ClientState.orbit;
        if (o == null) {
            return false;
        }
        Vec3d r = OrbitNow.stateNow(o, 0).r();
        return r.x() / r.length() > 0.55;
    }

    private record Look(String name, Vec3d dir) {
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (done || mc.player == null || mc.level == null) {
            return;
        }
        // A window behind others must not pause the game (and so freeze the ship and the script).
        mc.options.pauseOnLostFocus = false;
        if (MODE.equals("climb")) {
            climbTick(mc);
            return;
        }
        String dim = mc.level.dimension().identifier().getPath();
        if (!dim.equals(MODE) || (MODE.equals("orbit") && !daySide())) {
            ticks = 0;
            return;
        }
        if (++ticks < WAIT_TICKS) {
            return;
        }
        if (System.getenv("APOAPSIS_SKY_THIRD") != null) {
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        }
        List<Look> looks = MODE.equals("orbit") ? orbitLooks(mc) : paleLooks(mc);
        if (step >= looks.size() && MODE.equals("pale") && phase == 0) {
            // First pass (high up) done: wait for the touchdown, then a quick second pass on the ground.
            phase = 1;
            step = 0;
            stepTick = 0;
            forced = false;
            settle = 6;
            tag = "ground";
            return;
        }
        if (phase == 1 && mc.player.getY() - 64 > 8 && step == 0 && stepTick == 0) {
            return;
        }
        if (step >= looks.size()) {
            done = true;
            forced = false;
            if (System.getenv("APOAPSIS_SKY_QUIT") != null) {
                mc.stop();
            }
            return;
        }
        if (HOLD != null) {
            // Measuring: keep one look, with vsync off, then stop after the hold time.
            mc.options.enableVsync().set(false);
            mc.options.framerateLimit().set(260);
            for (Look l : looks) {
                if (l.name().equals(HOLD)) {
                    Vec3d d = l.dir().normalize();
                    yaw = (float) Math.toDegrees(Math.atan2(-d.x(), d.z()));
                    pitch = (float) Math.toDegrees(-Math.asin(Math.max(-1, Math.min(1, d.y()))));
                    forced = true;
                }
            }
            if (++stepTick > HOLD_TICKS) {
                done = true;
                mc.stop();
            }
            return;
        }
        Look look = looks.get(step);
        if (!mc.gui.hud.isHidden()) {
            mc.gui.hud.toggle();
        }
        if (stepTick < settle) {
            // Follows the target while it settles: the ship keeps moving along its orbit.
            Vec3d d = look.dir().normalize();
            yaw = (float) Math.toDegrees(Math.atan2(-d.x(), d.z()));
            pitch = (float) Math.toDegrees(-Math.asin(Math.max(-1, Math.min(1, d.y()))));
            forced = true;
        }
        if (++stepTick == settle) {
            String name = PREFIX + (tag.isEmpty() ? "-" : "-" + tag + "-") + look.name() + ".png";
            Screenshot.grab(mc.gameDirectory, name, mc.gameRenderer.mainRenderTarget(), 1,
                    m -> LOG.info("[sky shots] {}: {}", name, m.getString()));
        }
        if (stepTick >= settle + (phase == 1 ? 1 : 8)) {
            step++;
            stepTick = 0;
        }
    }

    private static int climbShots;

    /**
     * The climb: with {@code APOAPSIS_SKY_SHOTS=climb} (run with testAscent), a screenshot of the flight view every
     * {@code APOAPSIS_SKY_EVERY} ticks (default 12) from where the climb starts (1 km up on Verdance) and on into the
     * orbit dimension, named by altitude. The view is the pilot's own; nothing is steered.
     */
    private static void climbTick(Minecraft mc) {
        double share = SkyBodies.climbShare(mc.level.dimension(), mc.player.getY());
        boolean orbit = mc.level.dimension().identifier().getPath().equals("orbit");
        if (share <= 0 && !orbit) {
            return;
        }
        int every = System.getenv("APOAPSIS_SKY_EVERY") == null ? 12 : Integer.parseInt(System.getenv("APOAPSIS_SKY_EVERY"));
        if (++ticks % every != 0) {
            return;
        }
        String name = String.format("%s-climb-%03d-%s-share%02d.png", PREFIX, climbShots++, orbit ? "orbit" : "air",
                (int) Math.round(share * 100));
        Screenshot.grab(mc.gameDirectory, name, mc.gameRenderer.mainRenderTarget(), 1,
                m -> LOG.info("[sky shots] {}: {}", name, m.getString()));
    }

    /** Rotates {@code v} about unit {@code axis} by {@code angle} radians. */
    private static Vec3d rotate(Vec3d v, Vec3d axis, double angle) {
        return v.scale(Math.cos(angle)).add(axis.cross(v).scale(Math.sin(angle)))
                .add(axis.scale(axis.dot(v) * (1 - Math.cos(angle))));
    }

    private static List<Look> orbitLooks(Minecraft mc) {
        OrbitStatePayload o = ClientState.orbit;
        CelestialBody central = MeridianSystem.byId(BodyId.byOrdinal(o.body()).id);
        Vec3d r = OrbitNow.stateNow(o, 0).r();
        Vec3d planet = SkyMath.ORBIT.apply(r).scale(-1).normalize();
        Vec3d sun = SkyMath.ORBIT.apply(SUN);
        double angle = Math.asin(Math.min(1, central.radius / r.length()));
        Vec3d side = sun.cross(planet).normalize();
        // The limb at the terminator: on the planet's edge, 90 degrees round from the sun's side.
        Vec3d limb = planet.scale(Math.cos(angle * 0.92)).add(side.scale(Math.sin(angle * 0.92)));
        Vec3d centre = SkyMath.ORBIT.apply(SpaceSky.galacticCentre());
        List<Look> l = new ArrayList<>();
        // Deep sky: the galactic centre, or the opposite side when the planet is in the way.
        double clear = angle + Math.toRadians(35);
        Vec3d deep = Math.acos(Math.max(-1, Math.min(1, centre.normalize().dot(planet)))) < clear ? centre.scale(-1) : centre;
        l.add(new Look("deep", deep));
        // The band at its clearest: the point on the galactic equator farthest from the planet and the sun.
        Vec3d best = centre;
        double bestAngle = -1;
        for (int k = 0; k < 72; k++) {
            double t = 2 * Math.PI * k / 72;
            Vec3d p = SkyMath.ORBIT.apply(SpaceSky.galacticAxis(0).scale(Math.cos(t))
                    .add(SpaceSky.galacticAxis(1).scale(Math.sin(t)))).normalize();
            double fromPlanet = Math.acos(Math.max(-1, Math.min(1, p.dot(planet)))) - angle;
            double fromSun = Math.acos(Math.max(-1, Math.min(1, p.dot(sun.normalize()))));
            double a = Math.min(fromPlanet, fromSun);
            if (a > bestAngle) {
                bestAngle = a;
                best = p;
            }
        }
        l.add(0, new Look("milkyway", best));
        l.add(new Look("pole", SkyMath.ORBIT.apply(SpaceSky.galacticPole())));
        l.add(new Look("sun", sun));
        l.add(new Look("nearsun", rotate(sun, side, Math.toRadians(35))));
        l.add(new Look("limb", limb));
        l.add(new Look("planet", planet));
        l.add(new Look("planet-edge", rotate(planet, side, angle * 0.6)));
        return l;
    }

    private static List<Look> paleLooks(Minecraft mc) {
        Vec3 cam = mc.player.getEyePosition();
        double radius = MeridianSystem.PALE.radius;
        double lon = cam.x / radius;
        double lat = -cam.z / radius;
        Axes axes = SkyMath.surfaceAxes(lon, lat);
        Vec3d p = SkyMath.surfaceViewer(radius, lon, lat, 0);
        OrbitStatePayload o = ClientState.orbit;
        double ut = o != null ? OrbitNow.utNow(o, 0) : mc.level.getGameTime() / 20.0;
        Vec3d toVerdance = SkyMath.systemPosition(MeridianSystem.VERDANCE, ut)
                .sub(SkyMath.systemPosition(MeridianSystem.PALE, ut)).sub(p);
        List<Look> l = new ArrayList<>();
        l.add(new Look("verdance", axes.apply(toVerdance.normalize())));
        l.add(new Look("sun", axes.apply(SUN)));
        l.add(new Look("zenith", new Vec3d(0.001, 1, 0)));
        l.add(new Look("deep", axes.apply(SpaceSky.galacticCentre())));
        l.add(new Look("pole", axes.apply(SpaceSky.galacticPole())));
        return l;
    }
}
