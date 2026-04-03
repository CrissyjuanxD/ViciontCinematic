package com.vctcinematics.mixin.client;

import com.vctcinematics.core.CinematicManager;
import com.vctcinematics.core.Keyframe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderDebug(RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        if (CinematicManager.debugCinematic == null || CinematicManager.debugCinematic.keyframes.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d camPos = camera.getPos();
        MatrixStack matrices = new MatrixStack();
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

        for (Keyframe kf : CinematicManager.debugCinematic.keyframes) {
            matrices.push();
            // Trasladamos el punto 0,0,0 al centro del Keyframe (relativo a la cámara)
            matrices.translate(kf.x - camPos.x, kf.y - camPos.y + 0.8, kf.z - camPos.z);

            // Hacemos que el texto "mire" hacia la dirección que el keyframe tiene guardada (como un NPC)
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-kf.yaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(kf.pitch));

            // Volteamos y escalamos
            matrices.scale(-0.025F, -0.025F, 0.025F);

            String text = "Keyframe " + kf.index + " " + kf.transition.name() + " duracion:" + kf.timeMs + "ms/" + String.format("%.1f", kf.timeMs / 1000.0f) + "seg";
            float x = (float) (-client.textRenderer.getWidth(text) / 2);

            client.textRenderer.draw(text, x, 0f, 0xFFFFFF, false, matrices.peek().getPositionMatrix(), immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0x40000000, 15728880);
            matrices.pop();
        }
    }
}