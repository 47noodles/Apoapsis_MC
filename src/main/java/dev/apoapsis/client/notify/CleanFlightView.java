package dev.apoapsis.client.notify;

import dev.apoapsis.mixin.client.HudAccessor;
import dev.apoapsis.ship.ShipEntity;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jspecify.annotations.Nullable;

/**
 * While the local player pilots a ship, the vanilla parts of the HUD that have no job there are not drawn: the hotbar and
 * its item name, the crosshair (the flight HUD draws its own markers), the experience bar and level, and the vanilla
 * "Press Left Shift to dismount" line, which landed on the instruments. Health, hunger and air stay: the flight HUD leaves
 * room for them in survival. Everything is back the moment the player is not riding a ship, because nothing is changed,
 * only left undrawn. Client only; no mixin.
 */
public final class CleanFlightView {
    private static final Set<Identifier> HIDDEN = Set.of(
            VanillaGuiLayers.CROSSHAIR,
            VanillaGuiLayers.HOTBAR,
            VanillaGuiLayers.SELECTED_ITEM_NAME,
            VanillaGuiLayers.EXPERIENCE_LEVEL,
            VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND,
            VanillaGuiLayers.CONTEXTUAL_INFO_BAR);

    private CleanFlightView() {}

    /** Whether the local player is riding a ship. */
    static boolean piloting(Minecraft mc) {
        return mc.player != null && mc.player.getVehicle() instanceof ShipEntity;
    }

    static void onRenderLayer(RenderGuiLayerEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (!piloting(mc)) {
            return;
        }
        Identifier id = event.getName();
        if (HIDDEN.contains(id)) {
            event.setCanceled(true);
        } else if (VanillaGuiLayers.OVERLAY_MESSAGE.equals(id) && isDismountHint(mc.gui.hud)) {
            event.setCanceled(true);
        }
    }

    /** Whether the action-bar line showing is vanilla's boarding line, "press shift to dismount". */
    private static boolean isDismountHint(Hud hud) {
        HudAccessor access = (HudAccessor) hud;
        return access.apoapsis$overlayTime() > 0 && isBoardingHint(access.apoapsis$overlayMessage());
    }

    /** True for vanilla's boarding lines ({@code mount.onboard}, {@code mount.sit}). */
    public static boolean isBoardingHint(@Nullable Component msg) {
        return msg != null && msg.getContents() instanceof TranslatableContents t
                && (t.getKey().equals("mount.onboard") || t.getKey().equals("mount.sit"));
    }
}
