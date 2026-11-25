package com.y9pro.v5;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.TypedValue;
import android.view.View;
import java.util.function.Consumer;

final class UiUtil {
    private UiUtil() {}

    static float dpf(Context c, float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics());
    }

    static int dp(Context c, float v) {
        return Math.round(dpf(c, v));
    }

    static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    /** Progress of t inside [a, b], clamped to 0..1. */
    static float seg(float t, float a, float b) {
        return clamp01((t - a) / (b - a));
    }

    static int alpha(float a) {
        return Math.max(0, Math.min(255, Math.round(a)));
    }

    /** Small helper so we never need reflection-based property animators. */
    static ValueAnimator va(float from, float to, long ms, TimeInterpolator ip, Consumer<Float> onValue) {
        ValueAnimator a = ValueAnimator.ofFloat(from, to);
        a.setDuration(ms);
        if (ip != null) a.setInterpolator(ip);
        a.addUpdateListener(x -> onValue.accept((Float) x.getAnimatedValue()));
        a.start();
        return a;
    }

    static void shake(View v, float ampPx) {
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(380);
        a.addUpdateListener(x -> {
            float p = (Float) x.getAnimatedValue();
            v.setTranslationX((float) (Math.sin(p * Math.PI * 6) * (1f - p) * ampPx));
        });
        a.start();
    }

    /** Only http(s) and tg:// links are ever opened, always outside the app. */
    static void openExternal(Context c, String url) {
        if (url == null || url.trim().isEmpty()) return;
        Uri u = Uri.parse(url.trim());
        String s = u.getScheme();
        if (s == null || !(s.equals("https") || s.equals("http") || s.equals("tg"))) return;
        try {
            c.startActivity(new Intent(Intent.ACTION_VIEW, u).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException ignored) {
        }
    }

    @SuppressWarnings("deprecation")
    static void immersive(Activity a) {
        a.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
