package dev.apoapsis.test;

import dev.apoapsis.net.PlanetMapPayload;
import dev.apoapsis.net.PlanetMapRequest;
import dev.apoapsis.net.PlanetMapTilePayload;
import dev.apoapsis.planet.PlanetMapGen;
import dev.apoapsis.planet.PlanetMaps;
import dev.apoapsis.planet.map.MapLevels;
import dev.apoapsis.planet.map.PlanetMapStore;
import dev.apoapsis.planet.map.PlanetMapTileGen;
import dev.apoapsis.ship.ShipEntity;
import dev.apoapsis.test.ApoapsisGameTest.ShipCleanup;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.zip.DataFormatException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** GameTests for the refined planet map: tiles against the whole map, the store, the tile payload and who may ask. */
final class PlanetMapTests {
    private PlanetMapTests() {}

    /** Texels per side of the test map, and the level that covers it with two tiles. */
    private static final int WIDTH = 512;
    private static final int HEIGHT = 256;
    private static final int TILE = MapLevels.TILE;

    /** A patterned byte array of {@code length} bytes; {@code seed} shifts the pattern. */
    private static byte[] patterned(int length, int seed) {
        byte[] out = new byte[length];
        for (int i = 0; i < length; i++) {
            out[i] = (byte) ((i * 31 + (i >> 8) * 7 + seed) ^ (i >> 5));
        }
        return out;
    }

    /** The first texel where {@code tile} differs from the whole map's texels, or null if none differs. */
    private static String firstMismatch(byte[] tile, byte[] whole, int tx) {
        for (int j = 0; j < TILE; j++) {
            for (int i = 0; i < TILE; i++) {
                int got = 3 * (j * TILE + i);
                int want = 3 * (j * WIDTH + tx * TILE + i);
                for (int c = 0; c < 3; c++) {
                    if (tile[got + c] != whole[want + c]) {
                        return "tile " + tx + " texel (" + (tx * TILE + i) + ", " + j + ") differs from the whole map";
                    }
                }
            }
        }
        return null;
    }

