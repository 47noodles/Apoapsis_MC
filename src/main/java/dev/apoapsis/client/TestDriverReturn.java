package dev.apoapsis.client;

import static dev.apoapsis.client.TestDriverSupport.aim;
import static dev.apoapsis.client.TestDriverSupport.altitudeM;
import static dev.apoapsis.client.TestDriverSupport.fmt;
import static dev.apoapsis.client.TestDriverSupport.inOrbit;
import static dev.apoapsis.client.TestDriverSupport.send;
import static dev.apoapsis.client.TestDriverSupport.shot;
import static dev.apoapsis.client.TestDriverSupport.speedMps;
import static dev.apoapsis.client.TestDriverSupport.stateOf;
import static dev.apoapsis.client.TestDriverSupport.steer;

import com.mojang.logging.LogUtils;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.FlightDimensions;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.net.ShipInputPayload;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Elements;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.Patch;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.TrajectoryPredictor;
import dev.apoapsis.orbit.maneuver.EscapeSearch;
import dev.apoapsis.orbit.maneuver.Maneuvers;
import dev.apoapsis.ship.DescentGear;
import dev.apoapsis.ship.SasMode;
import dev.apoapsis.ship.ShipEntity;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.slf4j.Logger;

/**
 * The return-trip script for {@link TestDriver} ({@code apoapsis.testDriver=return}, the testReturn run). One run flies
 * the whole way home with the player's own controls and commands, and writes {@code m09-telemetry.csv} and the
 * {@code m09-*.png} shots into run/screenshots. It never moves the ship between legs. Five legs:
 * <ol>
 *   <li>Pale landing: the transfer and landing of {@link TestDriverPale}, unchanged.</li>
 *   <li>Pale ascent: vertical, then a 60 degree pitch-over east with SAS HOLD, cut at a 12.5 km predicted apoapsis, a
 *   prograde burn at apoapsis to a circular orbit (periapsis 11 km, above the 10 km warp floor).</li>
 *   <li>Transfer back: {@link EscapeSearch} finds the prograde burn (and the lap) that leaves Pale's sphere of influence
 *   with a Verdance periapsis of 6 km, near the longitude of the world spawn; warp to it, burn, check the prediction,
 *   warp across the sphere of influence and down to the atmosphere.</li>
 *   <li>Re-entry: SAS RETROGRADE, a full-throttle retrograde burn from 10 km down to 235 m/s, then the chutes
 *   (they shred above 250 m/s) while still above 2 km.</li>
 *   <li>Landing: the chute descent through the 2 km regime change to the ground, on the legs.</li>
 * </ol>
 * {@code -Dapoapsis.returnStage=ascent|escape|entry} starts a later leg from a labelled stage start (a scenario command
 * spawns the lander in that state) so a fix to one leg can be tried in a minute. The default {@code full} flies all five.
 */
final class TestDriverReturn implements TestDriverSupport.Script {
    private static final Logger LOG = LogUtils.getLogger();
    private static final CelestialBody PALE = MeridianSystem.PALE;
    private static final CelestialBody VERDANCE = MeridianSystem.VERDANCE;
    private static final String STAGE = System.getProperty("apoapsis.returnStage", "full");

    private static final double DT = 0.05;
    private static final int TIMEOUT_TICKS = 75 * 60 * 20;
    private static final int START_TICKS = 80;
    private static final int STAGE_WAIT_TICKS = 40 * 20;
    private static final int SAMPLE_TICKS = 20;
    /** Vacuum exhaust speed of the Spark, m/s: Isp 440 s times the ship's g0 of 9.81. */
    private static final double VE_VAC_MPS = 440 * 9.81;

    // Leg 2: Pale ascent.
    private static final double KICK_M = 120;
    private static final double PITCH_DEG = 60;
    private static final double AP_CUT_M = 12_500;
    private static final double CIRC_PE_M = 11_000;
    private static final double ORBIT_OK_PE_M = 10_400;
    private static final double APOAPSIS_RADIAL_MPS = 1.5;
    private static final int CLIMB_CAP_TICKS = 60 * 20;
    private static final int COAST_CAP_TICKS = 420 * 20;
    private static final int CIRC_CAP_TICKS = 40 * 20;

    // Leg 3: transfer back.
    private static final double RETURN_PE_M = 6_000;
    private static final double PE_OK_MIN_M = -4_000;
    private static final double PE_OK_MAX_M = 10_000;
    private static final double MAX_ESCAPE_DV_MPS = 130;
    private static final int PLAN_LAPS = 20;
    private static final double PLAN_STEP_S = 45;
    /** Metres flown from the atmosphere top to the touchdown: the burn and the chute opening. */
    private static final double ENTRY_RANGE_M = 16_000;
    /** The landing aims this far east of the world spawn, so it misses the spawn pad. */
    private static final double LAND_OFFSET_M = 2_500;
    private static final double PLAN_LEAD_S = 150;
    private static final double BURN_CUT_MARGIN_MPS = 0.8;
    private static final double ALIGN_DEG = 3;
    private static final int[] WARP_LEVELS = {1, 5, 10, 50, 100, 1000};
    /** Warp margin: a level is used only while the event is further off than WARP_BASE_S + WARP_PER_LEVEL_S * level. */
    private static final double WARP_BASE_S = 40;
    private static final double WARP_PER_LEVEL_S = 2.5;
    /** Seconds before the atmosphere at which the ship is at warp 1 and turning retrograde. */
    private static final double ENTRY_ALIGN_S = 100;

    // Leg 4: re-entry.
    private static final double ENTRY_BURN_ALT_M = 10_000;
    private static final double CHUTE_CUT_MPS = 235;
    private static final double CHUTE_ALT_MIN_M = 2_000;
    private static final int ENTRY_CAP_TICKS = 90 * 20;

