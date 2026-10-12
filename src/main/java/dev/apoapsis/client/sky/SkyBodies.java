package dev.apoapsis.client.sky;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.apoapsis.Apoapsis;
import dev.apoapsis.client.ClientState;
import dev.apoapsis.client.OrbitNow;
import dev.apoapsis.client.sky.SkyMath.Axes;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.FlightDimensions;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.Vec3d;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * The bodies in the sky, drawn through {@link SubmitCustomGeometryEvent}, so they share the level's translucent feature
 * pass and depth test. A body with a surface map is a textured, lit sphere cap ({@link PlanetCap}); otherwise a flat
 * disc. Verdance keeps its atmosphere rim. Orbit dimension: the central body and the other body from the piloted ship.
 * Overworld: Pale. Pale: Verdance. Client only.
 *
 * <p>Every vertex sits at {@code FAR_FRACTION * depthFar} from the camera, on the sight line of its angle, so the
 * image matches a flat disc of the same angular size while nothing is clipped by the far plane.
 */
public final class SkyBodies {
    private SkyBodies() {}

    /** Segments per circle: the disc fan and the rim have this many triangles. */
    private static final int SEGMENTS = 48;
    /** Share of the far plane at which the sky is drawn. */
    private static final double FAR_FRACTION = 0.9;
    /** Brightness of the unlit side of a lit body, 0..1. */
    private static final double AMBIENT = 0.10;
    /** Largest rim angle, so the glow stays in front of the camera's sky. */
    private static final double MAX_RIM = Math.toRadians(170);
    /** Sun direction in inertial axes: +X. In the orbit dimension this is local +X too. */
    private static final Vec3d SUN = new Vec3d(1, 0, 0);

    private static final int VERDANCE_RGB = 0x2F6F63;
    private static final int PALE_RGB = 0x8D8D88;
    private static final int OVERWORLD_SEA_LEVEL = 63;
    private static final int PALE_SEA_LEVEL = 64;

    private static final Identifier ORBIT_ID = Identifier.fromNamespaceAndPath(Apoapsis.MODID, "orbit");
    private static final Identifier PALE_ID = Identifier.fromNamespaceAndPath(Apoapsis.MODID, "pale");

