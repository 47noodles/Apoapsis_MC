package dev.apoapsis.planet.map;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.net.PlanetMapTilePayload;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import org.jspecify.annotations.Nullable;

/**
 * The on-disk cache of the refined planet map levels, and the zlib helpers for them. Tiles are kept deflated in memory
 * and on disk, so a level stays small; each file holds one body's level, with a header that must match the level it is
 * read for, and every tile is written as its length and its deflated bytes.
 */
public final class PlanetMapStore {
    /** The file format version; a file of another version is not read. */
    public static final int VERSION = 2;

    private PlanetMapStore() {}

    /**
     * One level as stored: its size and its deflated tiles, index {@code ty * tilesX + tx}.
     *
     * @param level the level index
     * @param width the level width, texels
     * @param height the level height, texels
     * @param tiles the deflated tile bytes, one entry per tile
     */
    public record Stored(int level, int width, int height, List<byte[]> tiles) {}

    /** Deflates {@code raw} with zlib at the fastest level. */
    public static byte[] deflate(byte[] raw) {
        Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        try {
            deflater.setInput(raw);
            deflater.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(raw.length / 4 + 64);
            byte[] buf = new byte[8192];
            while (!deflater.finished()) {
                int n = deflater.deflate(buf);
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }

    /**
     * Inflates zlib data of {@code deflated} into exactly {@code rawLength} bytes.
     *
     * @throws DataFormatException if the data is corrupt or does not inflate to exactly {@code rawLength} bytes
     */
    public static byte[] inflate(byte[] deflated, int rawLength) throws DataFormatException {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(deflated);
            byte[] out = new byte[rawLength];
            byte[] probe = new byte[1];
            int produced = 0;
            while (!inflater.finished()) {
                int n;
                if (produced < rawLength) {
                    n = inflater.inflate(out, produced, rawLength - produced);
                    produced += n;
                } else {
                    // The output is full; any further byte means the data is longer than rawLength.
                    n = inflater.inflate(probe);
                    if (n > 0) {
                        throw new DataFormatException("planet map tile inflates past " + rawLength + " bytes");
                    }
                }
                if (n == 0) {
                    break;
                }
            }
            if (!inflater.finished() || produced != rawLength) {
                throw new DataFormatException("planet map tile inflated to " + produced + " bytes, expected "
                        + rawLength);
            }
            return out;
        } finally {
            inflater.end();
        }
    }

    /**
     * Writes {@code stored} to {@code file}, through a {@code .tmp} file that then replaces it. Creates the parent
     * directories. On an I/O error it logs a warning and returns.
     */
    public static void write(Path file, Stored stored) {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(tmp)))) {
                out.writeInt(VERSION);
                out.writeInt(stored.level());
                out.writeInt(stored.width());
                out.writeInt(stored.height());
                out.writeInt(stored.tiles().size());
                for (byte[] tile : stored.tiles()) {
                    out.writeInt(tile.length);
                    out.write(tile);
                }
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Apoapsis.LOG.warn("Could not write planet map store {}", file, e);
        }
    }

    /**
     * Reads the level stored in {@code file} for {@code expected}. Returns null if the file is missing, its version or
     * its level, size or tile count differ from {@code expected}, a tile length is out of range, or the file is
     * truncated or unreadable (the last case also logs a warning).
     */
    public static @Nullable Stored read(Path file, MapLevels.Level expected) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            if (in.readInt() != VERSION) {
                return null;
            }
            int level = in.readInt();
            int width = in.readInt();
            int height = in.readInt();
            int count = in.readInt();
            if (level != expected.index() || width != expected.width() || height != expected.height()
                    || count != expected.tileCount()) {
                return null;
            }
            List<byte[]> tiles = new ArrayList<>(count);
            for (int t = 0; t < count; t++) {
                int length = in.readInt();
                if (length < 1 || length > PlanetMapTilePayload.MAX_DEFLATED) {
                    return null;
                }
                byte[] tile = new byte[length];
                in.readFully(tile);
                tiles.add(tile);
            }
            return new Stored(level, width, height, tiles);
        } catch (IOException e) {
            Apoapsis.LOG.warn("Planet map store {} unreadable", file, e);
            return null;
        }
    }
}
