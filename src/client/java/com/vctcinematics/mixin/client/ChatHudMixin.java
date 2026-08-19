package com.vctcinematics.mixin.client;

import com.vctcinematics.core.CinematicManager;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void hideChatDuringCinematic(DrawContext context, TextRenderer textRenderer, int x, int y, int width, boolean isChatFocused, boolean showBorder, CallbackInfo ci) {
        if (CinematicManager.isPlaying) ci.cancel();
    }
}
