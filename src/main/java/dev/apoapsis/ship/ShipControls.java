package dev.apoapsis.ship;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.flight.FlightHooks;
import dev.apoapsis.net.PlayerMessages;
import dev.apoapsis.net.ShipInputPayload;
import dev.apoapsis.ownership.ShipOwnership;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server handler for pilot input. Only the pilot of a ship may drive it.
 *
 * <p>Rate limits, per player: at most one payload is applied per server tick. Payloads that arrive within one tick
 * are merged: the latest rotation wins, and the newer throttle wins unless it is {@code THROTTLE_HOLD}, in which case
 * an older one-shot throttle command (full, cut) is kept, so a press is not lost to a later idle payload. One-shot
 * flags (SAS, assembly, warp, parachutes) are merged by union. They are edge-triggered: a flag fires only if the previous applied
 * tick did not carry it, and each flag then has a cooldown ({@link #TAP_COOLDOWN_TICKS} for SAS and warp,
 * {@link #FLAG_COOLDOWN_TICKS} for assembly and parachutes).
 */
@EventBusSubscriber(modid = Apoapsis.MODID)
public final class ShipControls {
    /** Throttle change per tick while UP or DOWN is held: full travel in 40 ticks (2 s). */
    private static final float THROTTLE_STEP = 1f / 40f;
    /** Ticks before the same one-shot flag may fire again for the same player. */
    static final int FLAG_COOLDOWN_TICKS = 10;
    /** Shorter cooldown for SAS and warp taps, so a quick second press still registers. */
    static final int TAP_COOLDOWN_TICKS = 2;
    /** The one-shot bits of {@link ShipInputPayload#flags}; other bits are ignored. */
    private static final int ONE_SHOT_MASK = ShipInputPayload.FLAG_TOGGLE_SAS | ShipInputPayload.FLAG_ASSEMBLY
            | ShipInputPayload.FLAG_WARP_UP | ShipInputPayload.FLAG_WARP_DOWN | ShipInputPayload.FLAG_DEPLOY_CHUTES;
    /** Marks a flag that has never fired: far enough in the past that its cooldown has passed. */
    private static final long NEVER = Long.MIN_VALUE / 2;

    /** Input waiting for the next server tick. Written by the payload handler, drained on the server thread. */
    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();
    /** Edge and cooldown state per player. Touched on the server thread only; removed on logout. */
    private static final Map<UUID, Gate> GATES = new ConcurrentHashMap<>();

    /** Input merged for one player: the latest payload, and the one-shot bits of every payload merged into it. */
    private record Pending(Player player, ShipInputPayload latest, int flags) {
    }

    /** Rate-limit state of one player. */
    private static final class Gate {
        /** Server tick of the last applied input, and the one-shot bits it carried. */
        long lastTick = NEVER;
        int lastFlags;
        /** Last server tick each one-shot flag fired, indexed by bit position. */
        final long[] firedAt = {NEVER, NEVER, NEVER, NEVER, NEVER};
        /** True once a failure has been logged for this player, so a repeating fault logs once. */
        boolean warned;
    }

    private ShipControls() {}

    /** Payload entry point. Runs on the server thread (the NeoForge default), so it only queues the input for the next tick. */
    public static void handle(ShipInputPayload payload, IPayloadContext context) {
        Player player = context.player();
        int flags = payload.flags() & ONE_SHOT_MASK;
        PENDING.merge(player.getUUID(), new Pending(player, payload, flags), ShipControls::merge);
    }

    private static Pending merge(Pending older, Pending newer) {
        ShipInputPayload n = newer.latest();
        // A newer HOLD does not cancel an older one-shot throttle command merged into the same tick.
        byte throttle = n.throttle() == ShipInputPayload.THROTTLE_HOLD ? older.latest().throttle() : n.throttle();
        ShipInputPayload merged = new ShipInputPayload(n.shipId(), n.pitch(), n.yaw(), n.roll(), throttle, n.flags());
        return new Pending(newer.player(), merged, older.flags() | newer.flags());
    }

    /** Applies each player's queued input once per server tick, before the ships tick. */
    @SubscribeEvent
    public static void onServerTickPre(ServerTickEvent.Pre event) {
        long now = event.getServer().getTickCount();
        for (UUID id : PENDING.keySet()) {
            // remove() takes whatever was merged up to now, so a payload arriving meanwhile is not lost.
            Pending pending = PENDING.remove(id);
            if (pending != null) {
                Gate gate = GATES.computeIfAbsent(id, k -> new Gate());
                try {
                    apply(pending, gate, now);
                } catch (RuntimeException e) {
                    // A listener from another mod must not break the tick; this player's input is dropped.
                    if (!gate.warned) {
                        gate.warned = true;
                        Apoapsis.LOG.warn("Ship input for {} dropped after an error", id, e);
                    }
                }
            }
        }
    }

    /** Forgets a player's queued input and rate-limit state when they leave. */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        PENDING.remove(id);
        GATES.remove(id);
    }

    private static void apply(Pending pending, Gate gate, long now) {
        Player player = pending.player();
        ShipInputPayload input = pending.latest();
        // A one-shot bit is a rising edge unless the previous applied tick carried it too.
        int previous = gate.lastTick == now - 1 ? gate.lastFlags : 0;
        int edges = pending.flags() & ~previous;
        gate.lastTick = now;
        gate.lastFlags = pending.flags();

        if (player.isRemoved() || !(player.level().getEntity(input.shipId()) instanceof ShipEntity ship)
                || ship.getPilot() != player) {
            return;
        }
        ship.setPilotInput(input.pitch(), input.yaw(), input.roll());

        byte throttle = input.throttle();
        if (throttle == ShipInputPayload.THROTTLE_UP) {
            ship.setThrottle(ship.getThrottle() + THROTTLE_STEP);
        } else if (throttle == ShipInputPayload.THROTTLE_DOWN) {
            ship.setThrottle(ship.getThrottle() - THROTTLE_STEP);
        } else if (throttle == ShipInputPayload.THROTTLE_FULL) {
            ship.setThrottle(1f);
        } else if (throttle == ShipInputPayload.THROTTLE_CUT) {
            ship.setThrottle(0f);
        }

        if (fire(gate, edges, ShipInputPayload.FLAG_TOGGLE_SAS, now, TAP_COOLDOWN_TICKS)) {
            ship.cycleSasMode();
        }
        if (fire(gate, edges, ShipInputPayload.FLAG_WARP_UP, now, TAP_COOLDOWN_TICKS)) {
            FlightHooks.requestWarp(ship, 1);
        }
        if (fire(gate, edges, ShipInputPayload.FLAG_WARP_DOWN, now, TAP_COOLDOWN_TICKS)) {
            FlightHooks.requestWarp(ship, -1);
        }
        if (fire(gate, edges, ShipInputPayload.FLAG_DEPLOY_CHUTES, now, FLAG_COOLDOWN_TICKS)) {
            ship.deployParachutes();
        }
        if (fire(gate, edges, ShipInputPayload.FLAG_ASSEMBLY, now, FLAG_COOLDOWN_TICKS)) {
            Component error = ShipOwnership.mayDisassemble(ship, player)
                    ? ShipAssembler.disassemble(ship) : ShipOwnership.notYours(ship);
            if (error != null && player instanceof ServerPlayer serverPlayer) {
                PlayerMessages.event(serverPlayer, error);
            }
        }
    }

    /** True if {@code flag} rose this tick and is past its cooldown; records the firing when it returns true. */
    private static boolean fire(Gate gate, int edges, int flag, long now, int cooldownTicks) {
        if ((edges & flag) == 0) {
            return false;
        }
        int bit = Integer.numberOfTrailingZeros(flag);
        if (now - gate.firedAt[bit] < cooldownTicks) {
            return false;
        }
        gate.firedAt[bit] = now;
        return true;
    }
}
