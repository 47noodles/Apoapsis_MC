package dev.apoapsis.mixin.client;

import dev.apoapsis.client.ship.CockpitCamera;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While the local player rides a ship with the cockpit view active, mouse turns go to the cockpit head instead of the
 * player's yaw and pitch, so vanilla's pitch clamp at straight up or down does not apply. Client only.
 */
@Mixin(Entity.class)
public abstract class EntityTurnMixin {
    @Inject(method = "turn(DD)V", at = @At("HEAD"), cancellable = true)
    private void apoapsis$cockpitTurn(double xo, double yo, CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer player && player.getVehicle() instanceof ShipEntity
                && CockpitCamera.isActive()) {
            CockpitCamera.turn(xo, yo);
            ci.cancel();
        }
    }
}
