package dev.apoapsis.flight;

import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.Vec3d;
import dev.apoapsis.ship.ShipEntity;
import dev.apoapsis.ship.ShipStructure;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Moves a ship between its surface dimension and the orbit dimension. The ship is replaced by a new entity in the
 * target level, and its pilot is teleported in and seated on the next tick.
 */
final class Transitions {
    /** Ticks to wait for a pilot to reach the target level before the seat is dropped. */
    private static final int SEAT_TIMEOUT_TICKS = 40;

    private static final Map<UUID, PendingSeat> PENDING = new HashMap<>();

    private Transitions() {}

    private record PendingSeat(ResourceKey<Level> dimension, UUID shipId, int ticksLeft) {}

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
        int slot = freeSlot(orbit);
        FlightState state = FlightState.inOrbit(body, new StateVector(r, v), UniverseTime.get(server).ut(), slot);
        state.firstFlight = old.firstFlight;

        FlightMessages.toPilot(ship, Component.translatable("apoapsis.flight.enter_orbit",
                FlightMessages.bodyName(body)));
        FlightWarp.clear();
        loadChunk(orbit, FlightDimensions.slotPosition(slot));
        move(ship, orbit, FlightDimensions.slotPosition(slot), q, state, Vec3.ZERO);
    }

    /** Orbit to surface: the ship comes down at the sub-point of its inertial position, on the body's surface. */
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
        Vec3 vel = Frames.toSurfaceVelocity(old.v(), t);
        Quaternionf q = Frames.orbitToSurfaceOrientation(ship.getOrientation(), t);
        FlightState state = new FlightState();
        state.regime = FlightRegime.SURFACE;
        state.body = body;
        state.firstFlight = old.firstFlight;

        FlightMessages.toPilot(ship, Component.translatable(body == BodyId.VERDANCE
                ? "apoapsis.flight.enter_atmosphere"
                : "apoapsis.flight.leave_orbit"));
        FlightWarp.clear();
        loadChunk(surface, pos);
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
        fresh.setSas(old.isSasOn());
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
    }

    /** The lowest orbit slot that no loaded ship uses. */
    static int freeSlot(ServerLevel orbit) {
        Set<Integer> used = new HashSet<>();
        for (Entity e : orbit.getAllEntities()) {
            if (e instanceof ShipEntity s) {
                int slot = FlightAttachments.state(s).slot;
                if (slot >= 0) {
                    used.add(slot);
                }
            }
        }
        int slot = 0;
        while (used.contains(slot)) {
            slot++;
        }
        return slot;
    }

    private static void loadChunk(ServerLevel level, Vec3 pos) {
        level.getChunk(Mth.floor(pos.x) >> 4, Mth.floor(pos.z) >> 4);
    }
}