    private enum Phase {
        START, STAGE_WAIT,
        PALE_LEG,
        ASC_SETTLE, ASC_CLIMB, ASC_COAST, ASC_CIRC, ASC_VERIFY,
        ESC_PLAN, ESC_WAIT, ESC_BURN, ESC_VERIFY, ESC_COAST, ENTRY_ALIGN,
        ENTRY_BURN, CHUTES,
        DESCENT, LANDED, DONE
    }

    /** One leg's result. */
    private record Leg(int number, String name, boolean pass, double dvMps, double fuelKg, double gameS, double realS,
                       String note) {}

    private final TestDriverPale pale = new TestDriverPale();
    /** Opened on the first tick: the script is built while the mod loads, before the game exists. */
    private ReturnTelemetry csv;
    private final List<Leg> legs = new ArrayList<>();

    private Phase phase = Phase.START;
    private int phaseTicks;
    private int ticks;
    private boolean failed;

    // Leg bookkeeping.
    private int legNumber;
    private String legName = "";
    private double legMass;
    private double legFuel;
    private double legUt;
    private int legStartTick;
    private String stageLabel = "";

    // Latest numbers.
    private double lastAlt;
    private double lastSpeed;
    private double lastUt;
    private Vec3 lastVel = Vec3.ZERO;
    private Vec3 prevVel = Vec3.ZERO;

    // Per-phase memory.
    private boolean shotA;
    private boolean shotB;
    private int shotAt = -1;
    private String shotName;
    private EscapeSearch.Plan plan;
    private double burnMass;
    private int verifyTries;
    private double atmosphereUt = Double.NaN;
    private double soiExitUt = Double.NaN;
    private int lastPredictTick = -1000;
    private double chuteDeployAlt = Double.NaN;
    private int chuteSentTick = -1000;

