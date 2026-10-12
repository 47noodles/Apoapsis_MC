package dev.apoapsis.client.sky;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.device.GpuDevice;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.lwjgl.system.MemoryUtil;

/**
 * A mipmapped RGBA texture for a planet's surface map, filled a region at a time from RGB bytes. Each write also
 * uploads box-filtered mip levels for the same region, so the texture never needs a CPU copy of the whole image. Client
 * only; call from the render thread.
 */
final class PlanetMapTexture extends AbstractTexture {
    /** Most mip levels a map gets, whatever its size. */
    private static final int MAX_MIPS = 9;

    private final int mips;

    /**
     * Creates an empty texture of {@code width} x {@code height} texels with a full mip chain (up to nine levels) and
     * the shared trilinear sampler from {@link PlanetTextures#sampler()}.
     */
    PlanetMapTexture(String label, int width, int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("empty map " + width + "x" + height);
        }
        GpuDevice device = RenderSystem.getDevice();
        int floorLog2 = 31 - Integer.numberOfLeadingZeros(Math.min(width, height));
        this.mips = Math.min(MAX_MIPS, 1 + floorLog2);
        this.texture = device.createTexture(() -> label, 5, GpuFormat.RGBA8_UNORM, width, height, 1, mips);
        this.textureView = device.createTextureView(this.texture);
        this.sampler = PlanetTextures.sampler();
    }

    /**
     * Writes the RGB texels {@code rgb} (three bytes each, {@code w * h} texels, rows tightly packed) into mip level 0 at
     * ({@code x}, {@code y}), then the box-filtered mip levels for the same region at their scaled positions.
     */
    void writeRegion(int x, int y, int w, int h, byte[] rgb) {
        if (w < 1 || h < 1 || rgb.length != w * h * 3) {
            throw new IllegalArgumentException("bad region " + w + "x" + h + " for " + rgb.length + " bytes");
        }
        byte[] level = rgb;
        int lw = w, lh = h;
        for (int m = 0; m < mips; m++) {
            if (m > 0) {
                if (lw / 2 < 1 || lh / 2 < 1) {
                    break;
                }
                level = halve(level, lw, lh);
                lw /= 2;
                lh /= 2;
            }
            upload(level, lw, lh, m, x >> m, y >> m);
        }
    }

    /** The number of mip levels the texture has. */
    int mipLevels() {
        return mips;
    }

    private void upload(byte[] rgb, int w, int h, int mip, int destX, int destY) {
        ByteBuffer buffer = MemoryUtil.memAlloc(w * h * 4);
        try {
            for (int i = 0; i < w * h; i++) {
                buffer.put(rgb[i * 3]).put(rgb[i * 3 + 1]).put(rgb[i * 3 + 2]).put((byte) 0xFF);
            }
            buffer.flip();
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, buffer, mip, 0, destX, destY,
                    w, h);
        } finally {
            MemoryUtil.memFree(buffer);
        }
    }

    /** Averages each 2x2 block of an RGB image (rounding) into a half-size image; odd edges are dropped. */
    private static byte[] halve(byte[] src, int w, int h) {
        int nw = w / 2, nh = h / 2;
        byte[] out = new byte[nw * nh * 3];
        for (int y = 0; y < nh; y++) {
            for (int x = 0; x < nw; x++) {
                int a = (y * 2 * w + x * 2) * 3;
                int b = a + 3;
                int c = a + w * 3;
                int d = c + 3;
                int o = (y * nw + x) * 3;
                for (int k = 0; k < 3; k++) {
                    int sum = (src[a + k] & 0xFF) + (src[b + k] & 0xFF) + (src[c + k] & 0xFF) + (src[d + k] & 0xFF);
                    out[o + k] = (byte) ((sum + 2) >> 2);
                }
            }
        }
        return out;
    }
}
