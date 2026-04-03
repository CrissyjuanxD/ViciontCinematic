package com.vctcinematics.core;

import org.joml.Vector3d;
import java.util.List;

public class Interpolator {

    public enum Type { LINEAR, SMOOTH }

    public static Keyframe getInterpolatedFrame(List<Keyframe> frames, long elapsedMs, Type type) {
        if (frames.isEmpty()) return new Keyframe(0, 0, 0, 0, 0, 0, TransitionType.NORMAL, 0);
        if (frames.size() == 1) return frames.get(0);

        long accumulated = 0;
        int currentIndex = 0;
        double t = 0;
        boolean found = false;

        for (int i = 0; i < frames.size() - 1; i++) {
            long segmentTime = frames.get(i).timeMs;
            if (segmentTime <= 0) segmentTime = 1;

            if (elapsedMs >= accumulated && elapsedMs < accumulated + segmentTime) {
                currentIndex = i;
                t = (double) (elapsedMs - accumulated) / segmentTime;
                found = true;
                break;
            }
            accumulated += segmentTime;
        }

        if (!found) {
            return frames.get(frames.size() - 1);
        }

        Keyframe p1 = frames.get(currentIndex);
        Keyframe p2 = frames.get(currentIndex + 1);

        if (p1.transition == TransitionType.CUT) {
            return t >= 0.999 ? p2 : p1;
        }

        if (type == Type.LINEAR) {
            return linearInterpolation(p1, p2, t);
        } else {
            // SMOOTH (Catmull-Rom)
            Keyframe p0 = currentIndex > 0 ? frames.get(currentIndex - 1) : p1;
            if (p0.transition == TransitionType.CUT) p0 = p1;

            Keyframe p3 = currentIndex < frames.size() - 2 ? frames.get(currentIndex + 2) : p2;
            if (p2.transition == TransitionType.CUT) p3 = p2;

            Vector3d v0 = new Vector3d(p0.x, p0.y, p0.z);
            Vector3d v1 = new Vector3d(p1.x, p1.y, p1.z);
            Vector3d v2 = new Vector3d(p2.x, p2.y, p2.z);
            Vector3d v3 = new Vector3d(p3.x, p3.y, p3.z);

            Vector3d pos = catmullRom(v0, v1, v2, v3, t);

            double tSmooth = easeInOutCubic(t);
            float yaw = lerpAngle(p1.yaw, p2.yaw, (float) tSmooth);
            float pitch = lerpAngle(p1.pitch, p2.pitch, (float) tSmooth);

            return new Keyframe(0, pos.x, pos.y, pos.z, yaw, pitch, TransitionType.NORMAL, 0);
        }
    }

    private static Keyframe linearInterpolation(Keyframe p1, Keyframe p2, double t) {
        double x = lerp(p1.x, p2.x, t);
        double y = lerp(p1.y, p2.y, t);
        double z = lerp(p1.z, p2.z, t);
        float yaw = lerpAngle(p1.yaw, p2.yaw, (float) t);
        float pitch = lerpAngle(p1.pitch, p2.pitch, (float) t);
        return new Keyframe(0, x, y, z, yaw, pitch, TransitionType.NORMAL, 0);
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    private static float lerpAngle(float a, float b, float t) {
        float delta = b - a;
        while (delta < -180.0f) delta += 360.0f;
        while (delta >= 180.0f) delta -= 360.0f;
        return a + delta * t;
    }
    private static double easeInOutCubic(double t) {
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    // Smooth
    private static Vector3d catmullRom(Vector3d p0, Vector3d p1, Vector3d p2, Vector3d p3, double t) {
        double t2 = t * t, t3 = t2 * t;
        double x = 0.5 * ((2 * p1.x) + (-p0.x + p2.x) * t + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3);
        double y = 0.5 * ((2 * p1.y) + (-p0.y + p2.y) * t + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3);
        double z = 0.5 * ((2 * p1.z) + (-p0.z + p2.z) * t + (2 * p0.z - 5 * p1.z + 4 * p2.z - p3.z) * t2 + (-p0.z + 3 * p1.z - 3 * p2.z + p3.z) * t3);
        return new Vector3d(x, y, z);
    }
}