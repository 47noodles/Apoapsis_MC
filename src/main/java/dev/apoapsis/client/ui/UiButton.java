package dev.apoapsis.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * The kit's button: a vanilla {@link Button} (so focus, Tab and arrow navigation, Enter, narration and sounds all
 * work) drawn in the kit's style, with hover, keyboard focus, pressed and disabled states. Place it with
 * {@link #place} in UI units. Client only.
 */
public final class UiButton extends Button {
    /** How a button looks. */
    public enum Variant {
        /** A row in a vertical menu: no box at rest, a brass bar on hover. */
        MENU,
        /** A bordered secondary button, label centred. */
        PLATE,
        /** The one main action: solid brass. */
        PRIMARY,
        /** A bordered button whose hover is copper: it leaves something behind. */
        DANGER,
        /** Small caption-sized text link, for footers. */
        LINK
    }

    private final Variant variant;
    private float hover;
    private long lastNanos;

    public UiButton(Component label, OnPress onPress, Variant variant) {
        super(0, 0, 100, 20, label, onPress, DEFAULT_NARRATION);
        this.variant = variant;
    }

    /** Sets the button's rectangle from UI units. */
    public UiButton place(UiScale scale, float ux, float uy, float uw, float uh) {
        setX(scale.gui(ux));
        setY(scale.gui(uy));
        setWidth(Math.max(1, scale.gui(uw)));
        setHeight(Math.max(1, scale.gui(uh)));
        return this;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        UiScale s = UiScale.current();
        int w = Math.round(s.ui(getWidth()));
        int h = Math.round(s.ui(getHeight()));
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 1f : Math.min(0.1f, (now - lastNanos) / 1e9f);
        lastNanos = now;
        float target = active && isHovered() ? 1f : 0f;
        hover += (target - hover) * (1f - (float) Math.exp(-dt / 0.06f));
        boolean focus = active && isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard();
        boolean pressed = active && isHovered() && Minecraft.getInstance().mouseHandler.isLeftPressed();

        s.begin(g, getX(), getY());
        switch (variant) {
            case MENU -> drawMenu(g, w, h, focus, pressed);
            case PLATE, DANGER -> drawPlate(g, w, h, focus, pressed);
            case PRIMARY -> drawPrimary(g, w, h, focus, pressed);
            case LINK -> drawLink(g, w, h, focus);
        }
        s.end(g);
    }

    private int textColor(boolean focus) {
        if (!active) {
            return UiPalette.INK_DIM;
        }
        return focus ? UiPalette.SIGNAL : UiPalette.mix(UiPalette.mix(UiPalette.INK_MUTED, UiPalette.INK, 0.7f), 0xFFFFFFFF, hover * 0.55f);
    }

    private void drawMenu(GuiGraphicsExtractor g, int w, int h, boolean focus, boolean pressed) {
        if (focus) {
            g.fill(0, 0, w, h, UiPalette.SIGNAL_GLASS);
            UiPanel.edge(g, 0, 0, w, h, UiPalette.SIGNAL_EDGE);
            UiPanel.brackets(g, -1, -1, w + 1, h + 1, 4, UiPalette.SIGNAL);
        } else if (hover > 0.01f) {
            g.fill(0, 0, w, h, UiPalette.fade(pressed ? UiPalette.STEEL_950 : UiPalette.STEEL_800, hover));
            g.fill(0, 0, 2, h, UiPalette.fade(UiPalette.BRASS, hover));
        }
        int cap = UiText.Type.TITLE.capHeight();
        int top = (h - cap) / 2 + (pressed ? 1 : 0);
        UiText.draw(g, UiText.Type.TITLE, getMessage().getString(), 14, top, textColor(focus));
    }

    private void drawPlate(GuiGraphicsExtractor g, int w, int h, boolean focus, boolean pressed) {
        int accent = variant == Variant.DANGER ? UiPalette.COPPER : UiPalette.BRASS;
        if (!active) {
            UiPanel.dashed(g, 0, 0, w, h, UiPalette.STEEL_600);
        } else if (focus) {
            g.fill(0, 0, w, h, UiPalette.SIGNAL_GLASS);
            UiPanel.edge(g, 0, 0, w, h, UiPalette.SIGNAL_EDGE);
            UiPanel.brackets(g, -1, -1, w + 1, h + 1, 4, UiPalette.SIGNAL);
        } else {
            g.fill(0, 0, w, h, pressed ? UiPalette.STEEL_950 : UiPalette.mix(UiPalette.STEEL_900, UiPalette.STEEL_800, hover));
            UiPanel.edge(g, 0, 0, w, h, UiPalette.mix(UiPalette.STEEL_600, accent, hover));
        }
        int top = (h - UiText.Type.TITLE.capHeight()) / 2 + (pressed ? 1 : 0);
        int color = !active ? UiPalette.INK_DIM : focus ? UiPalette.SIGNAL
                : UiPalette.mix(UiPalette.INK, accent, hover * 0.5f);
        UiText.drawCentered(g, UiText.Type.TITLE, getMessage().getString(), w / 2f, top, color);
    }

    private void drawPrimary(GuiGraphicsExtractor g, int w, int h, boolean focus, boolean pressed) {
        int fill = !active ? UiPalette.STEEL_700
                : pressed ? UiPalette.BRASS_DEEP : UiPalette.mix(UiPalette.BRASS, 0xFFE0C384, hover);
        g.fill(0, 0, w, h, fill);
        if (focus) {
            UiPanel.brackets(g, -2, -2, w + 2, h + 2, 4, UiPalette.SIGNAL);
        }
        int top = (h - UiText.Type.TITLE.capHeight()) / 2 + (pressed ? 1 : 0);
        UiText.drawCentered(g, UiText.Type.TITLE, getMessage().getString(), w / 2f, top,
                active ? UiPalette.STEEL_950 : UiPalette.INK_DIM);
    }

    private void drawLink(GuiGraphicsExtractor g, int w, int h, boolean focus) {
        int color = !active ? UiPalette.INK_DIM : focus ? UiPalette.SIGNAL
                : UiPalette.mix(UiPalette.INK_MUTED, UiPalette.INK, hover);
        int cap = UiText.Type.LABEL.capHeight();
        int top = (h - cap) / 2;
        UiText.draw(g, UiText.Type.LABEL, getMessage().getString(), 0, top, color);
        if (active && (focus || hover > 0.5f)) {
            int tw = Math.round(UiText.width(UiText.Type.LABEL, getMessage().getString()));
            g.fill(0, top + cap + 2, tw, top + cap + 3, focus ? UiPalette.SIGNAL : UiPalette.BRASS);
        }
    }
}
