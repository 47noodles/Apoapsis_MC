package dev.apoapsis.scenario;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.apoapsis.Apoapsis;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Saved per world: whether the test-flight set-up (Kestrel on a pad, Kestrel in orbit) has already run. */
public final class TestFlightFlag extends SavedData {
    public static final Codec<TestFlightFlag> CODEC = RecordCodecBuilder.create(
            i -> i.group(Codec.BOOL.optionalFieldOf("set_up", false).forGetter(f -> f.setUp)).apply(i, TestFlightFlag::new));
    public static final SavedDataType<TestFlightFlag> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Apoapsis.MODID, "test_flight"), TestFlightFlag::new, CODEC, null);

    private boolean setUp;

    public TestFlightFlag() {
        this(false);
    }

    public TestFlightFlag(boolean setUp) {
        this.setUp = setUp;
    }

    public boolean isSetUp() {
        return setUp;
    }

    public void markSetUp() {
        setUp = true;
        setDirty();
    }
}
