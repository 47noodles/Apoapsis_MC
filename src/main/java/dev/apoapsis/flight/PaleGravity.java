package dev.apoapsis.flight;

import dev.apoapsis.Apoapsis;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Pale's gravity (0.16 g) for living entities: a transient modifier on the gravity attribute in {@code apoapsis:pale},
 * removed everywhere else. The additive -0.84 on the total leaves 0.16 of the base gravity.
 */
final class PaleGravity {
    static final Identifier ID = Identifier.fromNamespaceAndPath(Apoapsis.MODID, "pale_gravity");
    private static final AttributeModifier MODIFIER =
            new AttributeModifier(ID, -0.84, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    private PaleGravity() {}

    /** Applies the modifier if the entity is in Pale, and removes it otherwise. Call on join and dimension change. */
    static void sync(LivingEntity entity) {
        AttributeInstance gravity = entity.getAttribute(Attributes.GRAVITY);
        if (gravity == null) {
            return;
        }
        if (FlightDimensions.PALE.equals(entity.level().dimension())) {
            gravity.addOrUpdateTransientModifier(MODIFIER);
        } else {
            gravity.removeModifier(ID);
        }
    }
}
