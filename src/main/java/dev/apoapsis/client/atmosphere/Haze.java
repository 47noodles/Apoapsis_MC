package dev.apoapsis.client.atmosphere;

import net.minecraft.client.renderer.fog.FogData;

/**
 * Verdance's haze: atmospheric fog tuned so the blocks and the far terrain meet inside it. The voxel world ends at the
 * render distance and the coarse far tiles carry on from there, with a visible change of shade and detail at the
 * boundary; here the fog is already a third to a half of the way to the sky colour at that boundary and thickens beyond
 * it, so the join dissolves and the far land fades toward the horizon like distant air. It follows the same
 * {@code FogData} the voxels and the far mesh read, so both haze alike. The haze comes in with the far terrain and goes
 * out as the climb goes on ({@link #FADE_FROM_M} to {@link #FADE_TO_M}), where the sky takes over (see
 * {@code SkyClimb}). Pure maths on a {@link FogData}; client only.
 */
public final class Haze {
    /** The fog starts at this share of the distance to the voxel boundary. */
    static final double START_SHARE = 0.25;
    /** The fog is complete at this many boundary distances. */
    static final double END_SHARE = 3.2;
    /** The haze starts to thin above this altitude, metres, and is gone at {@link #FADE_TO_M}. */
    static final double FADE_FROM_M = 450.0;
    static final double FADE_TO_M = 1500.0;

    private Haze() {}

    /**
     * Pulls the environmental fog in {@code fog} toward the haze. {@code farFade} is the far terrain's fade-in, 0..1;
     * {@code altitude} the camera's height above the ground in metres; {@code renderBlocks} the voxel render distance.
     */
    public static void apply(FogData fog, double farFade, double altitude, float renderBlocks) {
        double keep = farFade * (1.0 - smoothstep(FADE_FROM_M, FADE_TO_M, altitude));
        if (keep <= 0.0) {
            return;
        }
        // The voxel boundary is where the horizontal render distance meets the camera's slant range.
        double boundary = Math.sqrt((double) renderBlocks * renderBlocks + Math.max(altitude, 0.0) * Math.max(altitude, 0.0));
        float start = (float) (START_SHARE * boundary);
        float end = (float) (END_SHARE * boundary);
        fog.environmentalStart += (start - fog.environmentalStart) * (float) keep;
        fog.environmentalEnd += (end - fog.environmentalEnd) * (float) keep;
    }

    /** Hermite ramp from 0 at {@code edge0} to 1 at {@code edge1}. */
    static double smoothstep(double edge0, double edge1, double x) {
        double t = Math.min(1.0, Math.max(0.0, (x - edge0) / (edge1 - edge0)));
        return t * t * (3 - 2 * t);
    }
}
