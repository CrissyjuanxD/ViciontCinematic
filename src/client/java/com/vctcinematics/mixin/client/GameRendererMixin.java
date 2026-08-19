package com.vctcinematics.mixin.client;

import com.vctcinematics.core.CinematicManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "getFov", at = @At("HEAD"), cancellable = true)
    private void lockFov(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        if (CinematicManager.isCameraActive()) {
            net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
            cir.setReturnValue(client.options.getFov().getValue().floatValue());
        }
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true, require = 0)
    private void disableViewBobbing(MatrixStack matrices, float tickProgress, CallbackInfo ci) {
        if (CinematicManager.isCameraActive()) {
            ci.cancel();
        }
    }
}
