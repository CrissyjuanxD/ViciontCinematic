package com.vctcinematics.mixin.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.vctcinematics.core.CinematicManager;
import com.vctcinematics.core.Keyframe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.util.memory.ObjectAllocator;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderDebug(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f matrix4f, Matrix4f matrix4f2, Matrix4f matrix4f3, GpuBufferSlice gpuBufferSlice, Vector4f vector4f, boolean renderSky, CallbackInfo ci) {
        if (CinematicManager.debugCinematic == null || CinematicManager.debugCinematic.keyframes.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d camPos = camera.getCameraPos();
        MatrixStack matrices = new MatrixStack();
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

        for (Keyframe kf : CinematicManager.debugCinematic.keyframes) {
            matrices.push();
            matrices.translate(kf.x - camPos.x, kf.y - camPos.y + 0.8, kf.z - camPos.z);

            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-kf.yaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(kf.pitch));

            matrices.scale(-0.025F, -0.025F, 0.025F);

            String text = "Keyframe " + kf.index + " " + kf.transition.name() + " duracion:" + kf.timeMs + "ms/" + String.format("%.1f", kf.timeMs / 1000.0f) + "seg";
            float x = (float) (-client.textRenderer.getWidth(text) / 2);

            client.textRenderer.draw(text, x, 0f, 0xFFFFFF, false, matrices.peek().getPositionMatrix(), immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0x40000000, 15728880);
            matrices.pop();
        }
    }
}