    @Override
    public boolean tick(Minecraft mc, ShipEntity ship) {
        ticks++;
        phaseTicks++;
        if (ticks == 1) {
            csv = new ReturnTelemetry(mc.gameDirectory.toPath().resolve("screenshots").resolve("m09-telemetry.csv"));
            mc.options.chatVisibility().set(ChatVisiblity.HIDDEN);
            LOG.info("[test driver] return: stage {}", STAGE);
        }
        mc.gui.toastManager().clear();
        mc.gui.hud.setOverlayMessage(Component.empty(), false);
        if (ship == null) {
            return shipLost(mc);
        }
        if (ticks > TIMEOUT_TICKS) {
            return abort("the 75 minute cap is up in " + phase);
        }
        OrbitStatePayload p = ClientState.orbit;
        if (p == null) {
            return true;
        }
        lastUt = p.ut();
        lastAlt = altitudeM(ship, p);
        lastSpeed = speedMps(ship, p);
        prevVel = lastVel;
        lastVel = ship.getVelocity();
        if (shotName != null && phaseTicks >= shotAt) {
            snap(mc, shotName);
            shotName = null;
        }
        sample(ship, p);

        byte pitch = 0;
        byte yaw = 0;
        byte throttle = ShipInputPayload.THROTTLE_HOLD;
        byte flags = 0;
        switch (phase) {
            case START -> {
                if (phaseTicks >= START_TICKS) {
                    startRun(mc);
                }
            }
            case STAGE_WAIT -> stageWait(mc, ship, p);
            case PALE_LEG -> {
                if (beginLegLater) {
                    beginLegLater = false;
                    beginLeg(ship, 1, "Pale landing (transfer, retrograde burn, suicide burn)");
                }
                if (!pale.tick(mc, ship)) {
                    if (!pale.touchedDown()) {
                        return failLeg(ship, "the Pale landing script ended without a landing");
                    }
                    snap(mc, "m09-01-pale-landed.png");
                    endLeg(ship, true, "landed on Pale");
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                    next(Phase.ASC_SETTLE);
                    beginLeg(ship, 2, "Pale ascent");
                }
                return true;
            }
            case ASC_SETTLE -> {
                flags |= sasPress(ship, SasMode.HOLD);
                if (phaseTicks == 30) {
                    snap(mc, "m09-02-pale-before-liftoff.png");
                }
                if (phaseTicks >= 40 && ship.getSasMode() == SasMode.HOLD) {
                    next(Phase.ASC_CLIMB);
                }
            }
            case ASC_CLIMB -> {
                double h = ship.getY() - FlightDimensions.seaLevel(BodyId.PALE);
                Vector3f want = h < KICK_M ? new Vector3f(0, 1, 0)
                        : new Vector3f((float) Math.sin(Math.toRadians(PITCH_DEG)), (float) Math.cos(Math.toRadians(PITCH_DEG)), 0);
                double[] aimed = aim(ship, want);
                pitch = (byte) aimed[0];
                yaw = (byte) aimed[1];
                throttle = ShipInputPayload.THROTTLE_FULL;
                flags |= sasPress(ship, SasMode.HOLD);
                if (phaseTicks == 100) {
                    snap(mc, "m09-02-pale-climb.png");
                }
                // The apoapsis from the ship's own state, not the payload: the payload is up to five ticks old, and the orbit
                // of a ship at rest on the ground is degenerate (e = 1, apoapsis infinite).
                double apoapsis = inOrbit(p) ? elements(p).apoapsisRadius() - PALE.radius
                        : surfaceApoapsisM(lastAlt, lastVel.y, Math.hypot(lastVel.x, lastVel.z));
                if (phaseTicks % 20 == 0) {
                    LOG.info("[test driver] return climb: alt {} m, v {} m/s, vertical {} m/s, Ap {} m, nose {} deg off, landed {}",
                            fmt(lastAlt), fmt(lastSpeed), fmt(lastVel.y), fmt(apoapsis), fmt(aimed[2]), ship.isLanded());
                }
                if (apoapsis >= AP_CUT_M) {
                    throttle = ShipInputPayload.THROTTLE_CUT;
                    LOG.info("[test driver] return: apoapsis {} m reached at {} m, engines off", fmt(apoapsis), fmt(lastAlt));
                    next(Phase.ASC_COAST);
                } else if (phaseTicks > CLIMB_CAP_TICKS) {
                    return failLeg(ship, "the climb never reached a 12.5 km apoapsis");
                }
            }
            case ASC_COAST -> {
                throttle = phaseTicks < 10 ? ShipInputPayload.THROTTLE_CUT : ShipInputPayload.THROTTLE_HOLD;
                flags |= sasPress(ship, SasMode.PROGRADE);
                double vr = radialSpeed(p);
                if (phaseTicks % 40 == 0) {
                    LOG.info("[test driver] return coast: alt {} m, radial {} m/s, regime {}", fmt(lastAlt), fmt(vr), inOrbit(p) ? "orbit" : "surface");
                }
                if (vr <= APOAPSIS_RADIAL_MPS && lastAlt > 8_000) {
                    next(Phase.ASC_CIRC);
                    burnMass = ship.massKg();
                } else if (vr < -3 && lastAlt < 8_000) {
                    return failLeg(ship, "the ship is falling back at " + fmt(lastAlt) + " m");
                } else if (phaseTicks > COAST_CAP_TICKS) {
                    return failLeg(ship, "no apoapsis within seven minutes");
                }
            }
            case ASC_CIRC -> {
                throttle = ShipInputPayload.THROTTLE_FULL;
                flags |= sasPress(ship, SasMode.PROGRADE);
                Elements e = elements(p);
                if (phaseTicks == 15) {
                    snap(mc, "m09-03-pale-circularising.png");
                }
                if (e.eccentricity() < 1 && e.periapsisRadius() - PALE.radius >= CIRC_PE_M) {
                    throttle = ShipInputPayload.THROTTLE_CUT;
                    next(Phase.ASC_VERIFY);
                } else if (phaseTicks > CIRC_CAP_TICKS) {
                    return failLeg(ship, "the circularising burn never lifted the periapsis to 11 km");
                }
            }
            case ASC_VERIFY -> {
                throttle = ShipInputPayload.THROTTLE_CUT;
                if (phaseTicks == 30) {
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                }
                if (phaseTicks == 40) {
                    snap(mc, "m09-03-pale-orbit.png");
                }
                if (phaseTicks >= 50) {
                    Elements e = elements(p);
                    double pe = e.periapsisRadius() - PALE.radius;
                    LOG.info("[test driver] return: Pale orbit Pe {} m Ap {} m e {}", fmt(pe), fmt(e.apoapsisRadius() - PALE.radius),
                            String.format("%.4f", e.eccentricity()));
                    if (e.eccentricity() < 1 && pe >= ORBIT_OK_PE_M) {
                        endLeg(ship, true, String.format("orbit Pe %.0f m, Ap %.0f m", pe, e.apoapsisRadius() - PALE.radius));
                        beginLeg(ship, 3, "Transfer back to Verdance");
                        next(Phase.ESC_PLAN);
                    } else {
                        return failLeg(ship, "the Pale orbit is too low: Pe " + fmt(pe) + " m");
                    }
                }
            }
            case ESC_PLAN -> {
                throttle = ShipInputPayload.THROTTLE_CUT;
                if (!planEscape(mc, p)) {
                    return failLeg(ship, "no escape burn found within " + PLAN_LAPS + " laps");
                }
                next(Phase.ESC_WAIT);
            }
            case ESC_WAIT -> {
                double ttb = plan.burnUt() - estUt(p);
                double half = Maneuvers.burnSeconds(plan.deltaV(), ship.massKg(), 440, ship.maxThrustN(0)) / 2;
                flags |= warpFlag(p, ttb - half);
                if (ttb - half < 75) {
                    flags |= sasPress(ship, SasMode.PROGRADE);
                }
                if (phaseTicks % 100 == 0) {
                    LOG.info("[test driver] return: {} s to the escape burn, warp x{}", fmt(ttb), fmt(p.warp()));
                }
                double align = inOrbit(p) ? steer(ship, p, +1)[2] : 99;
                if (ttb - half <= 0 && p.warp() <= 1 && align < ALIGN_DEG) {
                    burnMass = ship.massKg();
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                    LOG.info("[test driver] return: escape burn starts, nose {} deg off prograde, plan dv {} m/s", fmt(align), fmt(plan.deltaV()));
                    next(Phase.ESC_BURN);
                } else if (ttb - half < -30) {
                    return failLeg(ship, "missed the burn window by " + fmt(-ttb) + " s");
                }
            }
            case ESC_BURN -> {
                throttle = ShipInputPayload.THROTTLE_FULL;
                flags |= sasPress(ship, SasMode.PROGRADE);
                double done = VE_VAC_MPS * Math.log(burnMass / ship.massKg());
                if (phaseTicks == 10) {
                    snap(mc, "m09-04-escape-burn.png");
                }
                if (done >= plan.deltaV() - BURN_CUT_MARGIN_MPS || phaseTicks > 30 * 20) {
                    throttle = ShipInputPayload.THROTTLE_CUT;
                    LOG.info("[test driver] return: escape burn off after {} m/s (plan {})", fmt(done), fmt(plan.deltaV()));
                    verifyTries = 0;
                    next(Phase.ESC_VERIFY);
                }
            }
            case ESC_VERIFY -> {
                throttle = phaseTicks < 8 ? ShipInputPayload.THROTTLE_CUT : ShipInputPayload.THROTTLE_HOLD;
                flags |= sasPress(ship, SasMode.PROGRADE);
                // Wait for a payload from after the cut (they come every 5 ticks), then check where the orbit leads.
                if (verifyTries >= 0 && phaseTicks >= 14 && ClientState.clientTicks - ClientState.orbitReceivedTick <= 2) {
                    double pe = EscapeSearch.parentPeriapsisAltM(PALE, stateOf(p), p.ut());
                    LOG.info("[test driver] return: after the burn the Verdance periapsis is {} m (wanted {})", fmt(pe), fmt(RETURN_PE_M));
                    if (!Double.isNaN(pe) && pe >= PE_OK_MIN_M && pe <= PE_OK_MAX_M) {
                        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                        MapClient.openMap();
                        shotName = "m09-05-map-return-trajectory.png";
                        shotAt = phaseTicks + 40;
                        verifyTries = -1;
                    } else if (++verifyTries > 8) {
                        return failLeg(ship, "the escape orbit misses the atmosphere: Pe " + fmt(pe) + " m");
                    } else {
                        // Not far enough: a short extra push.
                        throttle = ShipInputPayload.THROTTLE_FULL;
                        phaseTicks = 8;
                    }
                }
                if (verifyTries == -1 && shotName == null) {
                    mc.gui.setScreen(null);
                    next(Phase.ESC_COAST);
                }
            }
            case ESC_COAST -> {
                throttle = phaseTicks < 10 ? ShipInputPayload.THROTTLE_CUT : ShipInputPayload.THROTTLE_HOLD;
                if (!coastHome(mc, ship, p)) {
                    return failLeg(ship, "no atmosphere entry predicted on the way home");
                }
                double eventS = (BodyId.byOrdinal(p.body()) == BodyId.PALE ? soiExitUt : atmosphereUt - ENTRY_ALIGN_S) - estUt(p);
                flags |= warpFlag(p, eventS);
                if (BodyId.byOrdinal(p.body()) == BodyId.VERDANCE) {
                    double toAtm = atmosphereUt - estUt(p);
                    if (toAtm < ENTRY_ALIGN_S + 30) {
                        next(Phase.ENTRY_ALIGN);
                        endLeg(ship, true, "left Pale, Verdance periapsis " + fmt(verdancePeriapsisM(p)) + " m");
                        beginLeg(ship, 4, "Verdance re-entry with chutes");
                    }
                }
            }
            case ENTRY_ALIGN -> {
                throttle = ShipInputPayload.THROTTLE_HOLD;
                flags |= warpFlag(p, atmosphereUt - ENTRY_ALIGN_S - estUt(p));
                if (atmosphereUt - estUt(p) < ENTRY_ALIGN_S + 30) {
                    flags |= sasPress(ship, SasMode.RETROGRADE);
                }
                if (phaseTicks % 100 == 0) {
                    LOG.info("[test driver] return: {} s to the atmosphere, alt {} m, speed {} m/s, warp x{}, SAS {}",
                            fmt(atmosphereUt - estUt(p)), fmt(lastAlt), fmt(lastSpeed), fmt(p.warp()), ship.getSasMode());
                }
                if (lastAlt <= ENTRY_BURN_ALT_M && inOrbit(p)) {
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                    LOG.info("[test driver] return: entry burn starts at {} m, {} m/s, nose {} deg off retrograde", fmt(lastAlt),
                            fmt(lastSpeed), fmt(steer(ship, p, -1)[2]));
                    next(Phase.ENTRY_BURN);
                }
            }
            case ENTRY_BURN -> {
                throttle = ShipInputPayload.THROTTLE_FULL;
                flags |= sasPress(ship, SasMode.RETROGRADE);
                if (phaseTicks == 60) {
                    snap(mc, "m09-06-reentry-burn.png");
                }
                if (phaseTicks % 20 == 0) {
                    LOG.info("[test driver] return entry: alt {} m, speed {} m/s, fuel {} kg", fmt(lastAlt), fmt(lastSpeed), fmt(ship.fuelKg()));
                }
                if (lastSpeed <= CHUTE_CUT_MPS) {
                    throttle = ShipInputPayload.THROTTLE_CUT;
                    flags |= ShipInputPayload.FLAG_DEPLOY_CHUTES;
                    chuteDeployAlt = lastAlt;
                    chuteSentTick = ticks;
                    LOG.info("[test driver] return: burn off and chutes sent at {} m, {} m/s", fmt(lastAlt), fmt(lastSpeed));
                    next(Phase.CHUTES);
                } else if (ship.fuelKg() < 20) {
                    return failLeg(ship, "out of fuel at " + fmt(lastSpeed) + " m/s, " + fmt(lastAlt) + " m");
                } else if (lastAlt < CHUTE_ALT_MIN_M || phaseTicks > ENTRY_CAP_TICKS) {
                    return failLeg(ship, "still at " + fmt(lastSpeed) + " m/s at " + fmt(lastAlt) + " m");
                }
            }
            case CHUTES -> {
                throttle = ShipInputPayload.THROTTLE_CUT;
                flags |= sasPress(ship, SasMode.RETROGRADE);
                int state = ship.getChuteState();
                if (state == DescentGear.TORN) {
                    return failLeg(ship, "the chutes were shredded at " + fmt(chuteDeployAlt) + " m");
                }
                if (state == DescentGear.STOWED && ticks - chuteSentTick >= 10) {
                    flags |= ShipInputPayload.FLAG_DEPLOY_CHUTES;
                    chuteSentTick = ticks;
                }
                if (state >= DescentGear.INFLATE_TICKS) {
                    LOG.info("[test driver] return: chutes fully open at {} m, {} m/s (sent at {} m)", fmt(lastAlt), fmt(lastSpeed), fmt(chuteDeployAlt));
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                    shotName = "m09-07-chutes-open-above-2km.png";
                    shotAt = phaseTicks + 10;
                    boolean above = chuteDeployAlt >= CHUTE_ALT_MIN_M;
                    endLeg(ship, above, String.format("chutes sent at %.0f m and %.0f m/s", chuteDeployAlt, lastSpeed));
                    beginLeg(ship, 5, "Chute descent and landing");
                    next(Phase.DESCENT);
                } else if (phaseTicks > 10 * 20) {
                    return failLeg(ship, "the chutes never opened");
                }
            }
            case DESCENT -> {
                throttle = ShipInputPayload.THROTTLE_CUT;
                flags |= sasPress(ship, SasMode.RETROGRADE);
                if (phaseTicks % 100 == 0) {
                    LOG.info("[test driver] return descent: alt {} m, speed {} m/s, vertical {} m/s, regime {}, chute {}", fmt(lastAlt),
                            fmt(lastSpeed), fmt(lastVel.y), inOrbit(p) ? "orbit" : "surface", ship.getChuteState());
                }
                if (!shotA && !inOrbit(p)) {
                    shotA = true;
                    shotName = "m09-08-descent-below-2km.png";
                    shotAt = phaseTicks + 10;
                }
                if (!ship.isLanded()) {
                    lastAirVy = lastVel.y;
                    vyRing[vyRingAt++ % vyRing.length] = lastVel.y;
                }
                crossingFrames(mc, ship, p);
                double radar = radar(mc, ship);
                if (!shotB && !inOrbit(p) && radar < 120) {
                    shotB = true;
                    snap(mc, "m09-09-final-approach.png");
                }
                if (!inOrbit(p) && ship.isLanded()) {
                    touchdownVertical = -vyRing[vyRingAt % vyRing.length];
                    int spawnX = mc.level.getRespawnData().globalPos().pos().getX();
                    int spawnZ = mc.level.getRespawnData().globalPos().pos().getZ();
                    // What is under the ship: the first block below it that is not air, found by scanning the loaded blocks
                    // (the client keeps no ocean-floor heightmap). A fluid there means the ship came down in water.
                    net.minecraft.core.BlockPos.MutableBlockPos probe = new net.minecraft.core.BlockPos.MutableBlockPos(
                            Mth.floor(ship.getX()), Mth.floor(ship.getY()), Mth.floor(ship.getZ()));
                    for (int i = 0; i < 60 && mc.level.getBlockState(probe).isAir(); i++) {
                        probe.move(0, -1, 0);
                    }
                    boolean wet = !mc.level.getFluidState(probe).isEmpty();
                    touchdownDry = !wet;
                    touchdownSite = String.format("x %.0f z %.0f, %.0f m from the world spawn, on %s, %s", ship.getX(), ship.getZ(),
                            Math.hypot(ship.getX() - spawnX, ship.getZ() - spawnZ), mc.level.getBlockState(probe).getBlock(),
                            wet ? "IN WATER (landed on the sea floor)" : "on dry land");
                    LOG.info("[test driver] return: touchdown at {} m/s vertical, {} m/s horizontal; legs {} chutes {}; {}", fmt(touchdownVertical),
                            fmt(Math.hypot(prevVel.x, prevVel.z)), ship.landingLegCount(), ship.parachuteCount(), touchdownSite);
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                    next(Phase.LANDED);
                }
            }
            case LANDED -> {
                throttle = ShipInputPayload.THROTTLE_CUT;
                if (phaseTicks == 30) {
                    snap(mc, "m09-10-landed-third-person.png");
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                }
                if (phaseTicks == 60) {
                    snap(mc, "m09-11-landed-first-person.png");
                }
                if (phaseTicks >= 80) {
                    endLeg(ship, true, String.format("landed on %d legs, ship intact, %.1f m/s vertical at touchdown, %s",
                            ship.landingLegCount(), touchdownVertical, touchdownSite));
                    return finish();
                }
            }
            case DONE -> {
                return false;
            }
        }
        send(ship, pitch, yaw, throttle, flags);
        return true;
    }

