package dev.apoapsis.scenario;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.ages.Ages;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.Flight;
import dev.apoapsis.net.PlayerMessages;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.ownership.ShipOwnership;
import dev.apoapsis.ship.ShipEntity;
import dev.apoapsis.ship.ShipStructure;
import dev.apoapsis.test.LanderCommand;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Puts the Kestrel in a 16 km Verdance orbit with the player in the cockpit, Pale's phase set for a transfer window. */
public final class KestrelOrbit {
    /** Orbit altitude above Verdance's surface, m. */
    public static final double ALTITUDE_M = 16_000.0;
    /** Time from now until the Hohmann burn, s. */
    public static final double WINDOW_S = 120.0;
    private static final double SHIP_PHASE_DEG = 0.0;
    /** JVM system property: {@code lander} spawns the Kestrel with four parachutes and four landing legs, full tanks (the return-trip run). */
    public static final String SHIP_PROPERTY = "apoapsis.testShip";

    private KestrelOrbit() {}

    /**
     * Runs the orbit scenario for {@code player}: sets Pale's phase, spawns the ship in orbit, moves the player into it
     * and raises the player to at least Age 4. Returns false (after telling the player) if the ship could not be spawned.
     */
    public static boolean run(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        Blueprint blueprint;
        try {
            blueprint = Blueprint.load(server, "kestrel");
        } catch (IllegalStateException e) {
            PlayerMessages.warning(player, Component.translatable("apoapsis.scenario.blueprint.failed"));
            return false;
        }

        double mu = MeridianSystem.VERDANCE.mu;
        double r1 = MeridianSystem.VERDANCE.radius + ALTITUDE_M;
        double r2 = MeridianSystem.PALE.orbitRadius;
        double transferA = (r1 + r2) / 2.0;
        double transferS = Math.PI * Math.sqrt(transferA * transferA * transferA / mu);
        double shipPeriodS = 2.0 * Math.PI * Math.sqrt(r1 * r1 * r1 / mu);
        double palePeriodS = MeridianSystem.PALE.orbitPeriod();
        // Pale must lead the ship by 180 deg minus its own travel during the transfer, at the burn.
        double leadAtBurnDeg = 180.0 - 360.0 * transferS / palePeriodS;
        // The ship gains on Pale, so the lead shrinks: start with the extra lead it will have lost by the burn.
        double leadNowDeg = leadAtBurnDeg + (360.0 / shipPeriodS - 360.0 / palePeriodS) * WINDOW_S;
        Flight.setPalePhase(server, SHIP_PHASE_DEG + leadNowDeg);

        ShipStructure structure = "lander".equals(System.getProperty(SHIP_PROPERTY))
                ? LanderCommand.landerStructure(server, true, false)
                : blueprint.toStructure();
        ShipEntity ship = Flight.spawnInOrbit(server, structure, BodyId.VERDANCE, ALTITUDE_M, SHIP_PHASE_DEG);
        if (ship == null) {
            PlayerMessages.warning(player, Component.translatable("apoapsis.scenario.orbit.failed"));
            return false;
        }
        ShipOwnership.claim(ship, player);

        ServerLevel orbit = (ServerLevel) ship.level();
        player.teleportTo(orbit, ship.getX(), ship.getY(), ship.getZ(), Set.of(), player.getYRot(), player.getXRot(), true);
        Scenarios.runNextTick(() -> {
            try {
                ship.seatPilot(player);
            } catch (RuntimeException e) {
                Apoapsis.LOG.warn("Could not seat the pilot in the orbit scenario", e);
            }
        });
        // Quiet: no flight has happened, so the Custodian does not announce First Flight.
        // stored value: sandbox must not leak a raised Age into the save
        Ages.setAge(player, Math.max(Ages.stored(player), 4), false);
        PlayerMessages.event(player, Component.translatable("apoapsis.scenario.orbit.done"));
        PlayerMessages.event(player, Component.translatable("apoapsis.scenario.orbit.hint",
                Component.keybind("key.apoapsis.sas"), Component.keybind("key.apoapsis.system_map")));
        return true;
    }
}
