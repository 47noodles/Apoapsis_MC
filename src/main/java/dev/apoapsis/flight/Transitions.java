package dev.apoapsis.flight;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.ages.Firsts;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Kepler;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.Vec3d;
import dev.apoapsis.ownership.ShipOwnership;
import dev.apoapsis.ship.ShipEntity;
import dev.apoapsis.ship.ShipStructure;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Moves a ship between its surface dimension and the orbit dimension. The ship is replaced by a new entity in the
 * target level, and its pilot is teleported in and seated on the next tick.
 */
public final class Transitions {
    /** Ticks to wait for a pilot to reach the target level before the seat is dropped. */
    private static final int SEAT_TIMEOUT_TICKS = 40;

    /** Ticks between landing predictions and ticket refreshes, one second. The ENDER_PEARL ticket lasts 40 ticks. */
    static final int PREFETCH_INTERVAL_TICKS = 20;
    /** Game seconds ahead of a predicted crossing of the transition altitude at which landing chunks are requested. */
    static final double PREFETCH_LEAD_S = 30.0;
    /** A prefetch keeps its previous column while the new prediction lies within this many chunks of it. */
    static final int PREFETCH_KEEP_CHUNKS = 3;
    /** A blocking landing-chunk load slower than this is logged, so a play-test shows whether it fires. */
    private static final long BLOCKING_LOAD_WARN_NS = 50_000_000L;
    /** Game seconds per step of the crossing search. */
    private static final double PREFETCH_STEP_S = 0.5;
    /** Bisection steps that refine a crossing inside one search step (0.5 s / 2^8, about 2 ms). */
    private static final int PREFETCH_REFINE_STEPS = 8;
    /** Chunk radius of the landing ticket around the predicted column. */
    static final int LANDING_RADIUS = 2;
    /** Ticks a descent may wait in orbit for its landing chunk before it loads the chunk and descends at once. */
    static final int POSTPONE_LIMIT_TICKS = 20;
    /** Ticks ahead at which a waiting descent requests the column it will be over, so the chunk can generate. */
    static final int POSTPONE_LEAD_TICKS = 10;

    private static final Map<UUID, PendingSeat> PENDING = new HashMap<>();
    /** Ships whose descent is waiting for its landing chunk, with the ticks waited so far. */
    private static final Map<UUID, Integer> WAITING = new HashMap<>();
    /** The landing chunk each descending ship last asked for, so a small change in prediction does not retarget it. */
    private static final Map<UUID, ChunkPos> PREFETCH_COLUMN = new HashMap<>();

    private Transitions() {}

    private record PendingSeat(ResourceKey<Level> dimension, UUID shipId, int ticksLeft) {}

    /** Drops a ship's waiting and prefetch entries when it leaves the orbit level. */
    static void forget(UUID shipId) {
        WAITING.remove(shipId);
        PREFETCH_COLUMN.remove(shipId);
    }

    /** Surface to orbit: the ship goes up at its current inertial state, at a free orbit slot. */
    static void up(ShipEntity ship, FlightState old, ServerLevel surface) {
        MinecraftServer server = surface.getServer();
        ServerLevel orbit = server.getLevel(FlightDimensions.ORBIT);
        if (orbit == null) {
            return;
        }
        BodyId body = old.body;
        CelestialBody sys = FlightDimensions.system(body);
        Vec3 pos = ship.position();
        Frames.Tangent t = Frames.tangentAt(pos, sys.radius);
        Vec3d r = Frames.toInertialPosition(pos, sys.radius, FlightDimensions.seaLevel(surface.dimension()), t);
        Vec3d v = Frames.toInertialVelocity(ship.getVelocity(), t);
        Quaternionf q = Frames.surfaceToOrbitOrientation(ship.getOrientation(), t);
        int slot = OrbitSlots.claim(server, orbit);
        FlightState state = FlightState.inOrbit(body, new StateVector(r, v), UniverseTime.get(server).ut(), slot);
        state.firstFlight = old.firstFlight;
        state.transitionTick = server.getTickCount();

        FlightMessages.toPilot(ship, Component.translatable("apoapsis.flight.enter_orbit",
                FlightMessages.bodyName(body)));
        FlightWarp.clear();
        loadChunk(orbit, FlightDimensions.slotPosition(slot));
        moveRecordingSuborbital(ship, () -> move(ship, orbit, FlightDimensions.slotPosition(slot), q, state, Vec3.ZERO));
    }