    // ---- start, stage starts ---------------------------------------------------------------------------------------

    /** Leaves START: the full run goes straight to the Pale leg, a stage start sends its command and waits for it. */
    private void startRun(Minecraft mc) {
        switch (STAGE) {
            case "ascent" -> stageCommand(mc, "apoapsis scenario lander pale 60");
            case "escape" -> stageCommand(mc, "apoapsis seamless orbit pale 12500 20 0 0 lander");
            case "entry" -> stageCommand(mc, "apoapsis seamless dive 12000 906 14 lander");
            default -> {
                next(Phase.PALE_LEG);
                beginLegLater = true;
            }
        }
    }

    /** True until the first tick that has a ship to start leg 1 with. */
    private boolean beginLegLater;
    private boolean fpSet;
    private boolean mapShot;
    private boolean crossingDone;
    private int mapShotAt;
    private int mapCloseAt;
    private int crossingShots;
    private int crossingShipId = -1;
    private int crossingBurstUntil;
    private int lastCrossingTick = -1000;
    private double touchdownVertical;
    /** Vertical speed on the last tick the ship was not landed: the speed it hit the ground at (the first landed tick reads about zero). */
    private double lastAirVy;
    /** The last 10 airborne vertical speeds: the oldest is the speed half a second before contact (the velocity syncs ahead of the landed flag). */
    private final double[] vyRing = new double[10];
    private int vyRingAt;
    private String touchdownSite = "";
    private boolean touchdownDry = true;
    /** Times the base class has told this script the pilot is out of a ship while a stage start moves it (it asks every two seconds). */
    private int stageNullWaits;

