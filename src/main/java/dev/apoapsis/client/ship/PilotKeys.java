package dev.apoapsis.client.ship;

import com.mojang.blaze3d.platform.InputConstants;
import dev.apoapsis.Apoapsis;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * Key mappings for piloting. W/A/S/D and the vanilla keys are read from {@link Options}; these are the extra keys.
 * Client only.
 */
public final class PilotKeys {
    /** The Apoapsis controls category. Its label is the {@code key.category.apoapsis.apoapsis} string. */
    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "apoapsis"));

    public static final KeyMapping ROLL_LEFT = key("roll_left", InputConstants.KEY_Q);
    public static final KeyMapping ROLL_RIGHT = key("roll_right", InputConstants.KEY_E);
    public static final KeyMapping THROTTLE_UP = key("throttle_up", InputConstants.KEY_R);
    public static final KeyMapping THROTTLE_DOWN = key("throttle_down", InputConstants.KEY_F);
    public static final KeyMapping THROTTLE_FULL = key("throttle_full", InputConstants.KEY_Z);
    public static final KeyMapping THROTTLE_CUT = key("throttle_cut", InputConstants.KEY_X);
    public static final KeyMapping SAS = key("sas", InputConstants.KEY_G);
    public static final KeyMapping DISASSEMBLE = key("disassemble", InputConstants.KEY_B);
    public static final KeyMapping DEPLOY_PARACHUTES = key("deploy_parachutes", InputConstants.KEY_P);
    public static final KeyMapping SYSTEM_MAP = key("system_map", InputConstants.KEY_M);
    public static final KeyMapping WARP_UP = key("warp_up", InputConstants.KEY_PERIOD);
    public static final KeyMapping WARP_DOWN = key("warp_down", InputConstants.KEY_COMMA);

    private PilotKeys() {}

    private static KeyMapping key(String id, int keyCode) {
        return new KeyMapping("key.apoapsis." + id, InputConstants.Type.KEYBOARD, keyCode, CATEGORY);
    }

    /** Adds the category and every key mapping to the game. Called from {@code ShipClient} on the mod bus. */
    public static void registerTo(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(ROLL_LEFT);
        event.register(ROLL_RIGHT);
        event.register(THROTTLE_UP);
        event.register(THROTTLE_DOWN);
        event.register(THROTTLE_FULL);
        event.register(THROTTLE_CUT);
        event.register(SAS);
        event.register(DISASSEMBLE);
        event.register(DEPLOY_PARACHUTES);
        event.register(SYSTEM_MAP);
        event.register(WARP_UP);
        event.register(WARP_DOWN);
    }

    /**
     * Takes every press of {@code key} since the last call. Returns true if there was at least one.
     * Called every client tick, piloting or not, so a press never goes stale.
     */
    public static boolean pressed(KeyMapping key) {
        boolean any = false;
        while (key.consumeClick()) {
            any = true;
        }
        return any;
    }

    /**
     * Discards the clicks of vanilla keys that clash with pilot controls while piloting: Q drop, E inventory,
     * F swap-offhand, G quick actions (SAS is on G), and P social interactions (parachutes are on P). Must run before vanilla's key handling in the same tick.
     */
    public static void swallowVanillaClashes(Options options) {
        pressed(options.keyDrop);
        pressed(options.keyInventory);
        pressed(options.keySwapOffhand);
        pressed(options.keyQuickActions);
        pressed(options.keySocialInteractions);
    }
}