    /**
     * Runs {@code move}, which unseats the pilot, then records SUBORBITAL for whoever was seated before it. Public so a
     * GameTest can drive it: the orbit dimension is not loaded in the GameTest world, so {@link #up} cannot run there.
     */
    public static void moveRecordingSuborbital(ShipEntity ship, Runnable move) {
        Entity pilot = ship.getPilot();
        move.run();
        Firsts.suborbital(pilot);
    }

    /**
     * Orbit to surface: the ship comes down at the sub-point of its inertial position, on the body's surface. While the
     * landing chunk is still generating, the ship stays in orbit for up to {@link #POSTPONE_LIMIT_TICKS} ticks, each
     * time only if the next tick keeps it above the surface; then the chunk is loaded and the descent goes ahead.
     */
    static void down(ShipEntity ship, FlightState old, ServerLevel orbit) {
        MinecraftServer server = orbit.getServer();
        BodyId body = old.body;
        ServerLevel surface = server.getLevel(FlightDimensions.surfaceDimension(body));
        if (surface == null) {
            return;
        }
        CelestialBody sys = FlightDimensions.system(body);
        Vec3d r = old.r();
        Frames.Tangent t = Frames.tangentAt(r);
        Vec3 pos = Frames.toSurfacePosition(r, sys.radius, FlightDimensions.seaLevel(body), t);
        if (!isLoaded(surface, pos) && canWait(server, ship.getUUID(), old, sys)) {
            // The ship moves over the ground while it waits, so the column it will be over in a few ticks is requested.
            requestLanding(surface, pos);
            requestLanding(surface, surfacePointAfter(old, sys, body,
                    POSTPONE_LEAD_TICKS * FlightEvents.TICK_SECONDS * FlightWarp.factor(server)));
            return;
        }
        WAITING.remove(ship.getUUID());
        Vec3 vel = Frames.toSurfaceVelocity(old.v(), t);
        Quaternionf q = Frames.orbitToSurfaceOrientation(ship.getOrientation(), t);
        FlightState state = new FlightState();
        state.regime = FlightRegime.SURFACE;
        state.body = body;
        state.firstFlight = old.firstFlight;
        state.transitionTick = server.getTickCount();

        FlightMessages.toPilot(ship, Component.translatable(body == BodyId.VERDANCE
                ? "apoapsis.flight.enter_atmosphere"
                : "apoapsis.flight.leave_orbit"));
        FlightWarp.clear();
        OrbitSlots.release(server, old.slot);
        loadChunk(surface, pos);
        pos = aboveTerrain(surface, pos);
        move(ship, surface, pos, q, state, vel);
    }

    /** Replaces {@code old} with a new ship in {@code target}, carrying its state and pilot. */
    private static ShipEntity move(ShipEntity old, ServerLevel target, Vec3 pos, Quaternionf orientation,
                                   FlightState state, Vec3 velocity) {
        ServerPlayer pilot = old.getPilot() instanceof ServerPlayer p ? p : null;
        ShipStructure structure = old.getStructure().copy();
        old.setTransferring(true);
        if (pilot != null) {
            pilot.stopRiding();
        }
        ShipEntity fresh = ShipEntity.spawn(target, structure, pos, orientation);
        fresh.setThrottle(old.getThrottle());
        fresh.setSasMode(old.getSasMode());
        ShipOwnership.copy(old, fresh);
        fresh.setVelocity(velocity);
        fresh.setAngularVelocity(old.getAngularVelocity());
        fresh.setData(FlightAttachments.FLIGHT.get(), state);
        old.discard();
        if (pilot != null) {
            pilot.teleport(new TeleportTransition(target, pos, Vec3.ZERO, pilot.getYRot(), pilot.getXRot(),
                    TeleportTransition.DO_NOTHING));
            PENDING.put(pilot.getUUID(), new PendingSeat(target.dimension(), fresh.getUUID(), SEAT_TIMEOUT_TICKS));
        }
        old.setTransferring(false);
        return fresh;
    }

