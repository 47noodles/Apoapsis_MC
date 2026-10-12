package dev.apoapsis.client.hud;

import dev.apoapsis.client.ClientState;
import dev.apoapsis.client.ship.PilotKeys;
import dev.apoapsis.client.ui.UiKeycap;
import dev.apoapsis.client.ui.UiPalette;
import dev.apoapsis.client.ui.UiPanel;
import dev.apoapsis.client.ui.UiText;
import dev.apoapsis.client.ui.UiTextSmooth;
import dev.apoapsis.ship.SasMode;
import dev.apoapsis.ship.ShipEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * One quiet row of keycaps above the bottom-left readouts, only while it helps: for the first minute and a half after
 * boarding, and for a while after the ship first reaches orbit. A hint is dropped as soon as the player uses its control,
 * and the whole row fades out at the end of its window. The chase view hint is the exception: while the ship sits on the
 * pad it stays, larger and with teal brackets, until the player has looked from outside or lifted off. Every key shown
 * is the player's own binding. Client only.
 */
public final class HintRow {
    /** Ticks after boarding that the controls row shows. */
    private static final int BOARD_WINDOW = 90 * 20;
    /** Ticks the chase hint may stay on the pad, in the worst case. */
    private static final int PAD_WINDOW = 5 * 60 * 20;
    /** Ticks after entering orbit that the map and warp hints show. */
    private static final int ORBIT_WINDOW = 40 * 20;
    private static final int FADE_TICKS = 60;
    private static final int GAP = 12;
    private static final int PAD_X = 7;
    private static final int PROMINENT_H = UiKeycap.HEIGHT + 8;

    private enum Id { CHASE, THROTTLE, SAS, MAP, WARP }

    private record Hint(Id id, KeyMapping[] keys, String captionKey) {}

    private static boolean piloting;
    private static long boardTick;
    private static long orbitTick = -1;
    private static boolean seenFirstBoarding;
    private static SasMode sasAtBoarding = SasMode.OFF;
    private static final boolean[] USED = new boolean[Id.values().length];

    private HintRow() {}

    /** The player opened a screen: opening the map counts as using the map. Called by the flight HUD every frame. */
    public static void screenOpen(Screen screen) {
        if (screen != null && screen.getClass().getSimpleName().startsWith("Map")) {
            USED[Id.MAP.ordinal()] = true;
        }
    }

    /** Notes what the player has used. Call once per frame while piloting, before {@link #draw}. */
    public static void observe(Minecraft mc, ShipEntity ship, boolean orbit, float warp) {
        long now = ClientState.clientTicks;
        if (!piloting) {
            piloting = true;
            boardTick = now;
            orbitTick = -1;
            sasAtBoarding = ship.getSasMode();
        }
        if (orbit && orbitTick < 0) {
            orbitTick = now;
        } else if (!orbit) {
            orbitTick = -1;
        }
        if (!mc.options.getCameraType().isFirstPerson()) {
            USED[Id.CHASE.ordinal()] = true;
        }
        if (ship.getThrottle() > 0.01f) {
            USED[Id.THROTTLE.ordinal()] = true;
        }
        if (ship.getSasMode() != sasAtBoarding) {
            USED[Id.SAS.ordinal()] = true;
        }
        if (warp > 1) {
            USED[Id.WARP.ordinal()] = true;
        }
    }

    /** The player left the ship: the next boarding starts a new window. */
    public static void left() {
        if (piloting) {
            piloting = false;
            seenFirstBoarding = true;
        }
    }

