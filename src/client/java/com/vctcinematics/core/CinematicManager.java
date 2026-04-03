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

    private static final long FADE_IN = 1400;
    private static final long FADE_STAY = 400;
    private static final long FADE_OUT = 1400;

    private static Perspective previousPerspective;
    private static boolean cameraRestored = false;

    public static void play(String name, Interpolator.Type type, boolean useFade) {
        activeCinematic = cinematics.get(name);
        if (activeCinematic == null || activeCinematic.keyframes.size() < 2) return;

        totalDurationMs = activeCinematic.getTotalDuration();
        currentType = type;
        hasFade = useFade;
        cameraRestored = false;

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
        cinematicEndTime = cinematicStartTime + totalDurationMs;
        isPlaying = true;
    }

    // MODIFICADO: Ahora exige el nombre para saber si debe detenerse
    public static void stop(String name) {
        if (!isPlaying || activeCinematic == null || !activeCinematic.name.equalsIgnoreCase(name)) return;
        long now = System.currentTimeMillis();

        if (hasFade && now < cinematicEndTime - (FADE_IN + FADE_STAY)) {
            cinematicEndTime = now + (FADE_IN + FADE_STAY);
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

        if (isPlaying) {
            if (hasFade) {
                if (now >= cinematicEndTime && !cameraRestored) {
                    restoreCamera();
                }
                if (now >= cinematicEndTime + FADE_OUT) {
                    endCinematic();
                }
            } else {
                if (now >= cinematicEndTime) {
                    endCinematic();
                }
            }
        }

        // SISTEMA DE DEBUG VISUAL (Limpiado Bezier y Spline)
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
            return now >= fadeStartMs + FADE_IN && now < cinematicEndTime;
        }
        return now >= cinematicStartTime && now <= cinematicEndTime;
    }

    public static Keyframe getCurrentCameraState() {
        long elapsed = System.currentTimeMillis() - cinematicStartTime;
        if (elapsed < 0) elapsed = 0;
        if (elapsed > totalDurationMs) elapsed = totalDurationMs;
        return Interpolator.getInterpolatedFrame(activeCinematic.keyframes, elapsed, currentType);
    }

    public static float getFadeAlpha() {
        if (!hasFade || !isPlaying) return 0.0f;
        long now = System.currentTimeMillis();

        if (now < fadeStartMs) return 0.0f;

        if (now < cinematicStartTime) {
            long elapsedFade = now - fadeStartMs;
            return elapsedFade < FADE_IN ? (float) elapsedFade / FADE_IN : 1.0f;
        } else if (now < cinematicStartTime + FADE_OUT) {
            long elapsedClear = now - cinematicStartTime;
            return 1.0f - ((float) elapsedClear / FADE_OUT);
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