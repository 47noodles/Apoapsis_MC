package dev.apoapsis.flight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.apoapsis.Apoapsis;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Universal time of the server, stored on the overworld. Advances 0.05 s of game time per tick times the warp factor. */
public final class UniverseTime extends SavedData {
    static final Codec<UniverseTime> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("ut", 0.0).forGetter(UniverseTime::ut)
    ).apply(i, UniverseTime::new));

    static final SavedDataType<UniverseTime> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Apoapsis.MODID, "universe_time"), UniverseTime::new, CODEC);

    private double ut;

    UniverseTime() {}

    private UniverseTime(double ut) {
        this.ut = ut;
    }

    /** The universal time of a server, seconds. */
    static UniverseTime get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Universal time in seconds. */
    double ut() {
        return ut;
    }

    void advance(double seconds) {
        ut += seconds;
        setDirty();
    }

    void setUt(double seconds) {
        ut = seconds;
        setDirty();
    }
}
