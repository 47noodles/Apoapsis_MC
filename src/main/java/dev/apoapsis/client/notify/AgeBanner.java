package dev.apoapsis.client.notify;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

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
    /** The widest the sentence wraps, as a share of the GUI width: keeps clear of the guide panel at the right. */
    private static final double MAX_SHARE = 0.42;
    private static final float TITLE_SCALE = 2.0f;
    private static final int PAD_Y = 9;
    private static final int GAP = 5;
    private static final int ROW = 10;
    private static final int RULE_W = 28;
    private static final int PAD_X = 22;

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
        Font font = mc.font;
        String title = showing.title().getString().toUpperCase(Locale.ROOT);
        int wrap = (int) Math.max(120, g.guiWidth() * MAX_SHARE);
        List<FormattedCharSequence> rows = font.split(showing.line(), wrap);
        int lineW = 0;
        for (FormattedCharSequence row : rows) {
            lineW = Math.max(lineW, font.width(row));
        }
        int titleW = (int) Math.ceil(font.width(title) * TITLE_SCALE);
        int titleH = (int) Math.ceil(8 * TITLE_SCALE);
        int contentW = Math.max(titleW, lineW);
        int boxW = contentW + 2 * PAD_X;
        int boxH = PAD_Y + titleH + GAP + 1 + GAP + rows.size() * ROW + PAD_Y;
        int cx = g.guiWidth() / 2;
        int top = (int) (g.guiHeight() * CENTRE_Y) - boxH / 2;

        // A soft band behind, then the hairlines drawing out from the middle.
        int half = (int) Math.round(boxW / 2.0 * grow);
        g.fill(cx - half, top, cx + half, top + boxH, NotifyStyle.faded(NotifyStyle.PLATE, fade * 0.7));
        g.fill(cx - half, top, cx + half, top + 1, NotifyStyle.faded(NotifyStyle.RULE, fade));
        g.fill(cx - half, top + boxH - 1, cx + half, top + boxH, NotifyStyle.faded(NotifyStyle.RULE, fade));

        int y = top + PAD_Y;
        g.pose().pushMatrix();
        g.pose().translate(cx - titleW / 2.0f, y);
        g.pose().scale(TITLE_SCALE, TITLE_SCALE);
        g.text(font, title, 0, 0, NotifyStyle.faded(NotifyStyle.TEXT, fade), true);
        g.pose().popMatrix();
        y += titleH + GAP;
        g.fill(cx - RULE_W / 2, y, cx + RULE_W / 2, y + 1, NotifyStyle.faded(NotifyStyle.RULE, fade));
        y += 1 + GAP;
        for (FormattedCharSequence row : rows) {
            g.text(font, row, cx - font.width(row) / 2, y, NotifyStyle.faded(NotifyStyle.TEXT_MUTED, fade), true);
            y += ROW;
        }
    }
}
