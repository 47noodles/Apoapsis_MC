package dev.apoapsis.ages;

import com.mojang.serialization.Codec;
import dev.apoapsis.Apoapsis;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Per-world sandbox flag: when on, every player counts as MAX_AGE. Stored on the overworld. */
final class SandboxData extends SavedData {
    static final Codec<SandboxData> CODEC = Codec.BOOL.optionalFieldOf("sandbox", false)
            .codec()
            .xmap(on -> new SandboxData(on), SandboxData::isEnabled);

    static final SavedDataType<SandboxData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Apoapsis.MODID, "ages_sandbox"),
            () -> new SandboxData(false),
            CODEC);

    private boolean enabled;

    SandboxData(boolean enabled) {
        this.enabled = enabled;
    }

    boolean isEnabled() {
        return enabled;
    }

    void setEnabled(boolean value) {
        this.enabled = value;
        setDirty();
    }

    /** The sandbox flag of this server's world (read from the overworld). */
    static SandboxData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
