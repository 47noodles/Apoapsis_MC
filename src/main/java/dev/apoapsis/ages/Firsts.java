package dev.apoapsis.ages;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.FlightDimensions;
import dev.apoapsis.net.PlayerMessages;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Vec3d;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * The firsts: one-off milestones per player. Each is recorded once, sends one Custodian line and awards the advancement
 * of the same id (data/apoapsis/advancement). Recorded ids live in {@link AgeData#FIRSTS}, copied on death.
 */
public final class Firsts {
    private Firsts() {}

    /** The firsts, in progression order. {@link #id()} is the stored entry, the lang key part and the advancement path. */
    public enum First {
        FIRST_FLIGHT, SUBORBITAL, ORBIT, PALE_FLYBY, PALE_LANDING, CREWED_RETURN, PALE_SAMPLE;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Ticks between orbit checks (one second). */
    static final int ORBIT_CHECK_TICKS = 20;
    /** Age a Pale regolith sample grants, on a Verdance landing after PALE_LANDING. */
    static final int SAMPLE_AGE = 5;

    /** Records a first once: a Custodian line and the advancement. Does nothing if it is already recorded. */
    public static void record(ServerPlayer player, First first) {
        record(player, first, true);
    }

    /** As {@link #record(ServerPlayer, First)}; {@code line} is false for a first whose line was shown before (old saves). */
    static void record(ServerPlayer player, First first, boolean line) {
        record(player, first, line, true);
    }

    /**
     * As above; {@code guide} is false to record a first without opening its step of the {@link Guide} (the quiet
     * old-save backfill).
     */
    static void record(ServerPlayer player, First first, boolean line, boolean guide) {
        if (has(player, first)) {
            return;
        }
        List<String> done = new ArrayList<>(recorded(player));
        done.add(first.id());
        player.setData(AgeData.FIRSTS.get(), List.copyOf(done));
        if (line) {
            PlayerMessages.first(player, Component.translatable("apoapsis.advancement." + first.id() + ".title"),
                    Component.translatable("apoapsis.firsts." + first.id() + ".line"));
        }
        AdvancementHolder holder = player.level().getServer().getAdvancements()
                .get(Identifier.fromNamespaceAndPath(Apoapsis.MODID, first.id()));
        if (holder != null) {
            player.getAdvancements().award(holder, "code");
        }
        if (guide) {
            Guide.onFirst(player, first);
        }
    }

    /** Whether the player has recorded this first. */
    public static boolean has(Player player, First first) {
        return player.getData(AgeData.FIRSTS.get()).contains(first.id());
    }

    /** The ids of the firsts the player has recorded, in order. */
    public static List<String> recorded(Player player) {
        return List.copyOf(player.getData(AgeData.FIRSTS.get()));
    }

    /** SUBORBITAL: the ship went up from a surface into orbit (flight/Transitions.up). */
    public static void suborbital(Entity pilot) {
        recordFor(pilot, First.SUBORBITAL);
    }

    /** PALE_FLYBY: the ship entered Pale's sphere of influence (flight/OrbitFlight.switchSoi). */
    public static void paleFlyby(Entity pilot) {
        recordFor(pilot, First.PALE_FLYBY);
    }

    /**
     * ORBIT: once a second, in orbit around Verdance with the engines off, the orbit is bound and its periapsis is above
     * the top of Verdance's atmosphere (atmosphereTop, 12 km). Called every tick of an orbiting ship
     * from flight/FlightRules.
     */
    public static void checkOrbit(Entity pilot, boolean enginesOff, BodyId body, Vec3d r, Vec3d v) {
        if (!(pilot instanceof ServerPlayer player) || player.tickCount % ORBIT_CHECK_TICKS != 0) {
            return;
        }
        if (!enginesOff || body != BodyId.VERDANCE || has(player, First.ORBIT)) {
            return;
        }
        CelestialBody sys = FlightDimensions.system(body);
        if (periapsisAltitude(sys, r, v) > sys.atmosphereTop) {
            record(player, First.ORBIT);
        }
    }

    /**
     * Periapsis altitude of the state, metres, from h^2 / (mu (1 + e)) minus the body radius. Negative infinity when the
     * orbit is not bound, so an escape trajectory never counts as stable.
     */
    static double periapsisAltitude(CelestialBody body, Vec3d r, Vec3d v) {
        double rr = r.length();
        double vv = v.lengthSquared();
        double energy = 0.5 * vv - body.mu / rr;
        if (energy >= 0) {
            return Double.NEGATIVE_INFINITY;
        }
        double h2 = Math.max(0, rr * rr * vv - Math.pow(r.dot(v), 2));
        double ecc = Math.sqrt(Math.max(0, 1 + 2 * energy * h2 / (body.mu * body.mu)));
        return h2 / (body.mu * (1 + ecc)) - body.radius;
    }

    /**
     * PALE_LANDING's Age 5 rule, as a pure decision for GameTests: a Verdance landing after PALE_LANDING with a Pale
     * regolith sample carried. The sample is kept.
     */
    public static boolean sampleAgeDue(boolean paleLanding, boolean onVerdance, boolean carriesSample) {
        return paleLanding && onVerdance && carriesSample;
    }

    /** Whether the player carries a Pale regolith sample anywhere in the inventory. */
    static boolean carriesSample(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).getItem() == AgeItems.PALE_SAMPLE.get()) {
                return true;
            }
        }
        return false;
    }

    private static void recordFor(Entity pilot, First first) {
        if (pilot instanceof ServerPlayer player) {
            record(player, first);
        }
    }
}
