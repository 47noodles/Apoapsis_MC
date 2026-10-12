package dev.apoapsis.client.audio;

import java.util.random.RandomGenerator;

/**
 * The music director's state machine, with no Minecraft types so a GameTest can drive it. Each client tick the caller
 * hands in where the player is ({@link Place}), whether they pilot a ship, how high, and whether a map is open; the
 * logic decides the {@link Mode} and tells a {@link Sink} to start or fade tracks. It also says whether vanilla music
 * must be held back. Nothing here allocates per tick.
 *
 * <p>Modes: SURFACE (vanilla music as normal), ASCENT (piloting up from the surface: no new vanilla tracks), ENTRY
 * (Galactic Temple once, the moment the climb ends in orbit), ORBIT (quiet: one calm track, then 1 to 3 minutes of
 * silence), PALE_APPROACH and PALE (First Light Particles, then calm tracks) and TITLE (Starfield Romance, looping).
 * A map opening never restarts a track; it only starts the map track when the music has been quiet a while.
 */
public final class MusicLogic {
    /** The four tracks. {@code event} is the sound event path in sounds.json; {@code label} reads in the log. */
    public enum Track {
        GALACTIC_TEMPLE("music.galactic_temple"),
        OBSERVING_THE_STAR("music.observing_the_star"),
        STARFIELD_ROMANCE("music.starfield_romance"),
        FIRST_LIGHT_PARTICLES("music.first_light_particles");

        public final String event;

        Track(String event) {
            this.event = event;
        }
    }

    /** What the director is doing. */
    public enum Mode {
        SURFACE, ASCENT, ENTRY, ORBIT, PALE_APPROACH, PALE, TITLE
    }

    /** Where the local player is, as far as music cares. */
    public enum Place {
        /** No level is loaded: a title-like screen is up, or the world is loading. */
        NO_LEVEL,
        /** Any dimension other than the flight dimensions: Verdance and whatever vanilla has. */
        SURFACE,
        ORBIT_VERDANCE,
        ORBIT_PALE,
        PALE_SURFACE
    }

    /** What the logic asks of the sound side. */
    public interface Sink {
        /** Starts {@code track} fading in over {@code fadeInTicks}; {@code loop} repeats it until faded out. */
        void play(Track track, int fadeInTicks, boolean loop);

        /** Fades whatever our music is playing out over {@code ticks}, then stops it. */
        void fadeOut(int ticks);

        /** One-shot note for the log, called only on a state change. */
        void note(String line);
    }

    /** Fade-in for the entering-space moment: slow, about 8 seconds. */
    public static final int FADE_IN_ENTRY = 160;
    /** Fade-in for every other track, about 5 seconds. */
    public static final int FADE_IN_NORMAL = 100;
    /** Fade-out and crossfade length, about 4 seconds. */
    public static final int FADE_OUT = 80;
    /** Silence between calm tracks: 60 to 180 seconds, like KSP and vanilla. */
    public static final int SILENCE_MIN = 1200;
    public static final int SILENCE_SPAN = 2400;
    /** First calm track after arriving in space with nothing playing: 20 to 50 seconds. */
    public static final int ARRIVAL_MIN = 400;
    public static final int ARRIVAL_SPAN = 600;
    /** First Light after arriving at Pale with nothing playing: 10 seconds. */
    public static final int PALE_DELAY = 200;
    /** The map track starts only when the music has been quiet this long: 45 seconds. */
    public static final int MAP_QUIET = 900;
    /** The title music starts after the level has been missing this long (2 s), unless a screen asks for it sooner. */
    public static final int NO_LEVEL_DEBOUNCE = 40;
    /** Galactic Temple does not play again within this many ticks of its last start: 10 minutes. */
    public static final int ENTRY_COOLDOWN = 12000;
    /** Ascent begins at this height above the surface and ends below {@link #ASCENT_OFF_M}, metres. */
    public static final double ASCENT_ON_M = 500.0;
    public static final double ASCENT_OFF_M = 300.0;
    /** Ascent can only begin after the pilot was seen below this height since the last time in space, metres. */
    public static final double ARM_M = 300.0;

    private static final Track[] ORBIT_POOL = {
            Track.OBSERVING_THE_STAR, Track.STARFIELD_ROMANCE, Track.FIRST_LIGHT_PARTICLES};
    private static final Track[] PALE_POOL = {Track.FIRST_LIGHT_PARTICLES, Track.OBSERVING_THE_STAR};

