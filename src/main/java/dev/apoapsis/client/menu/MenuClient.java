package dev.apoapsis.client.menu;

import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Swaps vanilla's menu screens for the kit's as they open, through NeoForge's {@link ScreenEvent.Opening}: no mixin.
 * Only the exact vanilla classes are swapped, so a subclass made by another mod (or our own screens, or the seamless
 * transition screen) is left alone. Client only.
 */
public final class MenuClient {
    private MenuClient() {}

    /** Called from {@code ApoapsisClient} with the mod event bus. */
    public static void register(IEventBus modBus) {
        modBus.addListener(PlanetPip::onRegister);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, MenuClient::onOpening);
        MenuShots.registerIfRequested();
    }

    private static void onOpening(ScreenEvent.Opening event) {
        var screen = event.getNewScreen();
        if (screen == null) {
            return;
        }
        Class<?> type = screen.getClass();
        if (type == TitleScreen.class) {
            event.setNewScreen(new ApoTitleScreen());
        } else if (type == PauseScreen.class) {
            event.setNewScreen(new ApoPauseScreen(((PauseScreen) screen).showsPauseMenu()));
        } else if (type == LevelLoadingScreen.class) {
            // Portals keep vanilla's own backdrops; the tracker is private, so read it from the screen being replaced.
            Object tracker = LoadingScreens.read(screen, "loadTracker");
            Object reason = LoadingScreens.read(screen, "reason");
            if (tracker instanceof LevelLoadTracker t && reason == LevelLoadingScreen.Reason.OTHER) {
                event.setNewScreen(new LoadingScreens.Level(t, LevelLoadingScreen.Reason.OTHER));
            }
        } else if (type == GenericMessageScreen.class) {
            event.setNewScreen(new LoadingScreens.Message(screen.getTitle()));
        } else if (type == ProgressScreen.class) {
            event.setNewScreen(new LoadingScreens.Progress());
        }
    }
}