    private void stageCommand(Minecraft mc, String command) {
        stageLabel = STAGE;
        LOG.info("[test driver] return STAGE START ({}): sending \"{}\"; the ship is placed by that command, not flown there", STAGE, command);
        mc.player.connection.sendCommand(command);
        next(Phase.STAGE_WAIT);
    }

    private void stageWait(Minecraft mc, ShipEntity ship, OrbitStatePayload p) {
        BodyId body = BodyId.byOrdinal(p.body());
        boolean ready = switch (STAGE) {
            case "ascent" -> body == BodyId.PALE && !inOrbit(p) && ship.isLanded() && phaseTicks > 60;
            case "escape" -> body == BodyId.PALE && inOrbit(p) && lastAlt > 12_000 && phaseTicks > 60;
            case "entry" -> body == BodyId.VERDANCE && inOrbit(p) && lastAlt < 12_500 && ship.landingLegCount() > 0 && phaseTicks > 20;
            default -> true;
        };
        if (!ready) {
            if (phaseTicks > STAGE_WAIT_TICKS) {
                abort("the stage start " + STAGE + " never took effect");
            }
            return;
        }
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        switch (STAGE) {
            case "ascent" -> {
                beginLeg(ship, 2, "Pale ascent (stage start)");
                next(Phase.ASC_SETTLE);
            }
            case "escape" -> {
                beginLeg(ship, 3, "Transfer back to Verdance (stage start)");
                next(Phase.ESC_PLAN);
            }
            default -> {
                beginLeg(ship, 4, "Verdance re-entry with chutes (stage start)");
                atmosphereUt = p.ut() + 1;
                next(Phase.ENTRY_ALIGN);
            }
        }
    }

