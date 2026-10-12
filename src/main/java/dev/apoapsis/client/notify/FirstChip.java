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

/**
 * The "first" chip: a small card at the top left when the player records a first (First Flight, Stable Orbit...). A
 * teal rule, the word First, the first's name and one sentence. It replaces the vanilla advancement toast; it sits at the
 * top left, away from the event line at the top centre and the regime label and guide panel at the top right. Chips that
 * arrive together show one after another. Client only.
 */
final class FirstChip {
    /** Ticks a chip lasts, fades included: six seconds. */
    static final int TOTAL = 120;
    private static final int FADE_IN = 8;
    private static final int FADE_OUT = 20;
    private static final int PAD = 7;
    private static final int RULE_W = 2;
    private static final int GAP = 4;
    private static final int ROW = 12;
    /** The widest the chip text runs, in UI units: it stays left of the event line and the Age banner. */
    private static final int MAX_TEXT = 130;

    private record Chip(Component title, Component line) {}

    private static final Deque<Chip> QUEUE = new ArrayDeque<>();
    private static int age;

    private FirstChip() {}

    static void show(Component title, Component line) {
        QUEUE.addLast(new Chip(title, line));
        if (QUEUE.size() == 1) {
            age = 0;
        }
    }

    static void tick() {
        if (QUEUE.isEmpty()) {
            return;
        }
        if (++age >= TOTAL) {
            QUEUE.pollFirst();
            age = 0;
        }
    }

    static void clear() {
        QUEUE.clear();
        age = 0;
    }

    static void render(GuiGraphicsExtractor g, Minecraft mc, float partial) {
        Chip chip = QUEUE.peekFirst();
        if (chip == null) {
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
            String word = Component.translatable("apoapsis.notify.first").getString();
            String title = chip.title().getString();
            List<String> rows = NotifyStyle.wrap(UiText.Type.BODY, chip.line().getString(), MAX_TEXT);
            float textW = Math.max(UiTextSmooth.width(UiText.Type.LABEL, word), UiTextSmooth.width(UiText.Type.BODY, title));
            for (String row : rows) {
                textW = Math.max(textW, UiTextSmooth.width(UiText.Type.BODY, row));
            }
            int labelCap = UiText.Type.LABEL.capHeight();
            int bodyCap = UiText.Type.BODY.capHeight();
            int w = RULE_W + PAD + (int) Math.ceil(textW) + PAD;
            int h = 2 * PAD + labelCap + GAP + bodyCap + GAP + bodyCap + (rows.size() - 1) * ROW;
            int y = lay.top();
            int x = lay.leftAt(y);
            g.fill(x, y, x + w, y + h, NotifyStyle.faded(NotifyStyle.PLATE, fade));
            g.fill(x, y, x + RULE_W, y + h, NotifyStyle.faded(NotifyStyle.RULE_FIRST, fade));
            float tx = x + RULE_W + PAD;
            int ty = y + PAD;
            UiTextSmooth.draw(g, UiText.Type.LABEL, word, tx, ty, NotifyStyle.faded(NotifyStyle.RULE_FIRST, fade));
            ty += labelCap + GAP;
            UiTextSmooth.draw(g, UiText.Type.BODY, title, tx, ty, NotifyStyle.faded(NotifyStyle.TEXT, fade));
            ty += bodyCap + GAP;
            for (String row : rows) {
                UiTextSmooth.draw(g, UiText.Type.BODY, row, tx, ty, NotifyStyle.faded(NotifyStyle.TEXT_MUTED, fade));
                ty += ROW;
            }
        } finally {
            s.end(g);
        }
    }
}
