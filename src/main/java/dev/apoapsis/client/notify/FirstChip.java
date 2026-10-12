package dev.apoapsis.client.notify;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

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
    private static final float SMALL = 0.8f;
    private static final int PAD = 6;
    private static final int RULE_W = 2;
    private static final int ROW = 11;
    /** The widest the chip text runs, GUI px: it stays left of the event line and the Age banner. */
    private static final int MAX_TEXT = 105;

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
        Font font = mc.font;
        Component label = Component.translatable("apoapsis.notify.first");
        List<FormattedCharSequence> rows = font.split(chip.line(), (int) (MAX_TEXT / SMALL));
        int labelW = (int) Math.ceil(font.width(label) * SMALL);
        int lineW = 0;
        for (FormattedCharSequence row : rows) {
            lineW = Math.max(lineW, (int) Math.ceil(font.width(row) * SMALL));
        }
        int textW = Math.max(font.width(chip.title()), Math.max(labelW, lineW));
        int w = RULE_W + PAD + textW + PAD;
        int h = PAD - 1 + 9 + 11 + (int) Math.ceil(rows.size() * 9 * SMALL) + PAD - 1;
        int x = NotifyStyle.EDGE;
        int y = NotifyStyle.EDGE;
        g.fill(x, y, x + w, y + h, NotifyStyle.faded(NotifyStyle.PLATE, fade));
        g.fill(x, y, x + RULE_W, y + h, NotifyStyle.faded(NotifyStyle.RULE_FIRST, fade));
        int tx = x + RULE_W + PAD;
        int ty = y + PAD - 1;
        small(g, font, label, tx, ty, NotifyStyle.faded(NotifyStyle.RULE_FIRST, fade));
        g.text(font, chip.title(), tx, ty + 9, NotifyStyle.faded(NotifyStyle.TEXT, fade), true);
        int ry = ty + 9 + ROW;
        for (FormattedCharSequence row : rows) {
            g.pose().pushMatrix();
            g.pose().translate(tx, ry);
            g.pose().scale(SMALL, SMALL);
            g.text(font, row, 0, 0, NotifyStyle.faded(NotifyStyle.TEXT_MUTED, fade), true);
            g.pose().popMatrix();
            ry += (int) Math.ceil(9 * SMALL);
        }
    }

    private static void small(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int argb) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(SMALL, SMALL);
        g.text(font, text, 0, 0, argb, true);
        g.pose().popMatrix();
    }
}
