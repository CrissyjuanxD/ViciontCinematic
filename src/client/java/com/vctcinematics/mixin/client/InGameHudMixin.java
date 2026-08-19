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

    @Inject(method = "render", at = @At("TAIL"))
    private void renderCinematicFade(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (CinematicManager.isPlaying) {
            float alpha = CinematicManager.getFadeAlpha();
            if (alpha > 0.0f) {
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
                com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
                com.mojang.blaze3d.systems.RenderSystem.depthMask(false);

                // Levantamos el Z a 5000 para tapar los HUDs de otros mods
                context.getMatrices().push();
                context.getMatrices().translate(0, 0, 5000);

                int color = ((int) (alpha * 255.0f) << 24) | 0x000000;
                MinecraftClient client = MinecraftClient.getInstance();
                context.fill(0, 0, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight(), color);

                context.getMatrices().pop();

                com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
                com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
                com.mojang.blaze3d.systems.RenderSystem.disableBlend();
            }
        }
    }

    // 2. Ocultar partes del HUD individualmente

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

    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true, require = 0)
    private void hideExpLevelDuringCinematic(CallbackInfo ci) {
        if (CinematicManager.isPlaying) ci.cancel();
    }

    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true, require = 0)
    private void hideStatusBars(CallbackInfo ci) {
        if (CinematicManager.isPlaying) ci.cancel();
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void hideEffectOverlay(CallbackInfo ci) {
        if (CinematicManager.isPlaying) ci.cancel();
    }

    // NUEVO: Oculta el nombre de los ítems en la mano
    @Inject(method = "renderHeldItemTooltip", at = @At("HEAD"), cancellable = true, require = 0)
    private void hideHeldItemTooltip(CallbackInfo ci) {
        if (CinematicManager.isPlaying) ci.cancel();
    }
}