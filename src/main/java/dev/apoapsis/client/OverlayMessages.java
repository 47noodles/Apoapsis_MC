package dev.apoapsis.client;

import dev.apoapsis.mixin.client.HudAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
    private static final int ABOVE_PANEL_PX = 12;
    private static final int TEXT_COLOUR = 0xFFFFFFFF;

    private OverlayMessages() {}

    /** The overlay message while its time is above zero, or null. Call from the render thread. */
    public static @Nullable Component current(Minecraft mc) {
        HudAccessor hud = (HudAccessor) mc.gui.hud;
        return hud.apoapsis$overlayTime() > 0 ? hud.apoapsis$overlayMessage() : null;
    }

    /** The overlay message only when it is one of ours, else null: the map shows Apoapsis messages and no vanilla ones. */
    public static @Nullable Component currentOwn(Minecraft mc) {
        Component msg = current(mc);
        return msg != null && isOurs(msg) ? msg : null;
    }

    /** Draws an Apoapsis overlay message centred just above a container's panel, on a backdrop like the HUD's. */
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
        Font font = mc.font;
        int width = font.width(msg);
        int x = screen.getLeftPos() + screen.getImageWidth() / 2 - width / 2;
        g.textWithBackdrop(font, msg, x, screen.getTopPos() - ABOVE_PANEL_PX, width, TEXT_COLOUR);
    }

    /** True for a message this mod sent: its translation key is in the apoapsis namespace. */
    static boolean isOurs(Component msg) {
        return msg.getContents() instanceof TranslatableContents t
                && (t.getKey().startsWith("apoapsis.") || t.getKey().startsWith("message.apoapsis."));
    }
}
