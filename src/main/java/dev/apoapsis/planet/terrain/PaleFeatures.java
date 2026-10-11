package dev.apoapsis.planet.terrain;

import com.mojang.serialization.MapCodec;
import dev.apoapsis.Apoapsis;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers the feature types that build Pale's surface, and names the pieces of Pale's data the code relies on. Owned by node Q-PALE. */
public final class PaleFeatures {
    private PaleFeatures() {}

    /** The biome of Pale's dark basalt plains. */
    public static final Identifier MARIA = Identifier.fromNamespaceAndPath(Apoapsis.MODID, "pale_maria");
    /** The biome of Pale's light regolith highlands. */
    public static final Identifier HIGHLANDS = Identifier.fromNamespaceAndPath(Apoapsis.MODID, "pale_highlands");
    /** The crater feature, which the far-terrain sampler reads its chances from. */
    public static final ResourceKey<Feature> CRATER = ResourceKey.create(Registries.FEATURE,
            Identifier.fromNamespaceAndPath(Apoapsis.MODID, "crater"));

    public static final DeferredRegister<MapCodec<? extends Feature>> FEATURE_TYPES =
            DeferredRegister.create(Registries.FEATURE_TYPE, Apoapsis.MODID);

    /** Registers the feature types on the mod bus. */
    public static void register(IEventBus modBus) {
        FEATURE_TYPES.register("crater", () -> CraterFeature.CODEC);
        FEATURE_TYPES.register(modBus);
    }
}
