package dev.apoapsis.client.sky;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import dev.apoapsis.Apoapsis;
import dev.apoapsis.net.PlanetMapPayload;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The bodies' surface colour maps on the client, as textures the sky can draw. Each map is registered under
 * {@code apoapsis:planet_map/<body>}; a new map replaces and closes the old one. Client only; call from the render
 * thread.
 */
public final class PlanetTextures {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<String, Identifier> IDS = new HashMap<>();

    private PlanetTextures() {}

    /** Takes a map sent by the server (called from the payload handler). Malformed maps are ignored. */
    public static void accept(PlanetMapPayload payload) {
        String body = payload.body();
        int w = payload.width(), h = payload.height();
        byte[] rgb = payload.rgb();
        if (!body.matches("[a-z_]{1,32}") || w < 1 || h < 1 || rgb.length != w * h * 3) {
            return;
        }
        NativeImage image;
        if (SpaceSky.off()) {
            image = new NativeImage(w, h, false);
            int i = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int r = rgb[i] & 0xFF, g = rgb[i + 1] & 0xFF, b = rgb[i + 2] & 0xFF;
                    image.setPixel(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
                    i += 3;
                }
            }
        } else {
            long t0 = System.nanoTime();
            image = PlanetDetail.magnify(rgb, w, h);
            LOG.info("[planet map] {} magnified {}x to {}x{} in {} ms", body, PlanetDetail.FACTOR, image.getWidth(),
                    image.getHeight(), (System.nanoTime() - t0) / 1_000_000L);
        }
        Identifier id = Identifier.fromNamespaceAndPath(Apoapsis.MODID, "planet_map/" + body);
        // TextureManager.register closes the texture it replaces.
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> id.toString(), image));
        IDS.put(body, id);
    }

    /** The texture for body {@code bodyId} ("verdance", "pale"), or null while none has arrived. */
    public static @Nullable Identifier texture(String bodyId) {
        return IDS.get(bodyId);
    }

    /** Forgets every map (logout), closing each texture. */
    public static void clear() {
        TextureManager textures = Minecraft.getInstance().getTextureManager();
        for (Identifier id : IDS.values()) {
            textures.release(id);
        }
        IDS.clear();
    }
}
