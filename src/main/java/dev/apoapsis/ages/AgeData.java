package dev.apoapsis.ages;

import com.mojang.serialization.Codec;
import dev.apoapsis.Apoapsis;
import java.util.List;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Player data attachments for the Age store. Both are copied on death. */
final class AgeData {
    private AgeData() {}

    static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Apoapsis.MODID);

    /** The player's stored Age, 0..MAX_AGE. Sandbox is applied on top of this, never written into it. */
    static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> AGE = ATTACHMENTS.register(
            "age",
            () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT.optionalFieldOf("age", 0))
                    .copyOnDeath()
                    .build());

    /** Whether this player has had the start (Age 3, ship recipes, first Custodian line). See {@link Ages#start}. */
    static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> STARTED = ATTACHMENTS.register(
            "started",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL.optionalFieldOf("started", false))
                    .copyOnDeath()
                    .build());

    /** The ids of the firsts this player has recorded, in the order they were recorded. See {@link Firsts}. */
    static final DeferredHolder<AttachmentType<?>, AttachmentType<List<String>>> FIRSTS = ATTACHMENTS.register(
            "firsts",
            () -> AttachmentType.builder(() -> List.<String>of())
                    .serialize(Codec.STRING.listOf().optionalFieldOf("firsts", List.of()))
                    .copyOnDeath()
                    .build());

    /** Whether the one-off "first landing on Pale" line has been shown to this player. */
    static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> PALE_ANNOUNCED = ATTACHMENTS.register(
            "pale_landing_announced",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL.optionalFieldOf("pale_landing_announced", false))
                    .copyOnDeath()
                    .build());
}
