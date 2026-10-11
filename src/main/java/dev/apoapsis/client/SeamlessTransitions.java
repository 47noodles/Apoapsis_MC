package dev.apoapsis.client;

import dev.apoapsis.flight.FlightDimensions;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterDimensionTransitionScreenEvent;
import net.neoforged.neoforge.client.DimensionTransitionScreenManager.ReceivingLevelScreenFactory;

/**
 * Ship transitions between the surface and the orbit or Pale dimensions show no loading screen. The level keeps
 * rendering under a non-pause screen, and the client handshake does not depend on the screen, so the screen closes
 * on its first tick. Client only.
 */
public final class SeamlessTransitions {
    private SeamlessTransitions() {}

    /** Called from {@code ApoapsisClient} with the mod event bus. */
    public static void register(IEventBus modBus) {
        modBus.addListener(SeamlessTransitions::onRegisterScreens);
    }

    private static void onRegisterScreens(RegisterDimensionTransitionScreenEvent event) {
        ReceivingLevelScreenFactory factory = SeamlessScreen::new;
        event.registerConditionalEffect(FlightDimensions.ORBIT, Level.OVERWORLD, factory);
        event.registerConditionalEffect(Level.OVERWORLD, FlightDimensions.ORBIT, factory);
        event.registerConditionalEffect(FlightDimensions.ORBIT, FlightDimensions.PALE, factory);
        event.registerConditionalEffect(FlightDimensions.PALE, FlightDimensions.ORBIT, factory);
    }

    /**
     * The transition screen: draws nothing and closes on its first tick. The new level and player already exist when it
     * opens, and the client-loaded handshake runs from ClientPacketListener's own tick, not from this screen.
     */
    static final class SeamlessScreen extends LevelLoadingScreen {
        SeamlessScreen(LevelLoadTracker loadTracker, LevelLoadingScreen.Reason reason) {
            super(loadTracker, reason);
        }

        @Override
        public void tick() {
            this.onClose();
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        }
    }
}