    // ---- legs and bookkeeping --------------------------------------------------------------------------------------

    private void beginLeg(ShipEntity ship, int number, String name) {
        legNumber = number;
        legName = name;
        legMass = ship.massKg();
        legFuel = ship.fuelKg();
        legUt = lastUt;
        legStartTick = ticks;
        LOG.info("[test driver] return LEG {} begins: {} (mass {} kg, fuel {} kg){}", number, name, fmt(legMass), fmt(legFuel),
                stageLabel.isEmpty() ? "" : " [stage start]");
    }

    private void endLeg(ShipEntity ship, boolean pass, String note) {
        double dv = VE_VAC_MPS * Math.log(legMass / Math.max(ship.massKg(), 1));
        Leg leg = new Leg(legNumber, legName, pass, dv, legFuel - ship.fuelKg(), lastUt - legUt, (ticks - legStartTick) * DT, note);
        legs.add(leg);
        LOG.info("[test driver] return LEG {} {}: {} | dv {} m/s (vacuum equivalent), fuel {} kg, game time {} s, client time {} s | {}",
                leg.number(), pass ? "PASS" : "FAIL", leg.name(), fmt(leg.dvMps()), fmt(leg.fuelKg()), fmt(leg.gameS()), fmt(leg.realS()), note);
    }

    private boolean failLeg(ShipEntity ship, String why) {
        failed = true;
        endLeg(ship, false, why);
        return finish();
    }

    private boolean abort(String why) {
        failed = true;
        LOG.warn("[test driver] return: stopping, {}", why);
        return finish();
    }

    /** The pilot left the ship: a crash during the descent ends the run, anything earlier is a loss. */
    private boolean shipLost(Minecraft mc) {
        if (phase == Phase.PALE_LEG) {
            if (!pale.tick(mc, null)) {
                failed = true;
                LOG.warn("[test driver] return LEG 1 FAIL: the Pale landing leg lost the ship in {}", pale.phaseName());
                return finish();
            }
            return true;
        }
        if (phase == Phase.STAGE_WAIT && ++stageNullWaits <= 8) {
            return true; // the stage command is moving the pilot into a new level; the base class asks again in two seconds
        }
        failed = true;
        LOG.warn("[test driver] return LEG {} FAIL: {}: the ship was lost in {} at {} m, {} m/s", legNumber, legName, phase,
                fmt(lastAlt), fmt(lastSpeed));
        legs.add(new Leg(legNumber, legName, false, 0, 0, lastUt - legUt, (ticks - legStartTick) * DT, "ship lost in " + phase));
        return finish();
    }

    private boolean finish() {
        double dv = 0;
        double fuel = 0;
        for (Leg leg : legs) {
            LOG.info("[test driver] return SUMMARY leg {} {} {}: dv {} m/s, fuel {} kg, game {} s, client {} s | {}", leg.number(),
                    leg.pass() ? "PASS" : "FAIL", leg.name(), fmt(leg.dvMps()), fmt(leg.fuelKg()), fmt(leg.gameS()), fmt(leg.realS()), leg.note());
            dv += leg.dvMps();
            fuel += leg.fuelKg();
        }
        LOG.info("[test driver] return OUTCOME: {} | total dv {} m/s, fuel {} kg over {} legs, {} client seconds", failed ? "FAILED" : "ALL LEGS PASSED",
                fmt(dv), fmt(fuel), legs.size(), fmt(ticks * DT));
        csv.close();
        phase = Phase.DONE;
        return false;
    }

    private void next(Phase to) {
        LOG.info("[test driver] return {} -> {} at {} m, {} m/s, ut {}", phase, to, fmt(lastAlt), fmt(lastSpeed), fmt(lastUt));
        phase = to;
        phaseTicks = 0;
        shotA = false;
        shotB = false;
    }

    private void snap(Minecraft mc, String name) {
        shot(mc, name);
        LOG.info("[test driver] return: shot {} at {} m, {} m/s", name, fmt(lastAlt), fmt(lastSpeed));
    }

    private void sample(ShipEntity ship, OrbitStatePayload p) {
        if (ticks % SAMPLE_TICKS != 0) {
            return;
        }
        String name = phase == Phase.PALE_LEG ? "L1-" + pale.phaseName() : "L" + legNumber + "-" + phase.name();
        csv.row(p.ut(), ticks * DT, BodyId.byOrdinal(p.body()).id, inOrbit(p) ? "orbit" : "surface", lastAlt, lastSpeed,
                ship.fuelKg(), ship.getThrottle(), name);
    }

    // ---- orbit helpers ---------------------------------------------------------------------------------------------

