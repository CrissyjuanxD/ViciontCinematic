package com.vctcinematics.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Cinematic {
    public String name;
    public List<Keyframe> keyframes = new ArrayList<>();

    public Cinematic(String name) {
        this.name = name;
    }

    public void addOrUpdateKeyframe(Keyframe kf) {
        keyframes.removeIf(k -> k.index == kf.index);
        keyframes.add(kf);
        Collections.sort(keyframes); // Mantiene el orden
    }

    public boolean removeKeyframe(int index) {
        return keyframes.removeIf(k -> k.index == index);
    }

    public boolean editKeyframe(int index, TransitionType transition, long timeMs) {
        for (Keyframe kf : keyframes) {
            if (kf.index == index) {
                kf.transition = transition;
                kf.timeMs = timeMs;
                return true;
            }
        }
        return false;
    }

    public long getTotalDuration() {
        long total = 0;
        if (keyframes.isEmpty()) return 0;
        for (int i = 0; i < keyframes.size() - 1; i++) {
            total += keyframes.get(i).timeMs;
        }
        return total;
    }
}