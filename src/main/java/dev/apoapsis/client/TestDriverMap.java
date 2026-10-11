package dev.apoapsis.client;

import static dev.apoapsis.client.TestDriverSupport.shot;

import com.mojang.logging.LogUtils;
import dev.apoapsis.client.map3d.Map3DScript;
import dev.apoapsis.client.map3d.MapSites;
import dev.apoapsis.client.map3d.OrbitPlot;
import dev.apoapsis.client.sky.PlanetTextures;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.FlightDimensions;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Elements;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

/**
 * The 3D map script for {@link TestDriver} ({@code apoapsis.testDriver=map}): from the test save's orbit it opens the map
 * (the 3D one is the default), waits for the planet maps, places a node on the orbit line with a click, drags the
 * prograde handle to raise the apoapsis, deletes the node with a right click, focuses Verdance for the site markers,
 * and flips to the 2D map and back with Tab. Screenshots {@code m08-*.png} go to the screenshots folder.
 */
final class TestDriverMap implements TestDriverSupport.Script {
    private static final Logger LOG = LogUtils.getLogger();
    private static final int MAPS_TIMEOUT_TICKS = 1800;

    private enum Phase { SETTLE, WAIT_MAPS, PICK, PLACE, DRAG, DELETE, FOCUS, MARKERS, DONE }

    private Phase phase = Phase.SETTLE;
    private int phaseTicks;
    private int totalTicks;

    @Override
    public boolean tick(Minecraft mc, ShipEntity ship) {
        if (ship == null) {
            LOG.warn("[test driver] map: the pilot left the ship during {}", phase);
            return false;
        }
        phaseTicks++;
        if (++totalTicks == 20) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        OrbitStatePayload p = ClientState.orbit;
        switch (phase) {
            case SETTLE -> {
                if (phaseTicks >= 40 && p != null) {
                    MapClient.openMap();
                    next(Phase.WAIT_MAPS);
                }
            }
            case WAIT_MAPS -> {
                boolean maps = PlanetTextures.texture("verdance") != null && PlanetTextures.texture("pale") != null;
                // Past the first-join toasts (about ten seconds), which would cover the corner of the view.
                if ((maps && phaseTicks >= 260) || phaseTicks >= MAPS_TIMEOUT_TICKS) {
                    LOG.info("[test driver] map: planet maps {} after {} ticks", maps ? "present" : "MISSING", phaseTicks);
                    shot(mc, "m08-01-default-3d-map.png");
                    LOG.info("[test driver] map: at the start, {}", Map3DScript.focusState());
                    next(Phase.PICK);
                }
            }
            case PICK -> {
                // The ship's line a few seconds ahead lies over Verdance's disc: the click must take the line, not the planet.
                if (phaseTicks == 5 && p != null) {
                    // The cost of one prediction, which a handle drag recomputes every frame.
                    CelestialBody body = FlightDimensions.system(BodyId.byOrdinal(p.body()));
                    long t0 = System.nanoTime();
                    int runs = 200;
                    for (int i = 0; i < runs; i++) {
                        OrbitPlot.compute(body, TestDriverSupport.stateOf(p), p.ut(), p.ut());
                    }
                    LOG.info("[test driver] map: one orbit prediction takes {} ms",
                            String.format(java.util.Locale.ROOT, "%.2f", (System.nanoTime() - t0) / 1e6 / runs));
                } else if (phaseTicks == 10 && p != null) {
                    CelestialBody body = FlightDimensions.system(BodyId.byOrdinal(p.body()));
                    double period = Elements.of(body.mu, TestDriverSupport.stateOf(p)).period();
                    boolean placed = Map3DScript.clickOrbit(0.05 * period);
                    LOG.info("[test driver] map: click on the line over the planet placed a node: {}; {}", placed,
                            Map3DScript.focusState());
                } else if (phaseTicks == 20) {
                    LOG.info("[test driver] map: right click deleted it: {}", Map3DScript.rightClickNode());
                    next(Phase.PLACE);
                }
            }
            case PLACE -> {
                if (phaseTicks == 20 && p != null) {
                    CelestialBody body = FlightDimensions.system(BodyId.byOrdinal(p.body()));
                    double period = Elements.of(body.mu, TestDriverSupport.stateOf(p)).period();
                    boolean placed = Map3DScript.clickOrbit(0.2 * period);
                    LOG.info("[test driver] map: click on the orbit placed a node: {}; {}", placed, Map3DScript.describe());
                } else if (phaseTicks == 40) {
                    next(Phase.DRAG);
                }
            }
            case DRAG -> {
                if (phaseTicks == 10) {
                    boolean dragged = Map3DScript.dragHandle(0, 90);
                    LOG.info("[test driver] map: prograde handle dragged: {}; {}", dragged, Map3DScript.describe());
                } else if (phaseTicks == 40) {
                    shot(mc, "m08-02-node-handles-predicted.png");
                } else if (phaseTicks == 50) {
                    next(Phase.DELETE);
                }
            }
            case DELETE -> {
                if (phaseTicks == 10) {
                    boolean gone = Map3DScript.rightClickNode();
                    LOG.info("[test driver] map: right click deleted the node: {}", gone);
                } else if (phaseTicks == 20) {
                    next(Phase.FOCUS);
                }
            }
            case FOCUS -> {
                // A click on Pale focuses it, and the view settles on it exactly.
                if (phaseTicks == 5) {
                    LOG.info("[test driver] map: click on Pale: {}", Map3DScript.clickBody(1));
                } else if (phaseTicks == 60) {
                    LOG.info("[test driver] map: after the click, {}", Map3DScript.focusState());
                    Map3DScript.focus(2, 6 * 64000);
                } else if (phaseTicks == 120) {
                    LOG.info("[test driver] map: back on the ship, {}", Map3DScript.focusState());
                    next(Phase.MARKERS);
                }
            }
            case MARKERS -> {
                if (phaseTicks == 1) {
                    // The test save has no landing, so seed one for the screenshot; the home marker is the world spawn.
                    MapSites.CURRENT.recordLanding(BodyId.VERDANCE, MapSites.fromSurface(-12000, -9000, 64000));
                    Map3DScript.focus(0, 2.8 * 64000);
                    Map3DScript.rotate(0, 25);
                } else if (phaseTicks == 80) {
                    shot(mc, "m08-03-markers.png");
                } else if (phaseTicks == 90) {
                    MapClient.switchMap();
                } else if (phaseTicks == 150) {
                    shot(mc, "m08-04-2d-map-via-tab.png");
                    MapClient.switchMap();
                    next(Phase.DONE);
                }
            }
            case DONE -> {
                if (phaseTicks >= 20) {
                    return false;
                }
            }
            default -> { }
        }
        return true;
    }

    private void next(Phase to) {
        phase = to;
        phaseTicks = 0;
    }
}