    private static CelestialBody bodyOf(OrbitStatePayload p) {
        return FlightDimensions.system(BodyId.byOrdinal(p.body()));
    }

    private static Elements elements(OrbitStatePayload p) {
        return Elements.of(bodyOf(p).mu, stateOf(p));
    }

    /**
     * Apoapsis height above Pale's radius, m, of the Kepler orbit through a surface state: height {@code altM}, vertical
     * speed {@code vr}, horizontal speed {@code vt}. Infinite when the state is unbound.
     */
    private static double surfaceApoapsisM(double altM, double vr, double vt) {
        double r = PALE.radius + Math.max(altM, 0);
        double energy = 0.5 * (vr * vr + vt * vt) - PALE.mu / r;
        if (energy >= 0) {
            return Double.POSITIVE_INFINITY;
        }
        double h = r * vt;
        double a = -PALE.mu / (2 * energy);
        double ecc = Math.sqrt(Math.max(0, 1 + 2 * energy * h * h / (PALE.mu * PALE.mu)));
        return a * (1 + ecc) - PALE.radius;
    }

    /** Speed along the radius vector, m/s (positive going up). */
    private static double radialSpeed(OrbitStatePayload p) {
        double r = Math.sqrt(p.rx() * p.rx() + p.ry() * p.ry() + p.rz() * p.rz());
        return (p.rx() * p.vx() + p.ry() * p.vy() + p.rz() * p.vz()) / r;
    }

    /** Universal time now: the last payload's, advanced by the client ticks since it arrived at its warp. */
    private static double estUt(OrbitStatePayload p) {
        long age = Math.max(0, ClientState.clientTicks - ClientState.orbitReceivedTick);
        return p.ut() + age * DT * Math.max(1.0, p.warp());
    }

    /** Periapsis height of the Verdance conic the ship is on, m. */
    private static double verdancePeriapsisM(OrbitStatePayload p) {
        return elements(p).periapsisRadius() - VERDANCE.radius;
    }

    /** SAS toggle flag for this tick when the ship's mode is not {@code target}: a press every 8 ticks, since the mode syncs late. */
    private byte sasPress(ShipEntity ship, SasMode target) {
        return ship.getSasMode() != target && phaseTicks % 8 == 0 ? ShipInputPayload.FLAG_TOGGLE_SAS : 0;
    }

    /**
     * Warp key for this tick: one step toward the highest level that still leaves time to stop before the event, which is
     * {@code secondsToEvent} (game seconds) away. A press every 10 ticks.
     */
    private byte warpFlag(OrbitStatePayload p, double secondsToEvent) {
        int want = 1;
        for (int level : WARP_LEVELS) {
            if (secondsToEvent > WARP_BASE_S + WARP_PER_LEVEL_S * level) {
                want = level;
            }
        }
        if (phaseTicks % 10 != 0) {
            return 0;
        }
        if (p.warp() < want - 0.5) {
            return ShipInputPayload.FLAG_WARP_UP;
        }
        if (p.warp() > want + 0.5) {
            return ShipInputPayload.FLAG_WARP_DOWN;
        }
        return 0;
    }

    // ---- leg 3 planning and coasting -------------------------------------------------------------------------------

    /** Finds the escape burn with {@link EscapeSearch}; logs the plan. False if there is none. */
    private boolean planEscape(Minecraft mc, OrbitStatePayload p) {
        StateVector s = stateOf(p);
        double ut = p.ut();
        Elements e = Elements.of(PALE.mu, s);
        double synodic = 1 / (1 / e.period() - 1 / PALE.orbitPeriod());
        int spawnX = mc.level.getRespawnData().globalPos().pos().getX();
        double landX = spawnX + LAND_OFFSET_M;
        double lonTarget = (landX - ENTRY_RANGE_M) / VERDANCE.radius;
        long t0 = System.nanoTime();
        ReturnLandingSites sites = ReturnLandingSites.scan(mc.getSingleplayerServer(), VERDANCE);
        if (sites != null && !sites.isEmpty()) {
            LOG.info("[test driver] return plan: {} dry landing sites on Verdance's equator (sea level {} m), scanned in {} ms",
                    sites.count(), sites.seaLevel(), fmt((System.nanoTime() - t0) / 1e6));
            plan = EscapeSearch.find(PALE, s, ut, ut + 150, ut + 150 + PLAN_LAPS * synodic, PLAN_STEP_S, RETURN_PE_M,
                    sites.penalty(ENTRY_RANGE_M), MAX_ESCAPE_DV_MPS);
            if (plan != null) {
                double aimX = plan.entryLonRad() * VERDANCE.radius + ENTRY_RANGE_M;
                LOG.info("[test driver] return plan: aims at x {} m, nearest dry site centre x {} m", fmt(aimX), fmt(sites.nearest(aimX)));
            }
        } else {
            LOG.warn("[test driver] return plan: no dry landing site found (scan {}), aiming at x {} m near the world spawn",
                    sites == null ? "unavailable" : "empty", fmt(landX));
            plan = EscapeSearch.find(PALE, s, ut, ut + 150, ut + 150 + PLAN_LAPS * synodic, PLAN_STEP_S, RETURN_PE_M, lonTarget,
                    MAX_ESCAPE_DV_MPS);
        }
        double ms = (System.nanoTime() - t0) / 1e6;
        if (plan == null) {
            return false;
        }
        double lonErr = Math.toDegrees(Math.IEEEremainder(plan.entryLonRad() - lonTarget, 2 * Math.PI));
        LOG.info("[test driver] return plan ({} ms): burn in {} s (ut {}), prograde {} m/s, Verdance Pe {} m, atmosphere at ut {}, entry lon {} deg "
                        + "(target {} deg, off {} deg), landing about x {} m (spawn x {}), synodic period {} s",
                fmt(ms), fmt(plan.burnUt() - ut), fmt(plan.burnUt()), fmt(plan.deltaV()), fmt(plan.periapsisAltM()), fmt(plan.entryUt()),
                fmt(Math.toDegrees(plan.entryLonRad())), fmt(Math.toDegrees(lonTarget)), fmt(lonErr),
                fmt(plan.entryLonRad() * VERDANCE.radius + ENTRY_RANGE_M), spawnX, fmt(synodic));
        return true;
    }

