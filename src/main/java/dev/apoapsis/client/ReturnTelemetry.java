package dev.apoapsis.client;

import com.mojang.logging.LogUtils;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.slf4j.Logger;

/**
 * The telemetry file of the return-trip script ({@link TestDriverReturn}): one CSV row per sample, flushed at once, so a
 * crash or a quit still leaves the run so far on disk.
 */
final class ReturnTelemetry {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String HEADER = "ut_s,real_s,body,regime,altitude_m,speed_mps,fuel_kg,throttle,phase";

    private BufferedWriter out;

    /** Opens (and truncates) {@code file}; if that fails the rows are dropped and a warning is logged. */
    ReturnTelemetry(Path file) {
        try {
            Files.createDirectories(file.getParent());
            out = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
            out.write(HEADER);
            out.newLine();
            out.flush();
        } catch (IOException e) {
            LOG.warn("[test driver] cannot write the telemetry file {}", file, e);
            out = null;
        }
    }

    /** Appends one sample. {@code utS} is universal time, {@code realS} client seconds since the script began. */
    void row(double utS, double realS, String body, String regime, double altitudeM, double speedMps, double fuelKg,
             double throttle, String phase) {
        if (out == null) {
            return;
        }
        try {
            out.write(String.format(Locale.ROOT, "%.1f,%.2f,%s,%s,%.1f,%.2f,%.1f,%.2f,%s", utS, realS, body, regime,
                    altitudeM, speedMps, fuelKg, throttle, phase));
            out.newLine();
            out.flush();
        } catch (IOException e) {
            LOG.warn("[test driver] telemetry write failed; no more rows", e);
            out = null;
        }
    }

    /** Closes the file. */
    void close() {
        if (out == null) {
            return;
        }
        try {
            out.close();
        } catch (IOException e) {
            LOG.warn("[test driver] telemetry close failed", e);
        }
        out = null;
    }
}