    private final Sink sink;
    private final RandomGenerator random;

    private Mode mode = Mode.TITLE;
    private Place lastPlace = Place.NO_LEVEL;
    private Track current;
    private Track lastPlayed;
    private Track forced;
    private long nextAt = -1;
    private long lastEnd = Long.MIN_VALUE / 2;
    private long lastEntry = Long.MIN_VALUE / 2;
    private long releaseAt = -1;
    private boolean releaseResets;
    private boolean armed = true;
    private boolean mapWas;
    private int noLevelTicks;
    private boolean started;

    public MusicLogic(Sink sink, RandomGenerator random) {
        this.sink = sink;
        this.random = random;
    }

    public Mode mode() {
        return mode;
    }

    /** The track that is meant to be playing (not one fading out), or null in silence. */
    public Track current() {
        return current;
    }

    /** True while vanilla music must not start or continue: our music owns the soundscape, or is still fading out. */
    public boolean suppressVanilla() {
        return switch (mode) {
            case SURFACE -> releaseAt >= 0;
            case ASCENT -> false;
            default -> true;
        };
    }

    /** True while vanilla may finish a track it is playing but must not start another (the climb). */
    public boolean holdNewVanilla() {
        return mode == Mode.ASCENT;
    }

    /**
     * Returns true once, on the tick vanilla music may come back after our music faded out. {@link #releaseResetsDelay}
     * then says whether the caller should reset vanilla's wait so the next track comes "later as normal".
     */
    public boolean takeRelease(long tick) {
        if (releaseAt >= 0 && mode == Mode.SURFACE && tick >= releaseAt) {
            releaseAt = -1;
            return true;
        }
        return false;
    }

    /** True if the release just taken should reset vanilla's song delay. */
    public boolean releaseResetsDelay() {
        return releaseResets;
    }

    /**
     * Called every client tick, before vanilla's own music tick.
     *
     * @param tick       a client tick count that keeps increasing
     * @param place      where the player is
     * @param piloting   the player sits in a ship
     * @param altitudeM  height above the surface in the surface dimension, metres (ignored elsewhere)
     * @param mapOpen    a map screen is open
     * @param titleHint  a title-like screen says so: skip the no-level debounce
     */
    public void update(long tick, Place place, boolean piloting, double altitudeM, boolean mapOpen, boolean titleHint) {
        Place p = place;
        if (p == Place.NO_LEVEL) {
            noLevelTicks++;
            if (!titleHint && noLevelTicks < NO_LEVEL_DEBOUNCE && started) {
                p = lastPlace;
            }
        } else {
            noLevelTicks = 0;
        }

        if (p != Place.SURFACE) {
            armed = false;
        } else if (!piloting || altitudeM < ARM_M) {
            armed = true;
        }

        Mode target = switch (p) {
            case NO_LEVEL -> Mode.TITLE;
            case ORBIT_PALE -> Mode.PALE_APPROACH;
            case PALE_SURFACE -> Mode.PALE;
            case ORBIT_VERDANCE -> orbitTarget(tick);
            case SURFACE -> surfaceTarget(piloting, altitudeM);
        };
        if (target != mode || !started) {
            enter(target, tick, p, altitudeM);
        }
        started = true;
        lastPlace = p;

        boolean calm = mode == Mode.ORBIT || mode == Mode.PALE_APPROACH || mode == Mode.PALE;
        if (current == null && nextAt >= 0 && tick >= nextAt) {
            if (calm) {
                startScheduled();
            } else if (mode == Mode.TITLE) {
                nextAt = -1;
                playNow(Track.STARFIELD_ROMANCE, FADE_IN_NORMAL, true);
            }
        }
        if (mapOpen && !mapWas && calm && current == null && tick - lastEnd >= MAP_QUIET) {
            sink.note("map opened in quiet: starting the map track");
            playNow(Track.OBSERVING_THE_STAR, FADE_IN_NORMAL, false);
        }
        mapWas = mapOpen;
    }

    /** The current track finished or could not start: the next calm track comes after a silence. */
    public void trackEnded(long tick) {
        Track ended = current;
        current = null;
        lastEnd = tick;
        if (mode == Mode.ENTRY) {
            mode = Mode.ORBIT;
            sink.note("mode ENTRY -> ORBIT after " + ended);
        }
        if (mode == Mode.TITLE) {
            nextAt = tick + SILENCE_MIN;
        } else if (forced != null) {
            nextAt = tick + PALE_DELAY;
        } else {
            nextAt = tick + SILENCE_MIN + random.nextInt(SILENCE_SPAN + 1);
        }
        sink.note("ended " + ended + ", next in " + (nextAt - tick) / 20 + " s");
    }

