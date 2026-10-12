package dev.apoapsis.client;

import dev.apoapsis.client.hud.HudLayout;
import dev.apoapsis.client.notify.CleanFlightView;
import dev.apoapsis.client.notify.Notify;
import dev.apoapsis.client.ui.UiPalette;
import dev.apoapsis.client.ui.UiScale;
import dev.apoapsis.client.ui.UiText;
import dev.apoapsis.client.ui.UiTextSmooth;
import dev.apoapsis.mixin.client.HudAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jspecify.annotations.Nullable;

/**
 * The vanilla overlay message (the action-bar line) while it is showing. The HUD draws it, but a full-screen map or a
 * container covers the HUD, so the map draws it in its bottom line and containers get it drawn above their panel.
 * Client only.
 */
public final class OverlayMessages {
    /** Space between the plate and the container's panel, and the plate's padding, UI units. */
    private static final int PLATE_GAP = 4;
    private static final int PAD_X = 8;
    private static final int PAD_Y = 4;

    private OverlayMessages() {}

    /**
     * The line a screen shows where the action bar would be, or null: the mod's event line while it shows, else the
     * overlay message while its time is above zero. Vanilla's boarding hint is never returned (see
     * {@link CleanFlightView}). Call from the render thread.
     */
    public static @Nullable Component current(Minecraft mc) {
        Component event = Notify.eventLine();
        if (event != null) {
            return event;
        }
        HudAccessor hud = (HudAccessor) mc.gui.hud;
        Component msg = hud.apoapsis$overlayTime() > 0 ? hud.apoapsis$overlayMessage() : null;
        return CleanFlightView.isBoardingHint(msg) ? null : msg;
    }

    /** The overlay message only when it is one of ours, else null: the map shows Apoapsis messages and no vanilla ones. */
    public static @Nullable Component currentOwn(Minecraft mc) {
        Component msg = current(mc);
        return msg != null && isOurs(msg) ? msg : null;
    }

    /** Draws an Apoapsis overlay message centred just above a container's panel, in the kit's type on a faint plate. */
    static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Component msg = current(mc);
        if (msg == null || !isOurs(msg)) {
            return;
        }
        GuiGraphicsExtractor g = event.getGuiGraphics();
        UiScale s = UiScale.current();
        String text = msg.getString();
        float width = UiTextSmooth.width(UiText.Type.BODY, text);
        float cx = (screen.getLeftPos() + screen.getImageWidth() / 2f) / s.scale();
        int capTop = Math.max(HudLayout.TOP_MARGIN, Math.round(screen.getTopPos() / s.scale()) - PLATE_GAP - PAD_Y
                - UiText.Type.BODY.capHeight());
        s.begin(g, 0, 0);
        int x0 = Math.round(cx - width / 2f) - PAD_X;
        int x1 = Math.round(cx + width / 2f) + PAD_X;
        g.fill(x0, capTop - PAD_Y, x1, capTop + UiText.Type.BODY.capHeight() + PAD_Y,
                UiPalette.alpha(UiPalette.STEEL_950, 0xC0));
        g.fill(x0, capTop - PAD_Y, x0 + 2, capTop + UiText.Type.BODY.capHeight() + PAD_Y, UiPalette.BRASS);
        UiTextSmooth.drawCentered(g, UiText.Type.BODY, text, cx, capTop, UiPalette.INK);
        s.end(g);
    }

    /** True for a message this mod sent: its translation key is in the apoapsis namespace. */
    static boolean isOurs(Component msg) {
        return msg.getContents() instanceof TranslatableContents t
                && (t.getKey().startsWith("apoapsis.") || t.getKey().startsWith("message.apoapsis."));
    }
}