    /**
     * Draws the row with its left edge at {@code x} and the keycaps' bottom at {@code bottomY}; a hint that would pass
     * {@code maxRight} is left out. Draws nothing when no hint applies.
     */
    public static void draw(GuiGraphicsExtractor g, ShipEntity ship, int x, int bottomY, int maxRight) {
        long now = ClientState.clientTicks;
        long sinceBoard = now - boardTick;
        int boardWindow = seenFirstBoarding ? BOARD_WINDOW : BOARD_WINDOW + 30 * 20;
        boolean onPad = ship.isLanded() && !USED[Id.THROTTLE.ordinal()];
        float boardFade = fade(sinceBoard, boardWindow);
        float chaseFade = USED[Id.CHASE.ordinal()] ? 0f : fade(sinceBoard, onPad ? PAD_WINDOW : boardWindow);
        float orbitFade = orbitTick < 0 ? 0f : fade(now - orbitTick, ORBIT_WINDOW);
        float mapFade = Math.max(orbitFade, boardFade);

        List<Hint> hints = new ArrayList<>();
        List<Float> fades = new ArrayList<>();
        add(hints, fades, Id.CHASE, chaseFade, "apoapsis.hud.hint.chase", PilotKeys.chaseViewKey());
        add(hints, fades, Id.THROTTLE, boardFade, "apoapsis.hud.hint.throttle", PilotKeys.THROTTLE_UP,
                PilotKeys.THROTTLE_DOWN);
        add(hints, fades, Id.SAS, boardFade, "apoapsis.hud.hint.sas", PilotKeys.SAS);
        add(hints, fades, Id.MAP, mapFade, "apoapsis.hud.hint.map", PilotKeys.SYSTEM_MAP);
        add(hints, fades, Id.WARP, orbitFade, "apoapsis.hud.hint.warp", PilotKeys.WARP_DOWN, PilotKeys.WARP_UP);

        int cx = x;
        for (int i = 0; i < hints.size(); i++) {
            Hint h = hints.get(i);
            float f = fades.get(i);
            String caption = Component.translatable(h.captionKey()).getString();
            boolean prominent = h.id() == Id.CHASE && onPad;
            int w = prominent ? prominentWidth(h, caption) : HudKeycap.hintWidth(h.keys(), caption);
            if (cx + w > maxRight) {
                continue;
            }
            if (prominent) {
                float pulse = 0.75f + 0.25f * (float) Math.sin(now * 0.12);
                cx += drawProminent(g, h, caption, cx, bottomY, f, pulse) + GAP;
            } else {
                cx += HudKeycap.hint(g, cx, bottomY - UiKeycap.HEIGHT, h.keys(), caption, UiPalette.INK, f)
                        + GAP;
            }
        }
    }

    private static void add(List<Hint> hints, List<Float> fades, Id id, float fade, String captionKey,
                            KeyMapping... keys) {
        if (fade > 0f && !USED[id.ordinal()]) {
            hints.add(new Hint(id, keys, captionKey));
            fades.add(fade);
        }
    }

    /** 1 inside the window, fading to 0 over its last {@link #FADE_TICKS}; 0 after it. */
    private static float fade(long age, long window) {
        if (age < 0 || age >= window) {
            return 0f;
        }
        return Math.min(1f, (window - age) / (float) FADE_TICKS);
    }

    private static int prominentWidth(Hint h, String caption) {
        return PAD_X + HudKeycap.hintWidth(h.keys(), "") - 5 + 4 + Math.round(UiTextSmooth.width(UiText.Type.BODY, caption))
                + PAD_X;
    }

    private static int drawProminent(GuiGraphicsExtractor g, Hint h, String caption, int x, int bottomY, float fade,
                                     float pulse) {
        int w = prominentWidth(h, caption);
        int y0 = bottomY - PROMINENT_H;
        g.fill(x, y0, x + w, bottomY, UiPalette.fade(UiPalette.alpha(UiPalette.STEEL_950, 0xC0), fade));
        UiPanel.brackets(g, x, y0, x + w, bottomY, 4, UiPalette.fade(UiPalette.SIGNAL, fade * pulse));
        int cx = x + PAD_X;
        for (int i = 0; i < h.keys().length; i++) {
            cx += HudKeycap.cap(g, cx, y0 + 4, UiKeycap.label(h.keys()[i]), fade) + 2;
        }
        UiTextSmooth.draw(g, UiText.Type.BODY, caption, cx + 2, y0 + (PROMINENT_H - UiText.Type.BODY.capHeight()) / 2 - 1,
                UiPalette.fade(UiPalette.INK, fade));
        return w;
    }
}
