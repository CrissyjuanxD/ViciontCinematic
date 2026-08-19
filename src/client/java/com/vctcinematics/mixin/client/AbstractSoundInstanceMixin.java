package com.vctcinematics.mixin.client;

import com.vctcinematics.core.CinematicManager;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSoundInstance.class)
public abstract class AbstractSoundInstanceMixin {

    @Shadow public abstract SoundCategory getCategory();

    // Método de ayuda para verificar si debemos alterar este sonido
    private boolean isCinematicSound() {
        if (CinematicManager.isCameraActive()) {
            SoundCategory cat = this.getCategory();
            return cat == SoundCategory.RECORDS || cat == SoundCategory.VOICE || cat == SoundCategory.MASTER;
        }
        return false;
    }

    // 1. Quitamos la caída de volumen por distancia
    @Inject(method = "getAttenuationType", at = @At("HEAD"), cancellable = true)
    private void removeAttenuationInCinematic(CallbackInfoReturnable<SoundInstance.AttenuationType> cir) {
        if (isCinematicSound()) {
            cir.setReturnValue(SoundInstance.AttenuationType.NONE);
        }
    }

    // 2. Le decimos al juego que este sonido es "Relativo" a la cabeza del jugador (como la música o la UI)
    @Inject(method = "isRelative", at = @At("HEAD"), cancellable = true)
    private void forceRelative(CallbackInfoReturnable<Boolean> cir) {
        if (isCinematicSound()) {
            cir.setReturnValue(true);
        }
    }

    // 3. Forzamos que las coordenadas del sonido sean exactamente el centro (0, 0, 0)
    @Inject(method = "getX", at = @At("HEAD"), cancellable = true)
    private void forceX(CallbackInfoReturnable<Double> cir) {
        if (isCinematicSound()) cir.setReturnValue(0.0D);
    }

    @Inject(method = "getY", at = @At("HEAD"), cancellable = true)
    private void forceY(CallbackInfoReturnable<Double> cir) {
        if (isCinematicSound()) cir.setReturnValue(0.0D);
    }

    @Inject(method = "getZ", at = @At("HEAD"), cancellable = true)
    private void forceZ(CallbackInfoReturnable<Double> cir) {
        if (isCinematicSound()) cir.setReturnValue(0.0D);
    }
}