package dev.apoapsis.client.audio;

import dev.apoapsis.Apoapsis;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * One streamed music track in the MUSIC sound category, heard flat in the head (no position, no distance falloff), so
 * the player's Music slider and the master slider apply. It fades its own gain toward a target each sound-engine tick
 * and stops itself once faded out. Client only.
 */
final class MusicVoice extends AbstractTickableSoundInstance {
    private final MusicLogic.Track track;
    private float level;
    private float target = 1.0F;
    private float step;

    MusicVoice(MusicLogic.Track track, int fadeInTicks, boolean loop) {
        super(SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Apoapsis.MODID, track.event)),
                SoundSource.MUSIC, RandomSource.create());
        this.track = track;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.relative = true;
        this.looping = loop;
        this.volume = 0.0F;
        this.step = 1.0F / Math.max(1, fadeInTicks);
    }

    MusicLogic.Track track() {
        return track;
    }

    /** Current fade gain, 0 to 1 (before the sounds.json volume and the sliders). */
    float level() {
        return level;
    }

    /** Fades to silence over {@code ticks} sound ticks, then stops. */
    void fadeOut(int ticks) {
        target = 0.0F;
        step = Math.max(level, 0.05F) / Math.max(1, ticks);
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (level < target) {
            level = Math.min(target, level + step);
        } else if (level > target) {
            level = Math.max(target, level - step);
        }
        if (target <= 0.0F && level <= 0.0F) {
            stop();
            return;
        }
        float s = Mth.clamp(level, 0.0F, 1.0F);
        this.volume = s * s * (3.0F - 2.0F * s);
    }
}
