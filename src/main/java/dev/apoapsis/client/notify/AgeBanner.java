package dev.apoapsis.client.notify;

import dev.apoapsis.client.hud.HudLayout;
import dev.apoapsis.client.ui.UiScale;
import dev.apoapsis.client.ui.UiText;
import dev.apoapsis.client.ui.UiTextSmooth;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * The Age banner: a short ceremonial moment when the Age goes up. The Age's name in capitals, large, with one sentence
 * under it between two brass hairlines that draw out from the middle. It sits in the upper third of the view, centred,
 * so the instruments at the bottom and the edges stay clear, and fades in and out over about five seconds. A banner that
 * arrives while one shows waits for it. Client only.
 */
final class AgeBanner {
    /** Ticks the banner lasts, fades included: 4.8 seconds. */
    static final int TOTAL = 96;
    private static final int FADE_IN = 16;
    private static final int FADE_OUT = 24;
    /** How far down the view the banner's middle sits. */
    private static final double CENTRE_Y = 0.24;
    /** The widest the sentence wraps, as a share of the view width: keeps clear of the guide panel at the right. */
    private static final double MAX_SHARE = 0.42;
    /** The narrowest the sentence wraps, in UI units. */
    private static final int MIN_WRAP = 140;
    private static final int PAD_X = 16;
    private static final int PAD_Y = 10;
    private static final int GAP = 6;
    private static final int ROW = 12;
    private static final int RULE_W = 28;

    private record Banner(Component title, Component line) {}

    private static Banner showing;
    private static Banner waiting;
    private static int age;

    private AgeBanner() {}

    static void show(Component title, Component line) {
        Banner next = new Banner(title, line);
        if (showing == null) {
            showing = next;
            age = 0;
        } else {
            waiting = next;
        }
    }

    static void tick() {
        if (showing == null) {
            return;
        }
        if (++age >= TOTAL) {
            showing = waiting;
            waiting = null;
            age = 0;
        }
    }

    /** Whether a banner is on show. */
    static boolean showing() {
        return showing != null;
    }

    static void clear() {
        showing = null;
        waiting = null;
        age = 0;
    }

    static void render(GuiGraphicsExtractor g, Minecraft mc, float partial) {
        if (showing == null) {
            return;
        }
        double fade = NotifyStyle.envelope(age + partial, FADE_IN, TOTAL, FADE_OUT);
        if (fade <= 0.02) {
            return;
        }
        double grow = Math.min(1.0, (age + partial) / FADE_IN);
        grow = 1.0 - (1.0 - grow) * (1.0 - grow);
        HudLayout lay = HudLayout.current();
        UiScale s = lay.scale();
        s.begin(g, 0, 0);
        try {
            String title = showing.title().getString();
            List<String> rows = NotifyStyle.wrap(UiText.Type.BODY, showing.line().getString(),
                    Math.max(MIN_WRAP, (float) (lay.width() * MAX_SHARE)));
            float lineW = 0;
            for (String row : rows) {
                lineW = Math.max(lineW, UiTextSmooth.width(UiText.Type.BODY, row));
            }
            float contentW = Math.max(UiTextSmooth.width(UiText.Type.TITLE, title), lineW);
            int boxW = (int) Math.ceil(contentW) + 2 * PAD_X;
            int titleCap = UiText.Type.TITLE.capHeight();
            int bodyCap = UiText.Type.BODY.capHeight();
            int boxH = PAD_Y + titleCap + GAP + 1 + GAP + bodyCap + (rows.size() - 1) * ROW + PAD_Y;
            int cx = lay.width() / 2;
            int top = Math.round(lay.height() * (float) CENTRE_Y) - boxH / 2;

            // A soft band behind, then the hairlines drawing out from the middle.
            int half = (int) Math.round(boxW / 2.0 * grow);
            g.fill(cx - half, top, cx + half, top + boxH, NotifyStyle.faded(NotifyStyle.PLATE, fade * 0.7));
            g.fill(cx - half, top, cx + half, top + 1, NotifyStyle.faded(NotifyStyle.RULE, fade));
            g.fill(cx - half, top + boxH - 1, cx + half, top + boxH, NotifyStyle.faded(NotifyStyle.RULE, fade));

            int y = top + PAD_Y;
            UiTextSmooth.drawCentered(g, UiText.Type.TITLE, title, cx, y, NotifyStyle.faded(NotifyStyle.TEXT, fade));
            y += titleCap + GAP;
            g.fill(cx - RULE_W / 2, y, cx + RULE_W / 2, y + 1, NotifyStyle.faded(NotifyStyle.RULE, fade));
            y += 1 + GAP;
            for (String row : rows) {
                UiTextSmooth.drawCentered(g, UiText.Type.BODY, row, cx, y, NotifyStyle.faded(NotifyStyle.TEXT_MUTED, fade));
                y += ROW;
            }
        } finally {
            s.end(g);
        }
    }
}
