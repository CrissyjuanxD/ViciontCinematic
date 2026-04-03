package com.vctcinematics.core;

public class Keyframe implements Comparable<Keyframe> {
    public int index;
    public double x, y, z;
    public float yaw, pitch;
    public TransitionType transition;
    public long timeMs;

    public Keyframe(int index, double x, double y, double z, float yaw, float pitch, TransitionType transition, long timeMs) {
        this.index = index;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.transition = transition;
        this.timeMs = timeMs;
    }

    // Ordena automáticamente la lista de keyframes por su número
    @Override
    public int compareTo(Keyframe o) {
        return Integer.compare(this.index, o.index);
    }
}