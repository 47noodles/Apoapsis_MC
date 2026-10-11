package dev.apoapsis.flight;

import dev.apoapsis.ages.Ages;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * End-of-tick flight rules for one ship: regime transitions, the First Flight and landing milestones, warp
 * validation, and the orbit-state sync to the pilot every 5 ticks.
 */
public final class FlightRules {
    /** Airborne time before a landing counts as a landing (10 s at 0.05 s per tick). */
    static final int LANDING_TICKS = 200;
    /** Altitude for First Flight above Verdance, metres. */
    static final double FIRST_FLIGHT_ALTITUDE_M = 1500.0;
    /** Ticks between state sync packets. */
    static final int SYNC_INTERVAL_TICKS = 5;
    /** Ticks after a transition before the reverse one may fire (2 s), so a hover at the boundary does not flip. */
    static final int DWELL_TICKS = 40;

    private FlightRules() {}

    /**
     * First Flight for one tick, as a pure decision so GameTests can call it without a client. Reaching 1,500 m only
     * sets {@code reachedHigh} (the caller does that); the milestone is due on the first landing on Verdance after it,
     * with the pilot aboard, and only once per ship state.
     */
    public static boolean firstFlightDue(boolean reachedHigh, boolean granted, boolean onVerdance, boolean landed,
                                         boolean pilotAboard) {
        return reachedHigh && !granted && onVerdance && landed && pilotAboard;
    }

    /** True once {@link #DWELL_TICKS} have passed since the ship's last transition. */
    private static boolean dwelled(MinecraftServer server, FlightState st) {
        return server.getTickCount() - st.transitionTick >= DWELL_TICKS;
    }

    static void afterTick(ShipEntity ship) {
        if (ship.isRemoved() || !(ship.level() instanceof ServerLevel level)) {
            return;
        }
        MinecraftServer server = level.getServer();
        FlightState st = FlightAttachments.state(ship);
        ServerPlayer pilot = ship.getPilot() instanceof ServerPlayer p ? p : null;
        FlightWarp.validate(ship);

        if (st.regime == FlightRegime.SURFACE) {
            st.body = FlightDimensions.surfaceBody(level.dimension());
            double alt = ship.getY() - FlightDimensions.seaLevel(st.body);
            if (alt >= FlightDimensions.transitionAltitude(st.body) && dwelled(server, st)) {
                Transitions.up(ship, st, level);
                return;
            }
            if (st.body == BodyId.VERDANCE && alt >= FIRST_FLIGHT_ALTITUDE_M && pilot != null && !st.reachedHigh) {
                st.reachedHigh = true;
                FlightMessages.toPilot(ship, Component.translatable("apoapsis.flight.first_flight_high"));
            }
            boolean landed = ship.isLanded() && st.airborneTicks > LANDING_TICKS;
            if (firstFlightDue(st.reachedHigh, st.firstFlight, st.body == BodyId.VERDANCE, landed, pilot != null)) {
                st.firstFlight = true;
                Ages.onFirstFlight(pilot);
            }
            if (ship.isLanded()) {
                if (st.airborneTicks > LANDING_TICKS && pilot != null) {
                    Ages.onLanded(pilot, st.body);
                    FlightMessages.toPilot(ship, Component.translatable("apoapsis.flight.landed",
                            FlightMessages.bodyName(st.body)));
                }
                st.airborneTicks = 0;
            } else {
                st.airborneTicks = Math.min(st.airborneTicks + 1, LANDING_TICKS + 1);
            }
        } else if (st.altitude() < FlightDimensions.transitionAltitude(st.body)
                && st.r().dot(st.v()) < 0 && dwelled(server, st)) {
            Transitions.down(ship, st, level);
            return;
        } else {
            Transitions.prefetchLanding(server, ship, st);
        }

        if (pilot != null && ship.tickCount % SYNC_INTERVAL_TICKS == 0) {
            FlightSync.send(pilot, ship, st, UniverseTime.get(server).ut(), FlightWarp.factorFor(ship));
        }
    }
}
