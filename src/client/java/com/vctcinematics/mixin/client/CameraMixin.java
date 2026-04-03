package com.vctcinematics.mixin.client;

import com.vctcinematics.core.CinematicManager;
import com.vctcinematics.core.Keyframe;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void overrideCamera(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (CinematicManager.isCameraActive()) {
            Keyframe kf = CinematicManager.getCurrentCameraState();
            if (kf != null) {
                this.setPos(kf.x, kf.y, kf.z);
                this.setRotation(kf.yaw, kf.pitch);
            }
        }
    }
}