    private Mode orbitTarget(long tick) {
        if (mode == Mode.ENTRY) {
            return Mode.ENTRY;
        }
        boolean fromSurface = lastPlace == Place.SURFACE;
        if (fromSurface && tick - lastEntry >= ENTRY_COOLDOWN) {
            return Mode.ENTRY;
        }
        return Mode.ORBIT;
    }

    private Mode surfaceTarget(boolean piloting, double altitudeM) {
        if (mode == Mode.ASCENT) {
            return piloting && altitudeM >= ASCENT_OFF_M ? Mode.ASCENT : Mode.SURFACE;
        }
        return armed && piloting && altitudeM >= ASCENT_ON_M ? Mode.ASCENT : Mode.SURFACE;
    }

    private void enter(Mode to, long tick, Place p, double altitudeM) {
        Mode from = mode;
        mode = to;
        sink.note("mode " + from + " -> " + to + " (" + p + ", " + Math.round(altitudeM) + " m)");
        switch (to) {
            case TITLE -> {
                if (current != Track.STARFIELD_ROMANCE) {
                    fadeOutCurrent();
                    nextAt = tick;
                }
            }
            case SURFACE, ASCENT -> {
                fadeOutCurrent();
                nextAt = -1;
                forced = null;
                if (to == Mode.ASCENT) {
                    releaseAt = -1;
                } else if (from == Mode.ASCENT) {
                    releaseAt = tick;
                    releaseResets = true;
                } else if (from != Mode.SURFACE) {
                    releaseAt = tick + FADE_OUT + 5;
                    releaseResets = from != Mode.TITLE;
                }
            }
            case ENTRY -> {
                releaseAt = -1;
                lastEntry = tick;
                playNow(Track.GALACTIC_TEMPLE, FADE_IN_ENTRY, false);
            }
            case ORBIT -> {
                releaseAt = -1;
                if (from == Mode.TITLE) {
                    fadeOutCurrent();
                }
                if (current == null) {
                    nextAt = tick + ARRIVAL_MIN + random.nextInt(ARRIVAL_SPAN + 1);
                }
            }
            case PALE_APPROACH, PALE -> {
                releaseAt = -1;
                if (from == Mode.PALE_APPROACH || from == Mode.PALE) {
                    break;
                }
                if (from == Mode.TITLE) {
                    fadeOutCurrent();
                }
                if (current == Track.GALACTIC_TEMPLE) {
                    forced = Track.FIRST_LIGHT_PARTICLES;
                } else if (current == Track.FIRST_LIGHT_PARTICLES) {
                    break;
                } else if (current != null) {
                    playNow(Track.FIRST_LIGHT_PARTICLES, FADE_IN_NORMAL, false);
                } else {
                    forced = Track.FIRST_LIGHT_PARTICLES;
                    nextAt = tick + PALE_DELAY;
                }
            }
            default -> { }
        }
    }

    private void startScheduled() {
        Track t = forced != null ? forced : pick(mode == Mode.ORBIT ? ORBIT_POOL : PALE_POOL);
        forced = null;
        nextAt = -1;
        playNow(t, FADE_IN_NORMAL, false);
    }

    /** A random track of the pool that is not the one played last, when the pool has another. */
    private Track pick(Track[] pool) {
        Track t = pool[random.nextInt(pool.length)];
        if (t == lastPlayed) {
            t = pool[(indexOf(pool, t) + 1 + random.nextInt(pool.length - 1)) % pool.length];
        }
        return t;
    }

    private static int indexOf(Track[] pool, Track t) {
        for (int i = 0; i < pool.length; i++) {
            if (pool[i] == t) {
                return i;
            }
        }
        return 0;
    }

    private void playNow(Track t, int fadeIn, boolean loop) {
        if (current != null) {
            sink.fadeOut(FADE_OUT);
        }
        current = t;
        lastPlayed = t;
        nextAt = -1;
        sink.play(t, fadeIn, loop);
    }

    private void fadeOutCurrent() {
        if (current != null) {
            sink.fadeOut(FADE_OUT);
            lastPlayed = current;
            current = null;
        }
    }
}
