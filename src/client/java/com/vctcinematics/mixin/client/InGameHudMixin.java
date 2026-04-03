package com.vctcinematics.mixin.client;

import com.vctcinematics.core.CinematicManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    // 1. Renderizar el FADE al final de todo el HUD (Aquí sí necesitamos los parámetros para dibujar)
    @Inject(method = "render", at = @At("TAIL"))
    private void renderCinematicFade(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (CinematicManager.isPlaying) {
            float alpha = CinematicManager.getFadeAlpha();
            if (alpha > 0.0f) {
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
                int color = ((int) (alpha * 255.0f) << 24) | 0x000000;
                MinecraftClient client = MinecraftClient.getInstance();
                context.fill(0, 0, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight(), color);
                com.mojang.blaze3d.systems.RenderSystem.disableBlend();
            }
        }
    }

    // 2. Ocultar partes del HUD individualmente con la técnica segura (CallbackInfo)

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true, require = 0)
    private void hideHotbar(CallbackInfo ci) {
        if (CinematicManager.isPlaying) ci.cancel();
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void hideCrosshair(CallbackInfo ci) {
        if (CinematicManager.isPlaying) ci.cancel();
    }

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true, require = 0)
    private void hideExpBar(CallbackInfo ci) {
        if (CinematicManager.isPlaying) ci.cancel();
    }

    // renderStatusBars engloba vida, armadura, burbujas de aire y comida.
    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true, require = 0)
    private void hideStatusBars(CallbackInfo ci) {
        if (CinematicManager.isPlaying) ci.cancel();
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void hideEffectOverlay(CallbackInfo ci) {
        if (CinematicManager.isPlaying) ci.cancel();
    }
}