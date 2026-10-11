package dev.apoapsis.flight;

import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Vec3d;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Sends a ship's state, relative to its body in the body's inertial frame, to the pilot. */
final class FlightSync {
    private FlightSync() {}

    static void send(ServerPlayer pilot, ShipEntity ship, FlightState st, double ut, float warp) {
        Vec3d r;
        Vec3d v;
        if (st.regime == FlightRegime.SURFACE) {
            CelestialBody sys = FlightDimensions.system(st.body);
            Vec3 pos = ship.position();
            Frames.Tangent t = Frames.tangentAt(pos, sys.radius);
            r = Frames.toInertialPosition(pos, sys.radius, FlightDimensions.seaLevel(st.body), t);
            v = Frames.toInertialVelocity(ship.getVelocity(), t);
        } else {
            r = st.r();
            v = st.v();
        }
        if (pilot.connection == null || !pilot.connection.hasChannel(OrbitStatePayload.TYPE)) {
            return;
        }
        PacketDistributor.sendToPlayer(pilot, new OrbitStatePayload(ship.getId(), (byte) st.body.ordinal(),
                (byte) st.regime.ordinal(), ut, warp, r.x(), r.y(), r.z(), v.x(), v.y(), v.z()));
    }
}