    /** Seats pilots who have arrived in the target level in their new ship. Called once per server tick. */
    static void tickPendingSeats(MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, PendingSeat>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, PendingSeat> entry = it.next();
            PendingSeat seat = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                it.remove();
                continue;
            }
            ServerLevel level = server.getLevel(seat.dimension());
            Entity ship = level == null ? null : level.getEntity(seat.shipId());
            if (ship instanceof ShipEntity s && s.isAlive() && player.level().dimension().equals(seat.dimension())) {
                s.seatPilot(player);
                if (s.hasData(FlightAttachments.FLIGHT.get())) {
                    FlightSync.send(player, s, FlightAttachments.state(s), UniverseTime.get(server).ut(),
                            FlightWarp.factor(server));
                }
                it.remove();
            } else if (seat.ticksLeft <= 1) {
                it.remove();
            } else {
                entry.setValue(new PendingSeat(seat.dimension(), seat.shipId(), seat.ticksLeft - 1));
            }
        }
    }

    /** Forgets all pending seats (server stop). */
    static void clearPending() {
        PENDING.clear();
        WAITING.clear();
        PREFETCH_COLUMN.clear();
    }

    /**
     * Requests the landing chunks of a descent that is coming, once a second, while the ship coasts down in orbit.
     * It finds when the orbit first drops through the transition altitude, at the current warp's game time. If that is
     * within {@link #PREFETCH_LEAD_S} game seconds, a ticket goes around the column it crosses above, so {@link #down}
     * finds the chunks generated. Does nothing when the ship is not descending.
     */
    static void prefetchLanding(MinecraftServer server, ShipEntity ship, FlightState st) {
        WAITING.remove(ship.getUUID());
        if (ship.tickCount % PREFETCH_INTERVAL_TICKS != 0) {
            return;
        }
        CelestialBody sys = FlightDimensions.system(st.body);
        if (st.altitude() < FlightDimensions.transitionAltitude(st.body) || st.r().dot(st.v()) >= 0) {
            return;
        }
        ServerLevel surface = server.getLevel(FlightDimensions.surfaceDimension(st.body));
        StateVector crossing = predictCrossing(sys, st.stateVector(),
                sys.radius + FlightDimensions.transitionAltitude(st.body));
        if (surface == null || crossing == null) {
            return;
        }
        Vec3d r = crossing.r();
        Vec3 landing = Frames.toSurfacePosition(r, sys.radius, FlightDimensions.seaLevel(st.body), Frames.tangentAt(r));
        ChunkPos column = new ChunkPos(Mth.floor(landing.x) >> 4, Mth.floor(landing.z) >> 4);
        ChunkPos kept = PREFETCH_COLUMN.get(ship.getUUID());
        if (kept != null && Math.max(Math.abs(kept.x() - column.x()), Math.abs(kept.z() - column.z()))
                <= PREFETCH_KEEP_CHUNKS) {
            column = kept;
        }
        PREFETCH_COLUMN.put(ship.getUUID(), column);
        requestLanding(surface, column);
    }

    /**
     * The state at which the orbit first falls below {@code floor} metres from the body centre, within
     * {@link #PREFETCH_LEAD_S} seconds of {@code now}, or null. Searches in steps, then bisects the step where it crosses.
     */
    private static StateVector predictCrossing(CelestialBody sys, StateVector now, double floor) {
        StateVector start = now;
        int steps = (int) Math.round(PREFETCH_LEAD_S / PREFETCH_STEP_S);
        for (int i = 0; i < steps; i++) {
            StateVector end = Kepler.propagate(sys.mu, start, PREFETCH_STEP_S);
            if (end.r().length() < floor) {
                double lo = 0.0;
                double hi = PREFETCH_STEP_S;
                for (int k = 0; k < PREFETCH_REFINE_STEPS; k++) {
                    double mid = 0.5 * (lo + hi);
                    if (Kepler.propagate(sys.mu, start, mid).r().length() < floor) {
                        hi = mid;
                    } else {
                        lo = mid;
                    }
                }
                return Kepler.propagate(sys.mu, start, hi);
            }
            start = end;
        }
        return null;
    }

    /** The surface point under the state of {@code st} after {@code dt} seconds of coasting. */
    private static Vec3 surfacePointAfter(FlightState st, CelestialBody sys, BodyId body, double dt) {
        Vec3d r = Kepler.propagate(sys.mu, st.stateVector(), dt).r();
        return Frames.toSurfacePosition(r, sys.radius, FlightDimensions.seaLevel(body), Frames.tangentAt(r));
    }

    /** Asks for the chunks around a landing point to be generated and kept, for {@link #LANDING_RADIUS} chunks. */
    private static void requestLanding(ServerLevel level, Vec3 pos) {
        requestLanding(level, new ChunkPos(Mth.floor(pos.x) >> 4, Mth.floor(pos.z) >> 4));
    }

    private static void requestLanding(ServerLevel level, ChunkPos chunk) {
        level.getChunkSource().addTicketWithRadius(TicketType.ENDER_PEARL, chunk, LANDING_RADIUS);
    }

    /**
     * Decides whether a descent whose landing chunk is not loaded may wait one more tick. It may, while it has waited
     * fewer than {@link #POSTPONE_LIMIT_TICKS} ticks and the next tick (at the current warp) keeps the orbit above the
     * surface. Otherwise it returns false and the caller loads the chunk at once.
     */
    private static boolean canWait(MinecraftServer server, UUID shipId, FlightState st, CelestialBody sys) {
        int waited = WAITING.getOrDefault(shipId, 0);
        StateVector next = Kepler.propagate(sys.mu, st.stateVector(),
                FlightEvents.TICK_SECONDS * FlightWarp.factor(server));
        if (waited >= POSTPONE_LIMIT_TICKS || next.r().length() <= sys.radius) {
            WAITING.remove(shipId);
            return false;
        }
        WAITING.put(shipId, waited + 1);
        return true;
    }

    /** Loads the chunk under a spawn point. A landing ticket usually has it ready; otherwise this call blocks. */
    private static void loadChunk(ServerLevel level, Vec3 pos) {
        if (!isLoaded(level, pos)) {
            long start = System.nanoTime();
            level.getChunk(Mth.floor(pos.x) >> 4, Mth.floor(pos.z) >> 4);
            long took = System.nanoTime() - start;
            if (took > BLOCKING_LOAD_WARN_NS) {
                Apoapsis.LOG.warn("Blocking landing chunk load took {} ms", took / 1_000_000L);
            }
        }
    }

    /** True when the chunk under a point is already loaded, without waiting for generation. */
    private static boolean isLoaded(ServerLevel level, Vec3 pos) {
        return level.getChunkSource().getChunkNow(Mth.floor(pos.x) >> 4, Mth.floor(pos.z) >> 4) != null;
    }

    /** Lifts a spawn point to one metre above the top solid block at its x and z when the point lies inside terrain. */
    private static Vec3 aboveTerrain(ServerLevel level, Vec3 pos) {
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(pos.x), Mth.floor(pos.z));
        return pos.y < top ? new Vec3(pos.x, top + 1.0, pos.z) : pos;
    }
}
