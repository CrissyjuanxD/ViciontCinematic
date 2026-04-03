package com.vctcinematics.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import java.util.HashMap;
import java.util.Map;

public class CinematicManager {
    public static final Map<String, Cinematic> cinematics = new HashMap<>();

    public static Cinematic activeCinematic = null;
    public static Cinematic debugCinematic = null;
    public static Interpolator.Type debugLineType = null;

    public static boolean isPlaying = false;
    public static boolean hasFade = false;

    private static long totalDurationMs;
    private static Interpolator.Type currentType = Interpolator.Type.SMOOTH;

    private static long fadeStartMs;
    private static long cinematicStartTime;
    private static long cinematicEndTime;

    // Variables de bucle
    private static int currentLoopCount = 1;
    private static boolean isInfinite = false;

    private static final long FADE_IN = 1400;
    private static final long FADE_STAY = 400;
    private static final long FADE_OUT = 1400;

    private static Perspective previousPerspective;
    private static boolean cameraRestored = false;

    public static void play(String name, Interpolator.Type type, boolean useFade, int loop) {
        activeCinematic = cinematics.get(name);
        if (activeCinematic == null || activeCinematic.keyframes.size() < 2) return;

        totalDurationMs = activeCinematic.getTotalDuration();
        currentType = type;
        hasFade = useFade;
        cameraRestored = false;

        // Si mandan 0 o 1, es 1 vuelta. Si es -1, es infinito.
        currentLoopCount = (loop <= 0 && loop != -1) ? 1 : loop;
        isInfinite = (currentLoopCount == -1);

        MinecraftClient client = MinecraftClient.getInstance();
        previousPerspective = client.options.getPerspective();
        client.options.setPerspective(Perspective.THIRD_PERSON_BACK);

        long now = System.currentTimeMillis();
        if (hasFade) {
            fadeStartMs = now;
            cinematicStartTime = now + (FADE_IN + FADE_STAY);
        } else {
            cinematicStartTime = now;
        }

        if (!isInfinite) {
            cinematicEndTime = cinematicStartTime + (totalDurationMs * currentLoopCount);
        } else {
            cinematicEndTime = -1; // No se usará hasta que alguien lo detenga manual
        }

        isPlaying = true;
    }

    public static void stop(String name) {
        if (!isPlaying || activeCinematic == null || !activeCinematic.name.equalsIgnoreCase(name)) return;
        long now = System.currentTimeMillis();

        if (hasFade) {
            // Si es infinito o la interrumpimos a la mitad, forzamos el FadeOut AHORA MISMO
            if (isInfinite || now < cinematicEndTime - (FADE_IN + FADE_STAY)) {
                cinematicEndTime = now + (FADE_IN + FADE_STAY);
                isInfinite = false; // Le quitamos lo infinito para que pueda morir
            }
        } else {
            endCinematic();
        }
    }

    private static void restoreCamera() {
        if (!cameraRestored) {
            cameraRestored = true;
            MinecraftClient client = MinecraftClient.getInstance();
            if (previousPerspective != null) {
                client.options.setPerspective(previousPerspective);
            }
        }
    }

    private static void endCinematic() {
        isPlaying = false;
        activeCinematic = null;
        restoreCamera();
    }

    public static void tick(MinecraftClient client) {
        long now = System.currentTimeMillis();

        if (isPlaying && !isInfinite) {
            if (hasFade) {
                if (now >= cinematicEndTime && !cameraRestored) restoreCamera();
                if (now >= cinematicEndTime + FADE_OUT) endCinematic();
            } else {
                if (now >= cinematicEndTime) endCinematic();
            }
        }

        // SISTEMA DE DEBUG VISUAL
        if (debugCinematic != null && client.world != null) {
            for (Keyframe kf : debugCinematic.keyframes) {
                if (Math.random() < 0.4) {
                    client.world.addParticle(ParticleTypes.END_ROD, kf.x, kf.y, kf.z, 0, 0.01, 0);
                }
            }

            if (debugLineType != null) {
                long total = debugCinematic.getTotalDuration();
                if (total > 0) {
                    long step = Math.max(10, total / 200);
                    for (long time = 0; time <= total; time += step) {
                        if (Math.random() < 0.1) {
                            Keyframe curr = Interpolator.getInterpolatedFrame(debugCinematic.keyframes, time, debugLineType);
                            ParticleEffect p = (debugLineType == Interpolator.Type.SMOOTH) ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME;
                            client.world.addParticle(p, curr.x, curr.y, curr.z, 0, 0, 0);
                        }
                    }
                }
            }
        }
    }

    public static boolean isCameraActive() {
        if (!isPlaying || activeCinematic == null) return false;
        long now = System.currentTimeMillis();
        if (hasFade) {
            if (isInfinite) return now >= fadeStartMs + FADE_IN;
            return now >= fadeStartMs + FADE_IN && now < cinematicEndTime;
        }
        if (isInfinite) return now >= cinematicStartTime;
        return now >= cinematicStartTime && now <= cinematicEndTime;
    }

    public static Keyframe getCurrentCameraState() {
        long elapsed = System.currentTimeMillis() - cinematicStartTime;
        if (elapsed < 0) elapsed = 0;

        if (!isInfinite && elapsed > totalDurationMs * currentLoopCount) {
            elapsed = totalDurationMs * currentLoopCount;
        }

        long loopElapsed;
        if (totalDurationMs == 0) {
            loopElapsed = 0;
        } else {
            loopElapsed = elapsed % totalDurationMs;
            
            if (!isInfinite && elapsed > 0 && elapsed == totalDurationMs * currentLoopCount) {
                loopElapsed = totalDurationMs;
            }
        }

        return Interpolator.getInterpolatedFrame(activeCinematic.keyframes, loopElapsed, currentType);
    }

    public static float getFadeAlpha() {
        if (!hasFade || !isPlaying) return 0.0f;
        long now = System.currentTimeMillis();

        if (now < fadeStartMs) return 0.0f;

        if (now < cinematicStartTime) {
            long elapsedFade = now - fadeStartMs;
            return elapsedFade < FADE_IN ? (float) elapsedFade / FADE_IN : 1.0f;
        }

        if (isInfinite) return 0.0f;

        if (now < cinematicEndTime - (FADE_IN + FADE_STAY)) {
            return 0.0f;
        } else if (now > cinematicEndTime - (FADE_IN + FADE_STAY) && now <= cinematicEndTime - FADE_STAY) {
            long elapsedEndFade = now - (cinematicEndTime - (FADE_IN + FADE_STAY));
            return (float) elapsedEndFade / FADE_IN;
        } else if (now > cinematicEndTime - FADE_STAY && now <= cinematicEndTime) {
            return 1.0f;
        } else if (now > cinematicEndTime && now < cinematicEndTime + FADE_OUT) {
            long elapsedEndClear = now - cinematicEndTime;
            return 1.0f - ((float) elapsedEndClear / FADE_OUT);
        }
        return 0.0f;
    }
}