    /** Inflates {@code deflated}, failing the test on corrupt data. */
    private static byte[] inflateOrThrow(byte[] deflated, int rawLength) {
        try {
            return PlanetMapStore.inflate(deflated, rawLength);
        } catch (DataFormatException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Whether inflating {@code deflated} to {@code rawLength} bytes is refused. */
    private static boolean inflateRefuses(byte[] deflated, int rawLength) {
        try {
            PlanetMapStore.inflate(deflated, rawLength);
            return false;
        } catch (DataFormatException e) {
            return true;
        }
    }

    /** The refined tiles of both bodies are the same texels as the whole map at that level, so no seam shows. */
    static void tilesMatchWholeMap(GameTestHelper helper, ShipCleanup ships) {
        RegistryAccess access = helper.getLevel().getServer().registryAccess();
        TerrainProbe.Body[] bodies = {TerrainProbe.pale(access), TerrainProbe.verdance(access)};
        MapLevels.Level level = new MapLevels.Level(1, WIDTH, HEIGHT, 1);
        for (TerrainProbe.Body body : bodies) {
            boolean pale = body.name().equals("pale");
            PlanetMapPayload map = PlanetMapGen.generate(body.sampler(), body.generator(), body.randomState(),
                    body.name(), body.radius(), WIDTH, HEIGHT, 1);
            helper.assertTrue(map != null && map.rgb().length == 3 * WIDTH * HEIGHT,
                    body.name() + " whole map was not generated at " + WIDTH + "x" + HEIGHT);
            byte[] whole = map.rgb();
            for (int tx = 0; tx < level.tilesX(); tx++) {
                byte[] tile = PlanetMapTileGen.generate(body.sampler(), body.generator(), body.randomState(), pale,
                        body.radius(), level, tx, 0);
                helper.assertTrue(tile != null && tile.length == 3 * TILE * TILE,
                        body.name() + " tile " + tx + " was not generated");
                String mismatch = firstMismatch(tile, whole, tx);
                helper.assertTrue(mismatch == null, body.name() + ": " + mismatch);
                boolean varied = false;
                for (byte b : tile) {
                    if (b != tile[0]) {
                        varied = true;
                        break;
                    }
                }
                helper.assertTrue(varied, body.name() + " tile " + tx + " is one colour throughout");
            }
        }
        helper.succeed();
    }

    /** The deflate helpers round trip, and the store reads back the level it wrote, or nothing for another level. */
    static void storeRoundTrip(GameTestHelper helper, ShipCleanup ships) {
        byte[] raw = patterned(196608, 0);
        byte[] rawDeflated = PlanetMapStore.deflate(raw);
        helper.assertTrue(Arrays.equals(inflateOrThrow(rawDeflated, raw.length), raw),
                "inflate(deflate(x)) changed the bytes");
        helper.assertTrue(inflateRefuses(rawDeflated, raw.length - 1), "a tile inflated past a short raw length");
        helper.assertTrue(inflateRefuses(rawDeflated, raw.length + 1), "a tile inflated to a longer raw length");

        MapLevels.Level level = new MapLevels.Level(1, WIDTH, HEIGHT, 1);
        byte[] tileA = PlanetMapStore.deflate(patterned(196608, 0));
        byte[] tileB = PlanetMapStore.deflate(patterned(196608, 77));
        PlanetMapStore.Stored stored = new PlanetMapStore.Stored(1, WIDTH, HEIGHT, List.of(tileA, tileB));
        Path file = null;
        try {
            file = Files.createTempFile("apoapsis-planet-map", ".bin");
            PlanetMapStore.write(file, stored);
            PlanetMapStore.Stored back = PlanetMapStore.read(file, level);
            helper.assertTrue(back != null, "the stored level did not read back");
            helper.assertTrue(back.level() == 1 && back.width() == WIDTH && back.height() == HEIGHT
                    && back.tiles().size() == 2, "the stored level header changed in the round trip");
            helper.assertTrue(Arrays.equals(back.tiles().get(0), tileA) && Arrays.equals(back.tiles().get(1), tileB),
                    "the stored tiles changed in the round trip");
            helper.assertTrue(PlanetMapStore.read(file, new MapLevels.Level(1, 1024, 512, 2)) == null,
                    "a level of another size was read");

            byte[] bytes = Files.readAllBytes(file);
            Files.write(file, Arrays.copyOf(bytes, bytes.length - 100));
            helper.assertTrue(PlanetMapStore.read(file, level) == null, "a file truncated by 100 bytes was read");

            helper.assertTrue(PlanetMapStore.read(file.resolveSibling(file.getFileName() + ".missing"), level) == null,
                    "a missing file was read");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (file != null) {
                file.toFile().delete();
            }
        }
        helper.succeed();
    }

    /** A tile payload survives its codec with every field, and a bad header or a tile outside its map is refused. */
    static void tilePayload(GameTestHelper helper, ShipCleanup ships) {
        RegistryAccess access = helper.getLevel().getServer().registryAccess();
        int raw = PlanetMapTilePayload.RAW_LENGTH;
        byte[] deflated = PlanetMapStore.deflate(patterned(raw, 3));
        PlanetMapTilePayload tile = new PlanetMapTilePayload("verdance", 1, 2048, 1024, 3, 1, raw, deflated);
        helper.assertTrue(tile.valid(), "a well-formed tile reports invalid");
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
        PlanetMapTilePayload.STREAM_CODEC.encode(buf, tile);
        PlanetMapTilePayload back = PlanetMapTilePayload.STREAM_CODEC.decode(buf);
        helper.assertTrue("verdance".equals(back.body()) && back.level() == 1 && back.width() == 2048
                && back.height() == 1024 && back.tx() == 3 && back.ty() == 1 && back.rawLength() == raw
                && Arrays.equals(back.deflated(), deflated), "the tile changed in the codec round trip");

        helper.assertTrue(!new PlanetMapTilePayload("pale", 0, 512, 256, 0, 0, raw, deflated).valid(),
                "level 0 accepted as a refined tile");
        helper.assertTrue(!new PlanetMapTilePayload("pale", 1, 500, 256, 0, 0, raw, deflated).valid(),
                "a width that is not a multiple of the tile accepted");
        helper.assertTrue(!new PlanetMapTilePayload("pale", 1, 512, 256, 2, 0, raw, deflated).valid(),
                "a tile column outside the map accepted");
        helper.assertTrue(!new PlanetMapTilePayload("pale", 1, 512, 256, 0, 0, 5, deflated).valid(),
                "a raw length of 5 accepted");
        helper.assertTrue(!new PlanetMapTilePayload("pale", 1, 512, 256, 0, 0, raw, new byte[0]).valid(),
                "an empty deflated tile accepted");
        helper.assertTrue(!new PlanetMapTilePayload("Pale!", 1, 512, 256, 0, 0, raw, deflated).valid(),
                "a bad body name accepted");

        RegistryFriendlyByteBuf bad = new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
        bad.writeUtf("pale", 32);
        bad.writeVarInt(1);
        bad.writeVarInt(512);
        bad.writeVarInt(256);
        bad.writeVarInt(5);
        bad.writeVarInt(0);
        bad.writeVarInt(raw);
        bad.writeByteArray(deflated);
        boolean refused = false;
        try {
            PlanetMapTilePayload.STREAM_CODEC.decode(bad);
        } catch (DecoderException e) {
            refused = true;
        }
        helper.assertTrue(refused, "the codec decoded a tile outside its map");
        helper.succeed();
    }

    /** Only a ship's pilot may ask for refined maps, and the request codec keeps its body and level. */
    @SuppressWarnings("removal")
    static void requestGate(GameTestHelper helper, ShipCleanup ships) {
        ServerPlayer walker = helper.makeMockServerPlayerInLevel();
        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
        ShipEntity ship = ships.track(TestSupport.spawnAboveFloor(helper, TestSupport.threeBlockShip(helper, 600.0), 6));
        ship.seatPilot(pilot);
        helper.assertTrue(ship.getPilot() == pilot, "the pilot was not seated in the ship");

        helper.assertTrue(PlanetMaps.mayRequestMap(pilot), "the pilot of a ship may not ask for a refined map");
        helper.assertTrue(!PlanetMaps.mayRequestMap(walker), "a player on foot in the overworld may ask for a refined map");

        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().getServer().registryAccess());
        PlanetMapRequest.STREAM_CODEC.encode(buf, new PlanetMapRequest("pale", 2));
        PlanetMapRequest back = PlanetMapRequest.STREAM_CODEC.decode(buf);
        helper.assertTrue("pale".equals(back.body()) && back.level() == 2,
                "the planet map request changed in the codec round trip");
        helper.succeed();
    }
}
