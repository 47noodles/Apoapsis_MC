package dev.apoapsis.ship;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.flight.FlightHooks;
import dev.apoapsis.net.ShipInputPayload;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
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
 * are merged, so the latest rotation and throttle values win and no one-shot press is lost. One-shot flags
 * (SAS, assembly, warp) are edge-triggered: a flag fires only if the previous applied tick did not carry it, and each
 * flag then has a {@link #FLAG_COOLDOWN_TICKS} cooldown.
 */
@EventBusSubscriber(modid = Apoapsis.MODID)
public final class ShipControls {
    /** Throttle change per tick while UP or DOWN is held: full travel in 40 ticks (2 s). */
    private static final float THROTTLE_STEP = 1f / 40f;
    /** Ticks before the same one-shot flag may fire again for the same player. */
    static final int FLAG_COOLDOWN_TICKS = 10;
    /** The one-shot bits of {@link ShipInputPayload#flags}; other bits are ignored. */
    private static final int ONE_SHOT_MASK = ShipInputPayload.FLAG_TOGGLE_SAS | ShipInputPayload.FLAG_ASSEMBLY
            | ShipInputPayload.FLAG_WARP_UP | ShipInputPayload.FLAG_WARP_DOWN;
    /** Marks a flag that has never fired: far enough in the past that its cooldown has passed. */
    private static final long NEVER = Long.MIN_VALUE / 2;

    /** Input waiting for the next server tick. Written on network threads, drained on the server thread. */
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
        final long[] firedAt = {NEVER, NEVER, NEVER, NEVER};
    }

    private ShipControls() {}

    /** Network entry point. Runs on the network thread, so it only queues the input for the next server tick. */
    public static void handle(ShipInputPayload payload, IPayloadContext context) {
        Player player = context.player();
        int flags = payload.flags() & ONE_SHOT_MASK;
        PENDING.merge(player.getUUID(), new Pending(player, payload, flags), ShipControls::merge);
    }

    private static Pending merge(Pending older, Pending newer) {
        return new Pending(newer.player(), newer.latest(), older.flags() | newer.flags());
    }

    /** Applies each player's queued input once per server tick, before the ships tick. */
    @SubscribeEvent
    public static void onServerTickPre(ServerTickEvent.Pre event) {
        long now = event.getServer().getTickCount();
        for (UUID id : PENDING.keySet()) {
            // remove() takes whatever was merged up to now, so a payload arriving meanwhile is not lost.
            Pending pending = PENDING.remove(id);
            if (pending != null) {
                apply(pending, GATES.computeIfAbsent(id, k -> new Gate()), now);
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

        if (fire(gate, edges, ShipInputPayload.FLAG_TOGGLE_SAS, now)) {
            ship.cycleSasMode();
        }
        if (fire(gate, edges, ShipInputPayload.FLAG_WARP_UP, now)) {
            FlightHooks.requestWarp(ship, 1);
        }
        if (fire(gate, edges, ShipInputPayload.FLAG_WARP_DOWN, now)) {
            FlightHooks.requestWarp(ship, -1);
        }
        if (fire(gate, edges, ShipInputPayload.FLAG_ASSEMBLY, now)) {
            Component error = ShipAssembler.disassemble(ship);
            if (error != null) {
                player.sendOverlayMessage(error);
            }
        }
    }

    /** True if {@code flag} rose this tick and is past its cooldown; records the firing when it returns true. */
    private static boolean fire(Gate gate, int edges, int flag, long now) {
        if ((edges & flag) == 0) {
            return false;
        }
        int bit = Integer.numberOfTrailingZeros(flag);
        if (now - gate.firedAt[bit] < FLAG_COOLDOWN_TICKS) {
            return false;
        }
        gate.firedAt[bit] = now;
        return true;
    }
}
