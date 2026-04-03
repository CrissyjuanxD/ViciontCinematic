package com.vctcinematics.mixin.client;

import com.vctcinematics.core.CinematicManager;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    // Fuerza a que el FOV sea siempre el predeterminado del jugador
    @Inject(method = "getFov", at = @At("HEAD"), cancellable = true)
    private void lockFov(net.minecraft.client.render.Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        if (CinematicManager.isCameraActive()) {
            net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
            cir.setReturnValue(client.options.getFov().getValue().doubleValue());
        }
    }

    // BLOQUEA EL VIEW BOBBING (El temblor de cámara al caminar)
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true, require = 0)
    private void disableViewBobbing(CallbackInfo ci) {
        if (CinematicManager.isCameraActive()) {
            ci.cancel();
        }
    }
}