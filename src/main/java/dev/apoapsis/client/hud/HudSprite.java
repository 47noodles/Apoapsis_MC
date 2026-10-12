package dev.apoapsis.client.hud;

import com.mojang.blaze3d.platform.NativeImage;
import dev.apoapsis.Apoapsis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * A texture the HUD paints itself in device pixels, for shapes with smooth edges (the navball, the flight markers): the
 * pixels are written by code each time the picture changes and drawn one device pixel to one texel, so nothing is
 * resampled. Registered under {@code apoapsis:hud/<name>}. Client only; render thread.
 */
final class HudSprite {
    private final Identifier id;
    private DynamicTexture texture;
    private int width;
    private int height;

    HudSprite(String name) {
        this.id = Identifier.fromNamespaceAndPath(Apoapsis.MODID, "hud/" + name);
    }

    Identifier id() {
        return id;
    }

    int width() {
        return width;
    }

    int height() {
        return height;
    }

    /**
     * The sprite's pixel buffer at {@code w} by {@code h}. A size change makes a new, clear texture and returns true
     * through {@link #resized()} on the next call only; the caller repaints either way.
     */
    NativeImage pixels(int w, int h) {
        if (texture == null || w != width || h != height) {
            width = w;
            height = h;
            texture = new DynamicTexture(() -> id.toString(), w, h, true);
            // TextureManager.register closes the texture it replaces.
            Minecraft.getInstance().getTextureManager().register(id, texture);
        }
        return texture.getPixels();
    }

    /** Whether the sprite has pixels of this size. */
    boolean has(int w, int h) {
        return texture != null && w == width && h == height;
    }

    /** Sends the painted pixels to the GPU. */
    void upload() {
        if (texture != null) {
            texture.upload();
        }
    }

    /** Alpha composite of straight-alpha colour {@code rgb} at {@code a} (0..1) over {@code dst} (ARGB). */
    static int over(int dst, int rgb, float a) {
        if (a <= 0f) {
            return dst;
        }
        a = Math.min(1f, a);
        float da = ((dst >>> 24) & 0xFF) / 255f;
        float oa = a + da * (1f - a);
        if (oa <= 0f) {
            return 0;
        }
        int r = blend((dst >> 16) & 0xFF, (rgb >> 16) & 0xFF, a, da, oa);
        int g = blend((dst >> 8) & 0xFF, (rgb >> 8) & 0xFF, a, da, oa);
        int b = blend(dst & 0xFF, rgb & 0xFF, a, da, oa);
        return (Math.round(oa * 255f) << 24) | (r << 16) | (g << 8) | b;
    }

    private static int blend(int d, int s, float a, float da, float oa) {
        return Math.min(255, Math.round((s * a + d * da * (1f - a)) / oa));
    }

    /** Coverage (0..1) of a stroke of half width {@code hw} at signed distance {@code dist} from its centre line. */
    static float stroke(double dist, double hw) {
        return (float) Math.max(0.0, Math.min(1.0, hw - Math.abs(dist) + 0.5));
    }

    /** Distance from (px, py) to the segment (ax, ay)-(bx, by). */
    static double segment(double px, double py, double ax, double ay, double bx, double by) {
        double abx = bx - ax;
        double aby = by - ay;
        double len2 = abx * abx + aby * aby;
        double t = len2 <= 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * abx + (py - ay) * aby) / len2));
        return Math.hypot(px - (ax + t * abx), py - (ay + t * aby));
    }

    /** A colour scaled in brightness by {@code k} (above 1 lightens towards the channel limit), alpha kept. */
    static int shade(int argb, float k) {
        int r = Math.min(255, Math.round(((argb >> 16) & 0xFF) * k));
        int g = Math.min(255, Math.round(((argb >> 8) & 0xFF) * k));
        int b = Math.min(255, Math.round((argb & 0xFF) * k));
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}
