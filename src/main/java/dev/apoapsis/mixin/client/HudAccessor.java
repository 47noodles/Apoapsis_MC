package dev.apoapsis.mixin.client;

import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the vanilla action-bar overlay message (warp refusals, sphere of influence entry, hard landing) so the map and
 * the containers can draw it where the HUD cannot. Client only; see {@code dev.apoapsis.client.OverlayMessages}.
 */
@Mixin(Hud.class)
public interface HudAccessor {
    /** The overlay message text, or null. */
    @Accessor("overlayMessageString")
    Component apoapsis$overlayMessage();

    /** Ticks the overlay message has left; it shows while above zero. */
    @Accessor("overlayMessageTime")
    int apoapsis$overlayTime();
}
