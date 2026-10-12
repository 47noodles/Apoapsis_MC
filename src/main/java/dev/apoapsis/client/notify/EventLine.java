package dev.apoapsis.client.notify;

import dev.apoapsis.client.hud.HudLayout;
import dev.apoapsis.client.ui.UiScale;
import dev.apoapsis.client.ui.UiText;
import dev.apoapsis.client.ui.UiTextSmooth;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * The event line: one calm line at the top centre for a flight event ("Entering orbit around Verdance", "Time warp x5").
 * It fades in, stays about four seconds and fades out. Lines queue, but never more than two are held (the one showing
 * and one waiting; a newer line replaces the one waiting), and the one showing gives way early once another waits.
 * Client only.
 */
final class EventLine {
    /** Ticks a line lasts, fades included. */
    static final int TOTAL = 90;
    private static final int FADE_IN = 6;
    private static final int FADE_OUT = 14;
    private static final int FADE_START = TOTAL - FADE_OUT;
    /** The least a line shows before it gives way to a waiting one. */
    private static final int MIN_SHOW = 40;
    private static final int MAX_HELD = 2;
    private static final int PAD_X = 8;
    private static final int PAD_Y = 4;
    private static final int RULE_W = 2;
    /** The plate height for one row; each further row adds {@link #ROW}. */
    private static final int HEIGHT = 16;
    private static final int ROW = 11;

    private record Line(Component text, boolean warning) {}

    private static final Deque<Line> QUEUE = new ArrayDeque<>();
    private static int age;

    private EventLine() {}

    static void push(Component text, boolean warning) {
        Line last = QUEUE.peekLast();
        if (last != null && last.text().equals(text) && QUEUE.size() == 1 && age < FADE_START) {
            age = Math.min(age, FADE_IN); // the same line again: keep it up a little longer
            return;
        }
        if (last != null && last.text().equals(text)) {
            return;
        }
        if (QUEUE.size() >= MAX_HELD) {
            QUEUE.pollLast();
        }
        QUEUE.addLast(new Line(text, warning));
        if (QUEUE.size() == 1) {
            age = 0;
        }
    }

    /** One client tick. */
    static void tick() {
        if (QUEUE.isEmpty()) {
            return;
        }
        age++;
        if (QUEUE.size() > 1 && age >= MIN_SHOW && age < FADE_START) {
            age = FADE_START; // give way to the waiting line
        }
        if (age >= TOTAL) {
            QUEUE.pollFirst();
            age = 0;
        }
    }

    /** The line while it is showing and not yet fading out, for the maps and containers. */
    static @Nullable Component current() {
        Line line = QUEUE.peekFirst();
        return line != null && age < FADE_START ? line.text() : null;
    }

    static void clear() {
        QUEUE.clear();
        age = 0;
    }

    static void render(GuiGraphicsExtractor g, Minecraft mc, float partial) {
        Line line = QUEUE.peekFirst();
        if (line == null) {
            return;
        }
        double fade = NotifyStyle.envelope(age + partial, FADE_IN, TOTAL, FADE_OUT);
        if (fade <= 0.02) {
            return;
        }
        HudLayout lay = HudLayout.current();
        UiScale s = lay.scale();
        s.begin(g, 0, 0);
        try {
            // One row normally; a line longer than half the view wraps to a second row, so it never reaches the first
            // chip at the top left or the regime label at the top right.
            List<String> rows = NotifyStyle.wrap(UiText.Type.BODY, line.text().getString(),
                    Math.max(120, lay.width() / 2 - 2 * PAD_X));
            float textW = 0;
            for (String row : rows) {
                textW = Math.max(textW, UiTextSmooth.width(UiText.Type.BODY, row));
            }
            int w = (int) Math.ceil(textW) + RULE_W + 2 * PAD_X;
            int h = HEIGHT + (rows.size() - 1) * ROW;
            int x = lay.width() / 2 - w / 2;
            int y = lay.top();
            int rule = line.warning() ? NotifyStyle.RULE_WARNING : NotifyStyle.RULE;
            g.fill(x, y, x + w, y + h, NotifyStyle.faded(NotifyStyle.PLATE, fade));
            g.fill(x, y, x + RULE_W, y + h, NotifyStyle.faded(rule, fade));
            int ty = y + PAD_Y;
            float tx = x + RULE_W + PAD_X;
            for (String row : rows) {
                UiTextSmooth.draw(g, UiText.Type.BODY, row, tx, ty, NotifyStyle.faded(NotifyStyle.TEXT, fade));
                ty += ROW;
            }
        } finally {
            s.end(g);
        }
    }
}
