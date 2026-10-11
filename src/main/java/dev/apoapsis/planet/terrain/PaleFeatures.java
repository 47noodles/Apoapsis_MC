package dev.apoapsis.planet.terrain;

import com.mojang.serialization.MapCodec;
import dev.apoapsis.Apoapsis;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers the feature types that build Pale's surface. Owned by node Q-PALE. */
public final class PaleFeatures {
    private PaleFeatures() {}

    public static final DeferredRegister<MapCodec<? extends Feature>> FEATURE_TYPES =
            DeferredRegister.create(Registries.FEATURE_TYPE, Apoapsis.MODID);

    /** Registers the feature types on the mod bus. */
    public static void register(IEventBus modBus) {
        FEATURE_TYPES.register("crater", () -> CraterFeature.CODEC);
        FEATURE_TYPES.register(modBus);
    }
}