    /**
     * Keeps the predicted events fresh once a second (the SOI exit and the atmosphere) and returns false when the orbit
     * will not reach the atmosphere.
     */
    private boolean coastHome(Minecraft mc, ShipEntity ship, OrbitStatePayload p) {
        if (ticks - lastPredictTick >= 20) {
            lastPredictTick = ticks;
            List<Patch> patches = TrajectoryPredictor.predict(bodyOf(p), stateOf(p), p.ut(), 24 * 3600, 3);
            soiExitUt = Double.NaN;
            atmosphereUt = Double.NaN;
            for (Patch patch : patches) {
                if (patch.body() == PALE && patch.end() == Patch.EndReason.ESCAPE) {
                    soiExitUt = patch.endUt();
                }
                if (patch.body() == VERDANCE && (patch.end() == Patch.EndReason.ATMOSPHERE || patch.end() == Patch.EndReason.IMPACT)) {
                    atmosphereUt = patch.endUt();
                }
            }
            if (phaseTicks % 200 < 20) {
                LOG.info("[test driver] return coast: body {}, alt {} km, SOI exit in {} s, atmosphere in {} s, warp x{}",
                        BodyId.byOrdinal(p.body()).id, fmt(lastAlt / 1000), fmt(soiExitUt - estUt(p)), fmt(atmosphereUt - estUt(p)), fmt(p.warp()));
            }
        }
        if (Double.isNaN(atmosphereUt)) {
            return phaseTicks < 100;
        }
        if (BodyId.byOrdinal(p.body()) == BodyId.PALE && Double.isNaN(soiExitUt)) {
            return phaseTicks < 100;
        }
        return true;
    }

    // ---- the 2 km crossing, going down -------------------------------------------------------------------------------

    /**
     * First-person frames across the orbit-to-surface change at 2 km, going down: from 2,600 m the camera is first
     * person, and between 2,080 and 1,930 m a frame is saved every 15 ticks, each with a log line saying which regime the
     * payload reports, whether the client still holds an orbit state, which screen is open and the chute state. After
     * the crossing the 3D map is opened for one frame, to see that it hands over to the surface state.
     */
    private void crossingFrames(Minecraft mc, ShipEntity ship, OrbitStatePayload p) {
        if (!fpSet && lastAlt < 2600) {
            fpSet = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if (!fpSet || crossingDone) {
            return;
        }
        // A new ship entity is the change itself: take a quick burst of frames from the tick it appears.
        if (crossingShipId != -1 && ship.getId() != crossingShipId) {
            crossingBurstUntil = ticks + 24;
        }
        crossingShipId = ship.getId();
        boolean burst = ticks < crossingBurstUntil && ticks - lastCrossingTick >= 4;
        boolean slow = lastAlt <= 2150 && lastAlt >= 1900 && ticks - lastCrossingTick >= 20;
        if ((burst || slow) && crossingShots < 24) {
            lastCrossingTick = ticks;
            String regime = inOrbit(p) ? "orbit" : "surface";
            snap(mc, String.format("m09-crossing-%02d-%s.png", crossingShots++, regime));
            LOG.info("[test driver] return crossing {}: payload regime {} body {}, client orbit state {}, screen {}, chute {}, ship id {} "
                            + "(tick {}), dimension {}, light sky {} block {} at the ship, alt {} m, speed {} m/s", crossingShots - 1, regime, BodyId.byOrdinal(p.body()).id,
                    ClientState.orbit != null, mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getSimpleName(),
                    ship.getChuteState(), ship.getId(), ship.tickCount, mc.level.dimension().identifier(),
                    mc.level.getBrightness(net.minecraft.world.level.LightLayer.SKY, ship.blockPosition()),
                    mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, ship.blockPosition()), fmt(lastAlt), fmt(lastSpeed));
        }
        // Once the change is behind, open the map for a frame, then close it and go back to the chase camera.
        if (!mapShot && !inOrbit(p) && lastAlt < 1850 && ticks >= crossingBurstUntil) {
            mapShot = true;
            MapClient.openMap();
            mapShotAt = phaseTicks + 30;
            mapCloseAt = phaseTicks + 45;
            LOG.info("[test driver] return crossing: map opened at {} m, client orbit state {}, screen {}", fmt(lastAlt),
                    ClientState.orbit != null, mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getSimpleName());
        }
        if (mapShotAt > 0 && phaseTicks >= mapShotAt) {
            mapShotAt = 0;
            snap(mc, "m09-12-map-after-crossing.png");
            LOG.info("[test driver] return crossing: map frame at {} m, screen {}", fmt(lastAlt),
                    mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getSimpleName());
        }
        if (mapCloseAt > 0 && phaseTicks >= mapCloseAt) {
            mapCloseAt = 0;
            crossingDone = true;
            mc.gui.setScreen(null);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }

    // ---- terrain ---------------------------------------------------------------------------------------------------

    /** Metres from the lowest point of the ship to the terrain under it; large when the chunk is not loaded. */
    private static double radar(Minecraft mc, ShipEntity ship) {
        int bx = Mth.floor(ship.getX());
        int bz = Mth.floor(ship.getZ());
        if (!mc.level.hasChunkAt(bx, bz)) {
            return 1e9;
        }
        double ground = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
        return Math.max(0, ship.getY() - ship.lowestPointBelowCentreM() - ground);
    }
}
