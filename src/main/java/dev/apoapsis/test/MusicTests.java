package dev.apoapsis.test;

import dev.apoapsis.client.audio.MusicLogic;
import dev.apoapsis.client.audio.MusicLogic.Mode;
import dev.apoapsis.client.audio.MusicLogic.Place;
import dev.apoapsis.client.audio.MusicLogic.Track;
import dev.apoapsis.test.ApoapsisGameTest.ShipCleanup;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Tests of {@link MusicLogic}, the space music director's state machine. No client needed. */
final class MusicTests {
    private MusicTests() {}

    /** Registers the tests. */
    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        event.registerTest(TestSupport.id("music_title_then_surface"),
                new ApoapsisGameTest("music_title_then_surface", environment, 40, MusicTests::titleThenSurface));
        event.registerTest(TestSupport.id("music_entering_space_then_quiet_orbit"),
                new ApoapsisGameTest("music_entering_space_then_quiet_orbit", environment, 40, MusicTests::enteringSpace));
        event.registerTest(TestSupport.id("music_pale_and_return"),
                new ApoapsisGameTest("music_pale_and_return", environment, 40, MusicTests::paleAndReturn));
        event.registerTest(TestSupport.id("music_survives_the_level_swap"),
                new ApoapsisGameTest("music_survives_the_level_swap", environment, 40, MusicTests::levelSwap));
    }

    /** Records what the logic asks of the sound side. */
    private static final class Rec implements MusicLogic.Sink {
        final List<String> calls = new ArrayList<>();
        final List<Track> plays = new ArrayList<>();
        int fadeIns;
        boolean loop;
        int fadeOuts;

        @Override
        public void play(Track track, int fadeInTicks, boolean loop) {
            calls.add("play " + track);
            plays.add(track);
            fadeIns = fadeInTicks;
            this.loop = loop;
        }

        @Override
        public void fadeOut(int ticks) {
            calls.add("fade");
            fadeOuts++;
        }

        @Override
        public void note(String line) {}
    }

    /** One logic with a fixed seed, and a tick counter that only moves forward. */
    private static final class Rig {
        final Rec rec = new Rec();
        final MusicLogic logic = new MusicLogic(rec, new Random(7));
        long tick;

        void step(Place place, boolean piloting, double alt, boolean map, int ticks) {
            for (int i = 0; i < ticks; i++) {
                tick++;
                logic.update(tick, place, piloting, alt, map, false);
            }
        }

        /** Steps in orbit until the number of plays reaches {@code n}, at most 6000 ticks; returns the ticks used. */
        long untilPlays(Place place, int n) {
            long start = tick;
            while (rec.plays.size() < n && tick - start < 6000) {
                step(place, true, 0, false, 1);
            }
            return tick - start;
        }
    }

    /** With no level the title music loops; loading into a world fades it out and vanilla music is not reset. */
    private static void titleThenSurface(GameTestHelper helper, ShipCleanup ships) {
        Rig r = new Rig();
        r.tick++;
        r.logic.update(r.tick, Place.NO_LEVEL, false, 0, false, true);
        helper.assertTrue(r.logic.mode() == Mode.TITLE, "mode is " + r.logic.mode() + ", expected TITLE");
        r.step(Place.NO_LEVEL, false, 0, false, 5);
        helper.assertTrue(r.rec.plays.equals(List.of(Track.STARFIELD_ROMANCE)) && r.rec.loop,
                "title plays Starfield Romance looping once: " + r.rec.calls);
        helper.assertTrue(r.logic.suppressVanilla(), "vanilla menu music is held back on the title screen");
        r.step(Place.SURFACE, false, 70, false, 1);
        helper.assertTrue(r.logic.mode() == Mode.SURFACE && r.rec.fadeOuts == 1, "title fades out in the world");
        boolean released = false;
        for (int i = 0; i < 120 && !released; i++) {
            r.step(Place.SURFACE, false, 70, false, 1);
            released = r.logic.takeRelease(r.tick);
        }
        helper.assertTrue(released && !r.logic.releaseResetsDelay() && !r.logic.suppressVanilla(),
                "vanilla returns after the fade, without a long wait");
        helper.succeed();
    }

    /** Climb, Galactic Temple once at the edge of space, map toggles that never restart it, then calm tracks 1 to 3 minutes apart. */
    private static void enteringSpace(GameTestHelper helper, ShipCleanup ships) {
        Rig r = new Rig();
        r.step(Place.SURFACE, true, 20, false, 100);
        helper.assertTrue(r.logic.mode() == Mode.SURFACE, "on the pad the mode is " + r.logic.mode());
        r.step(Place.SURFACE, true, 800, false, 5);
        helper.assertTrue(r.logic.mode() == Mode.ASCENT && r.logic.holdNewVanilla() && r.rec.plays.isEmpty(),
                "the climb holds new vanilla tracks and plays none of ours: " + r.logic.mode());
        r.step(Place.ORBIT_VERDANCE, true, 0, false, 1);
        helper.assertTrue(r.logic.mode() == Mode.ENTRY, "entering orbit is ENTRY: " + r.logic.mode());
        helper.assertTrue(r.rec.plays.equals(List.of(Track.GALACTIC_TEMPLE)) && !r.rec.loop && r.rec.fadeIns >= 120,
                "Galactic Temple once with a slow fade-in: " + r.rec.calls + " fadeIn " + r.rec.fadeIns);
        helper.assertTrue(r.logic.suppressVanilla(), "vanilla music is held back in space");
        for (int i = 0; i < 6; i++) {
            r.step(Place.ORBIT_VERDANCE, true, 0, i % 2 == 0, 30);
        }
        helper.assertTrue(r.rec.plays.size() == 1, "map toggles restarted a track: " + r.rec.calls);
        r.logic.trackEnded(r.tick);
        helper.assertTrue(r.logic.mode() == Mode.ORBIT, "after the entry track the mode is " + r.logic.mode());
        long gap = r.untilPlays(Place.ORBIT_VERDANCE, 2);
        helper.assertTrue(r.rec.plays.size() == 2 && gap >= MusicLogic.SILENCE_MIN
                        && gap <= MusicLogic.SILENCE_MIN + MusicLogic.SILENCE_SPAN + 1,
                "second track after " + gap + " ticks of silence: " + r.rec.calls);
        Track second = r.rec.plays.get(1);
        helper.assertTrue(second != Track.GALACTIC_TEMPLE, "quiet orbit replayed the entry track");
        r.logic.trackEnded(r.tick);
        r.untilPlays(Place.ORBIT_VERDANCE, 3);
        helper.assertTrue(r.rec.plays.size() == 3, "no third track: " + r.rec.calls);
        Track third = r.rec.plays.get(2);
        helper.assertTrue(third != second && third != Track.GALACTIC_TEMPLE,
                "calm tracks repeat or include the entry track: " + r.rec.plays);
        helper.succeed();
    }

    /** First Light at Pale, kept across the landing; coming home to Verdance fades out and returns vanilla music later. */
    private static void paleAndReturn(GameTestHelper helper, ShipCleanup ships) {
        Rig r = new Rig();
        r.step(Place.SURFACE, true, 20, false, 5);
        r.step(Place.ORBIT_VERDANCE, true, 0, false, 1);
        r.logic.trackEnded(r.tick);
        r.untilPlays(Place.ORBIT_VERDANCE, 2);
        Track calm = r.logic.current();
        helper.assertTrue(calm != null && calm != Track.GALACTIC_TEMPLE, "a calm track plays in orbit: " + r.rec.calls);
        r.step(Place.ORBIT_PALE, true, 0, false, 1);
        helper.assertTrue(r.logic.mode() == Mode.PALE_APPROACH, "Pale orbit mode is " + r.logic.mode());
        helper.assertTrue(r.logic.current() == Track.FIRST_LIGHT_PARTICLES,
                "Pale approach switches to First Light: " + r.rec.calls);
        r.step(Place.ORBIT_PALE, true, 0, false, 300);
        int playsBefore = r.rec.plays.size();
        r.step(Place.PALE_SURFACE, true, 400, false, 1);
        r.step(Place.PALE_SURFACE, false, 3, false, 10);
        helper.assertTrue(r.logic.mode() == Mode.PALE && r.rec.plays.size() == playsBefore,
                "landing on Pale restarted the track: " + r.rec.calls);
        helper.assertTrue(r.logic.current() == Track.FIRST_LIGHT_PARTICLES, "First Light plays on Pale: " + r.logic.current());
        r.step(Place.ORBIT_PALE, true, 0, false, 1);
        r.step(Place.ORBIT_VERDANCE, true, 0, false, 1);
        helper.assertTrue(r.logic.mode() == Mode.ORBIT, "returning to Verdance orbit is " + r.logic.mode());
        r.step(Place.SURFACE, true, 2000, false, 1);
        helper.assertTrue(r.logic.mode() == Mode.SURFACE, "re-entry is not another ascent: " + r.logic.mode());
        helper.assertTrue(r.logic.suppressVanilla(), "vanilla stays quiet while the music fades out");
        boolean released = false;
        for (int i = 0; i < 120 && !released; i++) {
            r.step(Place.SURFACE, true, 1500, false, 1);
            released = r.logic.takeRelease(r.tick);
        }
        helper.assertTrue(released && r.logic.releaseResetsDelay(), "vanilla music returns later as normal");
        helper.assertTrue(r.logic.current() == null, "our music is gone on Verdance");
        helper.succeed();
    }

    /** A level missing for under two seconds, as in the 2 km swap, changes nothing; a long absence is the title screen. */
    private static void levelSwap(GameTestHelper helper, ShipCleanup ships) {
        Rig r = new Rig();
        r.step(Place.ORBIT_VERDANCE, true, 0, false, 200);
        int calls = r.rec.calls.size();
        Mode before = r.logic.mode();
        r.step(Place.NO_LEVEL, false, 0, false, 20);
        r.step(Place.ORBIT_VERDANCE, true, 0, false, 5);
        helper.assertTrue(r.logic.mode() == before && r.rec.calls.size() == calls,
                "a short gap without a level changed the music: " + r.rec.calls);
        r.step(Place.NO_LEVEL, false, 0, false, MusicLogic.NO_LEVEL_DEBOUNCE + 2);
        helper.assertTrue(r.logic.mode() == Mode.TITLE, "a long gap is the title screen: " + r.logic.mode());
        helper.succeed();
    }
}
