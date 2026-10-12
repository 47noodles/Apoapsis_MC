package dev.apoapsis.client.sky;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import dev.apoapsis.Apoapsis;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.net.PlanetMapPayload;
import dev.apoapsis.net.PlanetMapRequest;
import dev.apoapsis.net.PlanetMapTilePayload;
import dev.apoapsis.planet.map.MapLevels;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The bodies' surface colour maps on the client, as textures the sky can draw. Each map is registered under
 * {@code apoapsis:planet_map/<body>}; a new map replaces and closes the old one. The server sends level 0 (the base
 * map) and then refined levels as tiles; each refined level is assembled off to the side and registered once complete.
 * Map screens may sample {@link #texture(String)}, a mipmapped, anisotropic-sampled texture, and {@link #sampler()} is
 * the sampler to use with it. Client only; call from the render thread.
 */
public final class PlanetTextures {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<String, Identifier> IDS = new HashMap<>();
    /** The level of the map registered for each body: 0 for the base map, then each refined level. */
    private static final Map<String, Integer> LEVEL = new HashMap<>();
    /** The refined level being assembled for each body. */
    private static final Map<String, Pending> PENDING = new HashMap<>();
    /** The last level requested from the server for each body, and when. */
    private static final Map<String, Request> REQUESTS = new HashMap<>();
    /** Tile edge, texels. */
    private static final int TILE = PlanetMapTilePayload.TILE;
    /** How long to wait for a reply before asking again. */
    private static final long WAIT_NANOS = 5_000_000_000L;
    private static @Nullable GpuSampler sampler;
    private static boolean warnedTile;

    private PlanetTextures() {}

    /**
     * The sampler for {@link #texture(String)}: linear filtering through the mip chain, anisotropic, wrapping around the
     * longitude and clamped at the poles. Created on first use and kept for the life of the process.
     */
    public static GpuSampler sampler() {
        GpuSampler s = sampler;
        if (s == null) {
            try {
                s = RenderSystem.getDevice().createSampler(AddressMode.REPEAT, AddressMode.CLAMP_TO_EDGE,
                        FilterMode.LINEAR, FilterMode.LINEAR, 8, OptionalDouble.empty());
            } catch (RuntimeException e) {
                s = RenderSystem.getSamplerCache().getSampler(AddressMode.REPEAT, AddressMode.CLAMP_TO_EDGE,
                        FilterMode.LINEAR, FilterMode.LINEAR, true);
            }
            sampler = s;
        }
        return s;
    }

    /** Takes the base map sent by the server (called from the payload handler). Malformed maps are ignored. */
    public static void accept(PlanetMapPayload payload) {
        String body = payload.body();
        int w = payload.width(), h = payload.height();
        byte[] rgb = payload.rgb();
        if (!body.matches("[a-z_]{1,32}") || w < 1 || h < 1 || rgb.length != w * h * 3) {
            return;
        }
        if (SpaceSky.off()) {
            NativeImage image = new NativeImage(w, h, false);
            int i = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int r = rgb[i] & 0xFF, g = rgb[i + 1] & 0xFF, b = rgb[i + 2] & 0xFF;
                    image.setPixel(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
                    i += 3;
                }
            }
            Identifier id = idOf(body);
            // TextureManager.register closes the texture it replaces.
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> id.toString(), image));
            IDS.put(body, id);
            return;
        }
        long t0 = System.nanoTime();
        NativeImage image = PlanetDetail.magnify(rgb, w, h);
        LOG.info("[planet map] {} magnified {}x to {}x{} in {} ms", body, PlanetDetail.FACTOR, image.getWidth(),
                image.getHeight(), (System.nanoTime() - t0) / 1_000_000L);
        int iw = image.getWidth(), ih = image.getHeight();
        byte[] full;
        try {
            full = toRgb(image);
        } finally {
            image.close();
        }
        PlanetMapTexture texture = new PlanetMapTexture("apoapsis:planet_map/" + body, iw, ih);
        texture.writeRegion(0, 0, iw, ih, full);
        dropPending(body);
        REQUESTS.remove(body);
        register(body, texture);
        LEVEL.put(body, 0);
    }

    /**
     * Takes one tile of a refined map (called from the payload handler). The map is registered once every tile of its
     * level has arrived; tiles for a level no finer than the current one, and malformed tiles, are ignored.
     */
    public static void acceptTile(PlanetMapTilePayload p) {
        if (!p.valid() || SpaceSky.off()) {
            return;
        }
        String body = p.body();
        int level = p.level();
        if (level <= LEVEL.getOrDefault(body, -1)) {
            return;
        }
        byte[] rgb = inflate(p.deflated(), p.rawLength());
        if (rgb == null) {
            if (!warnedTile) {
                warnedTile = true;
                LOG.warn("[planet map] ignored a malformed tile of {} level {}", body, level);
            }
            return;
        }
        int width = p.width(), height = p.height();
        Pending pending = PENDING.get(body);
        if (pending == null || pending.level != level || pending.width != width || pending.height != height) {
            dropPending(body);
            pending = new Pending(level, width, height,
                    new PlanetMapTexture("apoapsis:planet_map/" + body + "/L" + level, width, height));
            PENDING.put(body, pending);
        }
        pending.texture.writeRegion(p.tx() * TILE, p.ty() * TILE, TILE, TILE, rgb);
        pending.lastTile = System.nanoTime();
        int index = p.ty() * (width / TILE) + p.tx();
        if (!pending.seen[index]) {
            pending.seen[index] = true;
            pending.received++;
        }
        if (pending.received < pending.seen.length) {
            return;
        }
        PENDING.remove(body);
        REQUESTS.remove(body);
        register(body, pending.texture);
        LEVEL.put(body, level);
        LOG.info("[planet map] {} level {} ready ({}x{})", body, level, width, height);
    }

    /**
     * Asks the server for the next refined level of {@code body} when the base map is here and the level is not yet
     * complete. Called every frame the body's cap is drawn, so it does nothing but a few map lookups.
     */
    public static void want(String body) {
        if (SpaceSky.off() || !LEVEL.containsKey(body)) {
            return;
        }
        BodyId bodyId = bodyOf(body);
        if (bodyId == null) {
            return;
        }
        int next = LEVEL.get(body) + 1;
        if (next > MapLevels.maxLevel(bodyId)) {
            return;
        }
        long now = System.nanoTime();
        Pending pending = PENDING.get(body);
        if (pending != null && now - pending.lastTile < WAIT_NANOS) {
            return;
        }
        Request last = REQUESTS.get(body);
        if (last != null && last.level == next && now - last.nanos < WAIT_NANOS) {
            return;
        }
        ClientPacketDistributor.sendToServer(new PlanetMapRequest(body, next));
        REQUESTS.put(body, new Request(next, now));
    }

    /** The level of the map now registered for {@code body}, or -1 while none has arrived. */
    public static int level(String body) {
        return LEVEL.getOrDefault(body, -1);
    }

    /**
     * The texture for body {@code bodyId} ("verdance", "pale"), or null while none has arrived. Asking for it every
     * frame is the way to get the sharper levels: each call may send {@link #want} on to the server.
     */
    public static @Nullable Identifier texture(String bodyId) {
        want(bodyId);
        return IDS.get(bodyId);
    }

    /** Forgets every map (logout), closing each texture. */
    public static void clear() {
        TextureManager textures = Minecraft.getInstance().getTextureManager();
        for (Identifier id : IDS.values()) {
            textures.release(id);
        }
        IDS.clear();
        for (Pending pending : PENDING.values()) {
            pending.texture.close();
        }
        PENDING.clear();
        LEVEL.clear();
        REQUESTS.clear();
    }

    private static Identifier idOf(String body) {
        return Identifier.fromNamespaceAndPath(Apoapsis.MODID, "planet_map/" + body);
    }

    /** Registers {@code texture} as the body's map; the texture it replaces is closed. */
    private static void register(String body, PlanetMapTexture texture) {
        Identifier id = idOf(body);
        // TextureManager.register closes the texture it replaces.
        Minecraft.getInstance().getTextureManager().register(id, texture);
        IDS.put(body, id);
    }

    /** Closes and forgets the refined level being assembled for {@code body}, if any. */
    private static void dropPending(String body) {
        Pending old = PENDING.remove(body);
        if (old != null) {
            old.texture.close();
        }
    }

    private static @Nullable BodyId bodyOf(String body) {
        for (BodyId id : BodyId.values()) {
            if (id.id.equals(body)) {
                return id;
            }
        }
        return null;
    }

    /** Inflates a zlib tile into exactly {@code rawLength} bytes, or returns null if it does not fit. */
    private static @Nullable byte[] inflate(byte[] deflated, int rawLength) {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(deflated);
            byte[] out = new byte[rawLength];
            int n = 0;
            while (n < rawLength && !inflater.finished()) {
                int k = inflater.inflate(out, n, rawLength - n);
                if (k == 0) {
                    break;
                }
                n += k;
            }
            return n == rawLength ? out : null;
        } catch (DataFormatException e) {
            return null;
        } finally {
            inflater.end();
        }
    }

    /** The image's colours as RGB bytes, three per texel, rows tightly packed. */
    private static byte[] toRgb(NativeImage image) {
        int w = image.getWidth(), h = image.getHeight();
        byte[] rgb = new byte[w * h * 3];
        int i = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = image.getPixel(x, y);
                rgb[i] = (byte) (argb >> 16);
                rgb[i + 1] = (byte) (argb >> 8);
                rgb[i + 2] = (byte) argb;
                i += 3;
            }
        }
        return rgb;
    }

    /** A refined level being assembled from tiles. */
    private static final class Pending {
        final int level;
        final int width;
        final int height;
        final PlanetMapTexture texture;
        final boolean[] seen;
        int received;
        long lastTile;

        Pending(int level, int width, int height, PlanetMapTexture texture) {
            this.level = level;
            this.width = width;
            this.height = height;
            this.texture = texture;
            this.seen = new boolean[(width / TILE) * (height / TILE)];
            this.lastTile = System.nanoTime();
        }
    }

    /** A request sent to the server for a level, and when it was sent. */
    private record Request(int level, long nanos) {}
}
