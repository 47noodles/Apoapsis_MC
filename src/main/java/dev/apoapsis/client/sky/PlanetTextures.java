package dev.apoapsis.client.sky;

import dev.apoapsis.net.PlanetMapPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * The bodies' surface colour maps on the client, as textures the sky can draw. Interface stub: the real upload lands
 * with the planet-texture work. Client only; call from the render thread.
 */
public final class PlanetTextures {
    private PlanetTextures() {}

    /** Takes a map sent by the server (called from the payload handler). */
    public static void accept(PlanetMapPayload payload) {
    }

    /** The texture for body {@code bodyId} ("verdance", "pale"), or null while none has arrived. */
    public static @Nullable Identifier texture(String bodyId) {
        return null;
    }

    /** Forgets every map (logout, level unload). */
    public static void clear() {
    }
}
