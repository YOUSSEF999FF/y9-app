package com.y9pro.v5;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

/** Rough device class used to scale effects: LOW = minimal effects, HIGH = full effects. */
final class PerfTier {
    static final int LOW = 0, MID = 1, HIGH = 2;
    private static int cached = -1;

    private PerfTier() {}

    static int get(Context c) {
        if (cached >= 0) return cached;
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        int mem = am.getMemoryClass();
        int cores = Runtime.getRuntime().availableProcessors();
        int t;
        if (am.isLowRamDevice() || mem <= 128 || cores <= 4) t = LOW;
        else if (mem >= 384 && cores >= 8 && Build.VERSION.SDK_INT >= 29) t = HIGH;
        else t = MID;
        cached = t;
        return t;
    }
}
