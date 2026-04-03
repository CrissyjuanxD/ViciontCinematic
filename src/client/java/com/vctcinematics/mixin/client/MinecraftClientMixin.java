package com.vctcinematics.mixin.client;

import com.vctcinematics.core.CinematicManager;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private void blockInputDuringCinematic(CallbackInfo ci) {
        if (CinematicManager.isPlaying) {
            MinecraftClient client = MinecraftClient.getInstance();

            while (client.options.togglePerspectiveKey.wasPressed()) {
            }
        }
    }
}