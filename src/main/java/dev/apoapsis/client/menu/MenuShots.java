package dev.apoapsis.client.menu;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import dev.apoapsis.client.ui.UiButton;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.sdl.SDLVideo;
import org.slf4j.Logger;

/**
 * A scripted screenshot run of the menus, for checking them in the game. Active only with the environment variable
 * {@code APOAPSIS_MENU_SHOTS}: {@code title} runs from the title screen (title, keyboard focus, hover, loading and
 * message screens, Options and back), {@code world} waits for a world and shoots the pause menu. Each run steps through
 * 1920x1080 and 1280x720 windows at GUI scale auto and 2, writes {@code imm-menu-<name>-<w>x<h>-gs<n>.png} into
 * run/screenshots, then quits. Client only.
 */
final class MenuShots {
    private static final Logger LOG = LogUtils.getLogger();
    private static final int[][] SIZES = {{1920, 1080}, {1280, 720}};
    private static final int[] SCALES = {0, 2};

    private record Step(int delay, Consumer<Minecraft> action) {}

    private static final List<Step> STEPS = new ArrayList<>();
    private static int stepIndex;
    private static int wait;
    private static int worldTicks;
    private static boolean world;
    private static int curW;
    private static int curH;
    private static int curScale;

    private MenuShots() {}

    static void registerIfRequested() {
        String mode = System.getenv("APOAPSIS_MENU_SHOTS");
        if (mode == null || mode.isBlank()) {
            return;
        }
        world = mode.equals("world");
        build();
        NeoForge.EVENT_BUS.addListener(MenuShots::onTick);
    }

    private static void add(int delay, Consumer<Minecraft> action) {
        STEPS.add(new Step(delay, action));
    }

    private static void build() {
        for (int[] size : SIZES) {
            for (int scale : SCALES) {
                add(10, mc -> {
                    curW = size[0];
                    curH = size[1];
                    curScale = scale;
                    SDLVideo.SDL_SetWindowSize(mc.getWindow().handle(), size[0], size[1]);
                });
                add(20, mc -> {
                    mc.options.guiScale().set(scale);
                    mc.resizeGui();
                });
                if (world) {
                    add(20, mc -> mc.gui.setScreen(new PauseScreen(true)));
                    add(40, mc -> shot(mc, "pause"));
                    add(5, mc -> hover(mc, 3));
                    add(30, mc -> shot(mc, "pause-hover"));
                    add(5, mc -> key(mc, InputConstants.KEY_DOWN, 3));
                    add(20, mc -> shot(mc, "pause-focus"));
                    add(5, mc -> mc.gui.setScreen(null));
                } else {
                    add(20, mc -> mc.gui.setScreen(new TitleScreen()));
                    add(40, mc -> shot(mc, "title"));
                    add(5, mc -> key(mc, InputConstants.KEY_DOWN, 2));
                    add(25, mc -> shot(mc, "title-focus"));
                    add(5, mc -> hover(mc, 3));
                    add(30, mc -> shot(mc, "title-hover"));
                    add(5, mc -> {
                        LevelLoadTracker t = new LevelLoadTracker();
                        t.start(LevelLoadListener.Stage.LOAD_INITIAL_CHUNKS, 100);
                        t.update(LevelLoadListener.Stage.LOAD_INITIAL_CHUNKS, 42, 100);
                        mc.gui.setScreen(new LoadingScreens.Level(t, LevelLoadingScreen.Reason.OTHER));
                    });
                    add(40, mc -> shot(mc, "loading"));
                    add(5, mc -> mc.gui.setScreen(new GenericMessageScreen(Component.literal("Saving world"))));
                    add(15, mc -> shot(mc, "message"));
                }
            }
        }
        if (!world) {
            add(10, mc -> mc.gui.setScreen(new OptionsScreen(mc.gui.screen(), mc.options)));
            add(40, mc -> shot(mc, "options"));
            add(5, mc -> mc.gui.setScreen(new TitleScreen()));
            add(30, mc -> shot(mc, "back"));
        }
        add(10, Minecraft::stop);
    }

    private static void shot(Minecraft mc, String name) {
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        String file = "imm-menu-" + name + "-" + w + "x" + h + "-gs" + curScale + ".png";
        LOG.info("[menu shots] {} (requested {}x{}, window {}x{}, gui {}x{} scale {})", file, curW, curH, w, h,
                mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), mc.getWindow().getGuiScale());
        Screenshot.grab(mc.gameDirectory, file, mc.gameRenderer.mainRenderTarget(), 1, m -> { });
    }

    private static void key(Minecraft mc, int key, int times) {
        Screen s = mc.gui.screen();
        if (s != null) {
            mc.setLastInputType(net.minecraft.client.InputType.KEYBOARD_TAB);
            for (int i = 0; i < times; i++) {
                s.keyPressed(new KeyEvent(81, 1073741905, 0));
            }
        }
    }

    /** Puts the mouse over the n-th kit button of the open screen. */
    private static void hover(Minecraft mc, int n) {
        Screen s = mc.gui.screen();
        if (s == null) {
            return;
        }
        mc.setLastInputType(net.minecraft.client.InputType.MOUSE);
        int count = 0;
        for (var child : s.children()) {
            if (child instanceof UiButton b && count++ == n) {
                double scale = mc.getWindow().getGuiScale();
                double x = (b.getX() + b.getWidth() / 2.0) * scale;
                double y = (b.getY() + b.getHeight() / 2.0) * scale;
                mc.mouseHandler.onMove(mc.getWindow().handle(), x, y, 0, 0);
                return;
            }
        }
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (stepIndex >= STEPS.size()) {
            return;
        }
        if (world) {
            if (mc.level == null || mc.player == null) {
                return;
            }
            if (worldTicks++ < 200) {
                return;
            }
        }
        if (wait > 0) {
            wait--;
            return;
        }
        Step step = STEPS.get(stepIndex++);
        step.action().accept(mc);
        wait = stepIndex < STEPS.size() ? STEPS.get(stepIndex).delay() : 0;
    }
}