    /**
     * Textured sphere pipeline: the GUI textured snippet (position, UV, colour; translucent blend) with the sky's depth
     * test, reversed-Z as the flat discs use, and no culling.
     */
    private static final RenderPipeline PLANET_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "pipeline/planet"))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();
    /** The finer planet look: the same quads with the planet shader (lit as the far terrain, a blue night side). */
    private static final RenderPipeline PLANET_PIPELINE_SPACE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "pipeline/planet_space"))
            .withVertexShader(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "core/space_planet"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "core/space_planet"))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();
    private static final Map<String, RenderType> PLANET_TYPES = new HashMap<>();

    /** One reusable cap per body, so the two bodies in a frame never share arrays. */
    private static final PlanetCap VERDANCE_CAP = new PlanetCap();
    private static final PlanetCap PALE_CAP = new PlanetCap();

    /**
     * A body to draw: its direction (local axes) and distance from the viewer, angular radii, colour and lighting, and
     * the viewer's position relative to the body centre (inertial) with the axes that take inertial vectors to local.
     */
    private record Disc(CelestialBody body, double distance, Vec3d dir, double angle, double rimAngle, int rgb,
            boolean lit, Vec3d viewer, Axes axes, double alpha) {
    }

    /** Vertices (x, y, z per vertex), ARGB per vertex, and the vertex order for one draw. */
    private record Mesh(RenderType type, float[] xyz, int[] argb, int[] order) {
    }

    /** Orthonormal axes perpendicular to a disc's direction. */
    private record Basis(Vec3d dir, Vec3d e1, Vec3d e2) {
        static Basis of(Vec3d dir) {
            Vec3d a = Math.abs(dir.x()) < 0.9 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0);
            Vec3d e1 = dir.cross(a).normalize();
            return new Basis(dir, e1, dir.cross(e1));
        }

        /** Unit limb direction at segment {@code i}: the surface normal of a limb point. */
        Vec3d limb(int i) {
            double t = 2 * Math.PI * i / SEGMENTS;
            return e1.scale(Math.cos(t)).add(e2.scale(Math.sin(t)));
        }

        /** Sight line at angle {@code phi} from the centre, toward limb direction {@code n}. */
        Vec3d sightLine(double phi, Vec3d n) {
            return dir.scale(Math.cos(phi)).add(n.scale(Math.sin(phi)));
        }
    }

    /**
     * Submit hook. Fires once per frame in the level's feature pass; does nothing outside the three dimensions,
     * and in the orbit dimension does nothing while not piloting.
     */
    public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        OrbitStatePayload orbit = ClientState.orbit;
        float partialTick = event.getLevelRenderState().worldPartialTicks;
        Identifier dim = level.dimension().identifier();
        List<Disc> discs;
        if (dim.equals(ORBIT_ID)) {
            if (orbit == null) {
                return;
            }
            discs = orbitDiscs(orbit, partialTick);
        } else if (Level.OVERWORLD.equals(level.dimension())) {
            discs = surfaceDiscs(level.dimension(), MeridianSystem.VERDANCE, MeridianSystem.PALE,
                    time(orbit, level, partialTick), camera(event));
        } else if (dim.equals(PALE_ID)) {
            discs = surfaceDiscs(FlightDimensions.PALE, MeridianSystem.PALE, MeridianSystem.VERDANCE,
                    time(orbit, level, partialTick), camera(event));
        } else {
            return;
        }
        if (discs.isEmpty()) {
            return;
        }

        double far = FAR_FRACTION * event.getLevelRenderState().cameraRenderState.depthFar;
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        // Farthest first, so a nearer body draws over a farther one.
        discs.sort(Comparator.comparingDouble(Disc::distance).reversed());
        for (Disc disc : discs) {
            Basis basis = Basis.of(disc.dir());
            if (disc.rimAngle() > 0) {
                submit(collector, poseStack, rimMesh(disc, basis, far));
            }
            RenderType planet = planetType(disc.body());
            if (planet != null) {
                submitCap(collector, poseStack, disc, planet, far);
                if (disc.rimAngle() > 0) {
                    submit(collector.order(1), poseStack, haloMesh(disc, basis, far));
                }
            } else {
                submit(collector, poseStack, discMesh(disc, basis, far));
            }
        }
    }

    /** The textured render type for a body's map, or null while the body has no map. */
    private static RenderType planetType(CelestialBody body) {
        Identifier map = PlanetTextures.texture(body.id);
        if (map == null) {
            return null;
        }
        if (SpaceSky.off()) {
            return PLANET_TYPES.computeIfAbsent(body.id, id -> RenderType.create("apoapsis_planet",
                    RenderSetup.builder(PLANET_PIPELINE).withTexture("Sampler0", map).createRenderSetup()));
        }
        // Asks for the next refined level; before the cache so it runs every frame.
        PlanetTextures.want(body.id);
        // Mipmapped, anisotropic, wrapping round the longitude and clamped at the poles.
        return PLANET_TYPES.computeIfAbsent(body.id, id -> RenderType.create("apoapsis_planet_space",
                RenderSetup.builder(PLANET_PIPELINE_SPACE).withTexture("Sampler0", map, PlanetTextures::sampler)
                        .createRenderSetup()));
    }

    /** Universal time now: the piloted ship's, advanced to this frame, else the level's game time in seconds. */
    private static double time(OrbitStatePayload orbit, Level level, float partialTick) {
        return orbit != null ? OrbitNow.utNow(orbit, partialTick) : level.getGameTime() / 20.0;
    }

    private static Vec3 camera(SubmitCustomGeometryEvent event) {
        return event.getLevelRenderState().cameraRenderState.pos;
    }

    /** The central body (in -r) and the other body, seen from the piloted ship now. */
    private static List<Disc> orbitDiscs(OrbitStatePayload o, float partialTick) {
        CelestialBody central = MeridianSystem.byId(BodyId.byOrdinal(o.body()).id);
        CelestialBody other = central == MeridianSystem.VERDANCE ? MeridianSystem.PALE : MeridianSystem.VERDANCE;
        double ut = OrbitNow.utNow(o, partialTick);
        Vec3d r = OrbitNow.stateNow(o, partialTick).r();
        List<Disc> out = new ArrayList<>(2);

        double rl = r.length();
        if (rl > central.radius) {
            out.add(disc(central, r, SkyMath.ORBIT, true));
        }
        Vec3d ship = SkyMath.systemPosition(central, ut).add(r);
        Vec3d toOther = SkyMath.systemPosition(other, ut).sub(ship);
        double dOther = toOther.length();
        if (dOther > other.radius) {
            out.add(disc(other, toOther.scale(-1), SkyMath.ORBIT, true));
        }
        return out;
    }

    /**
     * The climb's share 0..1 for a camera at height {@code camY} (world y) in dimension {@code dim}: 0 up to 1 km over
     * the surface, 1 at the transition altitude to orbit, 0 in every dimension that is not a surface one. Client only.
     */
    public static double climbShare(ResourceKey<Level> dim, double camY) {
        if (!Level.OVERWORLD.equals(dim) && !FlightDimensions.PALE.equals(dim)) {
            return 0;
        }
        double altitude = camY - FlightDimensions.seaLevel(dim);
        return SkyClimb.share(altitude, FlightDimensions.transitionAltitude(FlightDimensions.surfaceBody(dim)));
    }

    /**
     * The local body from the camera's real position, once the climb has started (it fades in with altitude), and the
     * other body, above the local horizon. The local body is a textured cap like the orbit one, from the same sight lines.
     */
    private static List<Disc> surfaceDiscs(ResourceKey<Level> dim, CelestialBody local, CelestialBody other, double ut,
            Vec3 cam) {
        double radius = local.radius;
        double lon = cam.x() / radius;
        double lat = -cam.z() / radius;
        double altitude = cam.y() - seaLevel(local);
        Vec3d p = SkyMath.surfaceViewer(radius, lon, lat, altitude);
        List<Disc> out = new ArrayList<>(2);
        double share = climbShare(dim, cam.y());
        if (share > 0 && planetType(local) != null) {
            out.add(disc(local, p, SkyMath.surfaceAxes(lon, lat), true, share));
        }
        Vec3d toOther = SkyMath.systemPosition(other, ut).sub(SkyMath.systemPosition(local, ut)).sub(p);
        double d = toOther.length();
        if (d > other.radius) {
            Vec3d w = toOther.scale(1 / d);
            if (!SkyMath.belowHorizon(p, w, radius)) {
                out.add(disc(other, toOther.scale(-1), SkyMath.surfaceAxes(lon, lat), false));
            }
        }
        return out;
    }

    /**
     * A disc for {@code body} seen from {@code viewer} (inertial, relative to the body centre), in the dimension's
     * {@code axes}. The direction and distance are the viewer's line to the body centre.
     */
    private static Disc disc(CelestialBody body, Vec3d viewer, Axes axes, boolean lit) {
        return disc(body, viewer, axes, lit, 1);
    }

    /** As {@link #disc(CelestialBody, Vec3d, Axes, boolean)}, drawn at {@code alpha} 0..1. */
    private static Disc disc(CelestialBody body, Vec3d viewer, Axes axes, boolean lit, double alpha) {
        double distance = viewer.length();
        Vec3d dir = axes.apply(viewer.scale(-1 / distance));
        double angle = Math.asin(Math.min(1, body.radius / distance));
        double rimAngle = rimAngleOf(body, distance, angle);
        int rgb = body == MeridianSystem.PALE ? PALE_RGB : VERDANCE_RGB;
        return new Disc(body, distance, dir, angle, rimAngle, rgb, lit, viewer, axes, alpha);
    }

    /**
     * The angle from the body centre where its atmosphere glow ends, or 0 for a body without an atmosphere. Outside the
     * atmosphere that is the top of its shell, at least 8% past the limb. Inside it (camera below the top), the horizon,
     * pushed past 90 degrees as the camera sinks toward the surface, so the haze reaches up past the horizon.
     */
    private static double rimAngleOf(CelestialBody body, double distance, double angle) {
        double h = body.atmosphereTop;
        if (h <= 0) {
            return 0;
        }
        double r = body.radius;
        double rim;
        if (distance >= r + h) {
            double top = Math.asin(Math.min(1, (r + h) / distance));
            rim = angle + Math.max(top - angle, 0.08 * angle);
        } else {
            double alt = Math.max(0, distance - r);
            rim = Math.max(angle + 0.08 * angle, Math.PI / 2 + Math.toRadians(16) * (1 - alt / h));
        }
        return Math.min(rim, MAX_RIM);
    }

    private static int seaLevel(CelestialBody body) {
        return body == MeridianSystem.PALE ? PALE_SEA_LEVEL : OVERWORLD_SEA_LEVEL;
    }

    /** Lambert term for a disc: {@code dot} is the cosine between the surface normal and the sun. */
    private static double light(Disc disc, double dot) {
        return disc.lit() ? AMBIENT + (1 - AMBIENT) * Math.max(0, dot) : 1;
    }

    private static int argb(int alpha, int rgb, double light) {
        int r = (int) Math.round(((rgb >> 16) & 0xFF) * light);
        int g = (int) Math.round(((rgb >> 8) & 0xFF) * light);
        int b = (int) Math.round((rgb & 0xFF) * light);
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    /** A fan: the centre on the disc direction, then SEGMENTS+1 limb vertices (the last repeats the first). */
    private static Mesh discMesh(Disc disc, Basis basis, double far) {
        int n = SEGMENTS + 2;
        float[] xyz = new float[n * 3];
        int[] argb = new int[n];
        put(xyz, 0, disc.dir().scale(far));
        argb[0] = argb(255, disc.rgb(), light(disc, -disc.dir().dot(SUN)));
        for (int i = 0; i <= SEGMENTS; i++) {
            Vec3d limb = basis.limb(i);
            put(xyz, i + 1, basis.sightLine(disc.angle(), limb).scale(far));
            argb[i + 1] = argb(255, disc.rgb(), light(disc, limb.dot(SUN)));
        }
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        return new Mesh(RenderTypes.debugTriangleFan(), xyz, argb, order);
    }

    /** The atmosphere glow from the limb out to the rim angle, as quads (a fan cannot have a hole for the body). */
    private static Mesh rimMesh(Disc disc, Basis basis, double far) {
        AtmosphereRim.Geometry g = AtmosphereRim.outer(disc.dir(), basis.e1(), basis.e2(), disc.angle(),
                disc.rimAngle(), disc.alpha(), disc.axes().apply(SUN), far, AtmosphereRim.SEGMENTS);
        return new Mesh(RenderTypes.debugQuads(), g.xyz(), g.argb(), g.order());
    }

    /** The thin halo just inside the limb, drawn over the planet cap. */
    private static Mesh haloMesh(Disc disc, Basis basis, double far) {
        AtmosphereRim.Geometry g = AtmosphereRim.inner(disc.dir(), basis.e1(), basis.e2(), disc.angle(), disc.alpha(),
                disc.axes().apply(SUN), far, AtmosphereRim.SEGMENTS);
        return new Mesh(RenderTypes.debugQuads(), g.xyz(), g.argb(), g.order());
    }

    private static void put(float[] xyz, int vertex, Vec3d p) {
        xyz[3 * vertex] = (float) p.x();
        xyz[3 * vertex + 1] = (float) p.y();
        xyz[3 * vertex + 2] = (float) p.z();
    }

    /** Hands the mesh to the level's submit collector; vertices are written later, in the feature pass. */
    private static void submit(OrderedSubmitNodeCollector collector, PoseStack poseStack, Mesh mesh) {
        collector.submitCustomGeometry(poseStack, mesh.type(), (pose, buffer) -> {
            for (int v : mesh.order()) {
                int c = mesh.argb()[v];
                buffer.addVertex(pose, mesh.xyz()[3 * v], mesh.xyz()[3 * v + 1], mesh.xyz()[3 * v + 2])
                        .setColor((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, c >>> 24);
            }
        });
    }

    /** Rebuilds the body's reusable cap for this frame and hands it to the collector. */
    private static void submitCap(SubmitNodeCollector collector, PoseStack poseStack, Disc disc, RenderType type,
            double far) {
        PlanetCap cap = disc.body() == MeridianSystem.PALE ? PALE_CAP : VERDANCE_CAP;
        cap.build(disc.body().radius, disc.viewer(), disc.axes(), far, SUN, disc.alpha());
        cap.setNightBlue(SpaceSky.off() ? -1 : disc.body().atmosphereTop > 0 ? 255 : 0);
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> cap.emit(pose, buffer));
    }
}
