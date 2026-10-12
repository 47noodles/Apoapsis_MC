package dev.apoapsis.client.audio;

import com.mojang.logging.LogUtils;
import dev.apoapsis.client.ClientState;
import dev.apoapsis.client.audio.MusicLogic.Place;
import dev.apoapsis.client.audio.MusicLogic.Track;
import dev.apoapsis.client.map.MapScreen;
import dev.apoapsis.client.map3d.Map3DScreen;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.FlightDimensions;
import dev.apoapsis.ship.ShipEntity;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * The space music: Galactic Temple as the climb ends in orbit, then calm tracks with long silences, First Light
 * Particles at Pale, Starfield Romance on the title screen. This class reads the game once per client tick and feeds
 * {@link MusicLogic}, which decides; it plays the tracks as {@link MusicVoice}s and holds vanilla music back through
 * NeoForge's {@link SelectMusicEvent} while our music owns the soundscape. The state lives here, not in any level, so
 * it carries across the 2 km dimension swap. Client only.
 */
public final class MusicDirector implements MusicLogic.Sink {
    private static final Logger LOG = LogUtils.getLogger();
    private static final MusicDirector INSTANCE = new MusicDirector();
    /** Ticks after a track starts when it is checked against the sound engine and logged once. */
    private static final int CHECK_TICKS = 20;

    private final MusicLogic logic = new MusicLogic(this, new Random());
    private MusicVoice active;
    private long tick;
    private long playedAt;
    private long checkAt = -1;
    private boolean failed;
    private boolean titleHint;
    private boolean wasSuppressed;
    private int blocked;
    private boolean mapWas;

    private MusicDirector() {}

    /** Called from {@code ApoapsisClient}: hooks the per-tick update and vanilla music's selection. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(INSTANCE::onTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, INSTANCE::onSelectMusic);
    }

    /**
     * A title-like screen calls this when it opens, so the title music starts at once instead of after the short wait
     * the director gives a missing level. Optional: with no level loaded the director plays the title music anyway,
     * whatever screen class is up, so a replaced title screen needs no hook.
     */
    public static void titleScreenOpened() {
        INSTANCE.titleHint = true;
    }

    /**
     * What a level swap stops (see {@code MinecraftSoundMixin}): every sound category but MUSIC, so the 2 km transition
     * between the surface and orbit does not cut the music.
     */
    public static void stopForLevelSwap(SoundManager sounds) {
        for (SoundSource source : SoundSource.values()) {
            if (source != SoundSource.MUSIC) {
                sounds.stop(null, source);
            }
        }
    }

    private void onTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        tick++;

        if (active != null && tick - playedAt > 5 && !mc.getSoundManager().isActive(active)) {
            active = null;
            logic.trackEnded(tick);
        }
        if (failed) {
            failed = false;
            logic.trackEnded(tick);
        }

        Place place = Place.NO_LEVEL;
        boolean piloting = false;
        double altitude = 0.0;
        if (mc.level != null && mc.player != null) {
            titleHint = false;
            ResourceKey<Level> dim = mc.level.dimension();
            if (dim == FlightDimensions.ORBIT) {
                place = bodyInOrbit() == BodyId.PALE ? Place.ORBIT_PALE : Place.ORBIT_VERDANCE;
            } else if (dim == FlightDimensions.PALE) {
                place = Place.PALE_SURFACE;
            } else {
                place = Place.SURFACE;
                piloting = mc.player.getVehicle() instanceof ShipEntity;
                altitude = mc.player.getY() - FlightDimensions.seaLevel(dim);
            }
        }
        Screen screen = mc.gui.screen();
        boolean map = screen instanceof MapScreen || screen instanceof Map3DScreen;
        if (map != mapWas) {
            mapWas = map;
            note("map " + (map ? "opened" : "closed") + ", playing " + (active != null ? active.track().event : "nothing"));
        }
        logic.update(tick, place, piloting, altitude, map, titleHint);

        if (logic.takeRelease(tick) && logic.releaseResetsDelay()) {
            mc.getMusicManager().stopPlaying();
            note("vanilla music released; its next track waits as normal");
        }
        boolean suppressed = logic.suppressVanilla() || logic.holdNewVanilla();
        if (suppressed != wasSuppressed) {
            wasSuppressed = suppressed;
            note(suppressed ? "vanilla music held back" : "vanilla music allowed");
        }
        if (checkAt >= 0 && tick >= checkAt) {
            checkAt = -1;
            logCheck(mc);
        }
    }

    private static BodyId bodyInOrbit() {
        var orbit = ClientState.orbit;
        return orbit != null ? BodyId.byOrdinal(orbit.body()) : BodyId.VERDANCE;
    }

    /** Vanilla's music manager asks what to play: say nothing while our music owns the soundscape. */
    private void onSelectMusic(SelectMusicEvent event) {
        if (logic.suppressVanilla() || (logic.holdNewVanilla() && event.getPlayingMusic() == null)) {
            event.overrideMusic(null);
            blocked++;
        }
    }

    @Override
    public void play(Track track, int fadeInTicks, boolean loop) {
        MusicVoice voice = new MusicVoice(track, fadeInTicks, loop);
        SoundEngine.PlayResult result = Minecraft.getInstance().getSoundManager().play(voice);
        if (result == SoundEngine.PlayResult.NOT_STARTED) {
            note("could not start " + track.event + " (no sound engine, or the sound is missing)");
            active = null;
            failed = true;
            return;
        }
        active = voice;
        playedAt = tick;
        checkAt = tick + CHECK_TICKS;
        note("play " + track.event + " fadeIn=" + fadeInTicks / 20.0 + "s loop=" + loop + " " + result);
    }

    @Override
    public void fadeOut(int ticks) {
        if (active != null) {
            note("fade out " + active.track().event + " over " + ticks / 20.0 + "s");
            active.fadeOut(ticks);
            active = null;
        }
    }

    @Override
    public void note(String line) {
        LOG.info("[music] {}", line);
    }

    /** The one-shot proof that a track is really playing in the sound engine and vanilla's is not. */
    private void logCheck(Minecraft mc) {
        if (active == null) {
            return;
        }
        LOG.info("[music] check {}: soundEngineActive={} gain={} vanillaMusic={} vanillaSelectionsBlocked={} musicSlider={} mode={}",
                active.track().event,
                mc.getSoundManager().isActive(active),
                String.format("%.2f", active.level()),
                mc.getMusicManager().getCurrentMusicTranslationKey(),
                blocked,
                String.format("%.2f", mc.options.getFinalSoundSourceVolume(SoundSource.MUSIC)),
                logic.mode());
    }
}
