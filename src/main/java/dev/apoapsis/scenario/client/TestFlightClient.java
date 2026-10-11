package dev.apoapsis.scenario.client;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.scenario.TestFlight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Opens (or creates) the save "Apoapsis Test Flight" the first time the title screen shows in a launch, when the
 * {@code apoapsis.testFlight} property is set. Client only.
 */
@EventBusSubscriber(modid = Apoapsis.MODID, value = Dist.CLIENT)
public final class TestFlightClient {
    /** Save folder and display name. */
    public static final String SAVE_NAME = "Apoapsis Test Flight";
    /** Fixed seed, so the test world is the same on every launch. */
    public static final long SEED = 26032026L;

    private static boolean opened;
    private static boolean pending;

    private TestFlightClient() {}

    @SubscribeEvent
    static void onScreenInit(ScreenEvent.Init.Post event) {
        if (opened || !TestFlight.enabled() || !(event.getScreen() instanceof TitleScreen)) {
            return;
        }
        opened = true;
        pending = true;
    }

    /** Opens the world on the next client tick, once the title screen is the current screen. */
    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (!pending) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.gui.screen() instanceof TitleScreen title)) {
            return;
        }
        pending = false;
        open(mc, title);
    }

    private static void open(Minecraft mc, Screen title) {
        if (mc.getLevelSource().levelExists(SAVE_NAME)) {
            mc.createWorldOpenFlows().openWorld(SAVE_NAME, () -> mc.gui.setScreen(title));
            return;
        }
        LevelSettings settings = new LevelSettings(
                SAVE_NAME, GameType.CREATIVE, LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(
                SAVE_NAME, settings, new WorldOptions(SEED, true, false), WorldPresets::createNormalWorldDimensions, title);
    }
}
