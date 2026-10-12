package dev.apoapsis.mixin.client;

import dev.apoapsis.client.audio.MusicDirector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Vanilla stops every sound, music included, whenever the client level is swapped. The orbit and Pale transitions swap
 * the level in mid-flight, which would cut the music dead. Here the swap stops everything except the MUSIC category;
 * the music director fades its own tracks as its state changes. Client only.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftSoundMixin {
    @Redirect(method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;stop()V"))
    private void apoapsis$keepMusicAcrossLevelSwap(SoundManager sounds) {
        MusicDirector.stopForLevelSwap(sounds);
    }
}
