package dev.apoapsis.client.menu;

import dev.apoapsis.client.ui.UiPalette;
import dev.apoapsis.client.ui.UiScale;
import dev.apoapsis.client.ui.UiText;
import dev.apoapsis.client.ui.UiText.Type;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * The loading screens in the kit's style: the title's sky and planets, and at the lower left one caption, a small line
 * under it, and a thin progress bar. {@link Level} stands in for the world loading and joining screen (and dimension
 * changes that are not seamless), {@link Message} for vanilla's one-line message screens (saving, reading world data),
 * {@link Progress} for the progress screen. Client only.
 */
final class LoadingScreens {
    private static final int BAR_W = 168;

    private LoadingScreens() {}

    /**
     * Draws the caption block. {@code progress} is 0..1, or negative for an unknown length (a sweeping segment).
     * {@code sub} may be null.
     */
    static void caption(GuiGraphicsExtractor g, String title, @Nullable String sub, float progress) {
        UiScale s = UiScale.current();
        int margin = Math.max(24, Math.min(56, Math.round(s.width() * 0.09f)));
        int y = s.height() - 84;
        s.begin(g, 0, 0);
        UiText.draw(g, Type.HEADING, title, margin, y, UiPalette.INK);
        int by = y + Type.HEADING.capHeight() + 14;
        g.fill(margin, by, margin + BAR_W, by + 2, UiPalette.STEEL_700);
        if (progress >= 0) {
            g.fill(margin, by, margin + Math.round(BAR_W * Math.min(1f, progress)), by + 2, UiPalette.BRASS);
        } else {
            double t = (System.nanoTime() / 1e9) % 1.6 / 1.6;
            int seg = 44;
            int x0 = margin + (int) ((BAR_W + seg) * t) - seg;
            g.fill(Math.max(margin, x0), by, Math.min(margin + BAR_W, x0 + seg), by + 2, UiPalette.BRASS);
        }
        if (sub != null) {
            UiText.draw(g, Type.LABEL, sub, margin, by + 10, UiPalette.INK_MUTED);
        }
        s.end(g);
    }

    /** World loading, joining and non-seamless dimension changes. */
    static final class Level extends LevelLoadingScreen {
        private LevelLoadTracker tracker;
        private float smooth;

        Level(LevelLoadTracker tracker, LevelLoadingScreen.Reason reason) {
            super(tracker, reason);
            this.tracker = tracker;
        }

        @Override
        public void update(LevelLoadTracker tracker, LevelLoadingScreen.Reason reason) {
            super.update(tracker, reason);
            this.tracker = tracker;
        }

        @Override
        public void tick() {
            super.tick();
            smooth += (tracker.serverProgress() - smooth) * 0.2f;
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            MenuScene.draw(g);
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            boolean known = tracker.hasProgress();
            String sub = known ? Math.round(smooth * 100) + "%" : null;
            caption(g, Component.translatable("apoapsis.menu.loading.terrain").getString(), sub, known ? smooth : -1f);
        }
    }

    /** One-line message screens: saving, reading world data, loading the game. */
    static final class Message extends GenericMessageScreen {
        Message(Component title) {
            super(title);
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            MenuScene.draw(g);
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            caption(g, getTitle().getString(), null, -1f);
        }
    }

    /** The progress screen: a header, the stage and its percentage. */
    static final class Progress extends ProgressScreen {
        private @Nullable Component header;
        private @Nullable Component stage;
        private int percent;
        private boolean stopped;

        Progress() {
            super(true);
        }

        @Override
        public void progressStart(Component title) {
            super.progressStart(title);
            header = title;
        }

        @Override
        public void progressStage(Component text) {
            super.progressStage(text);
            stage = text;
            percent = 0;
        }

        @Override
        public void progressStagePercentage(int value) {
            super.progressStagePercentage(value);
            percent = value;
        }

        @Override
        public void stop() {
            super.stop();
            stopped = true;
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            MenuScene.draw(g);
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            if (stopped) {
                minecraft.gui.setScreen(null);
                return;
            }
            String title = header != null ? header.getString()
                    : Component.translatable("apoapsis.menu.loading.working").getString();
            String sub = stage != null ? stage.getString() + (percent > 0 ? "  " + percent + "%" : "") : null;
            caption(g, title, sub, percent > 0 ? percent / 100f : -1f);
        }
    }

    /** Reads a vanilla private field once; null if the runtime will not allow it. */
    static @Nullable Object read(Object target, String name) {
        try {
            var f = LevelLoadingScreen.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
