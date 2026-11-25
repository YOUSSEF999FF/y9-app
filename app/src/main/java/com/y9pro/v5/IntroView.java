package com.y9pro.v5;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import java.util.Random;

/**
 * Page 1 cinematic intro, drawn with one Canvas and ONE animator (no per-frame allocations):
 *  0.00-0.18 background fades in with a soft red glow
 *  0.15-0.38 "Y9 PRO V5" glass plate + title pop up
 *  0.38-0.66 glow rises, glass reflection sweeps across
 *  0.55-0.80 light burst + a few red particles
 *  0.66-1.00 title is "engraved" into the screen (reveal sweep + sparks)
 * The frame time is watched: if the device is slow the effects are dropped automatically.
 */
final class IntroView extends View {
    private static final String TITLE = "Y9 PRO V5";
    private static final long DURATION = 2600L;
    private static final int RED = 0xFFFF2A3D;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF plate = new RectF();
    private final Path clip = new Path();
    private final OvershootInterpolator overshoot = new OvershootInterpolator(1.7f);
    private final float dp;
    private int tier;
    private float t, cx, cy, bandW, maxR;
    private RadialGradient bgGlow, flashGlow;
    private LinearGradient plateFill, sweep;
    private float[] ang, spd, sz;
    private ValueAnimator anim;
    private Runnable pendingDone;
    private boolean started, cancelled;
    private long lastNs;
    private int frames, slow;

    IntroView(Context c, int tier) {
        super(c);
        this.tier = tier;
        dp = c.getResources().getDisplayMetrics().density;
        tp.setTypeface(Typeface.DEFAULT_BOLD);
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setLetterSpacing(0.08f);
        setupParticles();
    }

    void start(Runnable onDone) {
        pendingDone = onDone;
        if (getWidth() > 0) begin();
    }

    void stop() {
        cancelled = true;
        if (anim != null) anim.cancel();
    }

    private void begin() {
        if (started) return;
        started = true;
        final Runnable done = pendingDone;
        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(DURATION);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(a -> { t = (Float) a.getAnimatedValue(); invalidate(); });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator a) { if (!cancelled && done != null) done.run(); }
        });
        anim.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    private void setupParticles() {
        int n = tier >= PerfTier.HIGH ? 28 : (tier == PerfTier.MID ? 14 : 0);
        ang = new float[n]; spd = new float[n]; sz = new float[n];
        Random r = new Random(7);
        for (int i = 0; i < n; i++) {
            ang[i] = (float) (r.nextFloat() * Math.PI * 2);
            spd[i] = 0.35f + r.nextFloat() * 0.65f;
            sz[i] = (1.5f + r.nextFloat() * 2f) * dp;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        cx = w / 2f;
        cy = h / 2f - h * 0.02f;
        maxR = Math.max(w, h) * 0.55f;
        float pw = Math.min(w * 0.80f, 380 * dp), ph = 112 * dp;
        plate.set(cx - pw / 2f, cy - ph / 2f, cx + pw / 2f, cy + ph / 2f);
        bgGlow = new RadialGradient(cx, cy, Math.max(w, h) * 0.6f,
                new int[]{0x99B3001B, 0x33400008, 0x00000000}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP);
        flashGlow = new RadialGradient(cx, cy, w * 0.7f,
                new int[]{0xFFFFD9DD, 0xFFFF2A3D, 0x00FF2A3D}, new float[]{0f, 0.35f, 1f}, Shader.TileMode.CLAMP);
        plateFill = new LinearGradient(0, plate.top, 0, plate.bottom, 0x26FFFFFF, 0x08FFFFFF, Shader.TileMode.CLAMP);
        bandW = plate.width() * 0.35f;
        sweep = new LinearGradient(0, 0, bandW, 0, new int[]{0x00FFFFFF, 0x40FFFFFF, 0x00FFFFFF},
                null, Shader.TileMode.CLAMP);
        tp.setTextSize(ph * 0.44f);
        float tw = tp.measureText(TITLE), max = pw * 0.86f;
        if (tw > max) tp.setTextSize(tp.getTextSize() * max / tw);
        if (pendingDone != null && !started) begin();
    }

    private void adapt() {
        long now = System.nanoTime();
        if (lastNs != 0) {
            long dt = now - lastNs;
            frames++;
            if (frames > 5 && dt > 30_000_000L) {
                if (++slow >= 4 && tier > PerfTier.LOW) { tier--; slow = 0; setupParticles(); }
            } else if (dt < 22_000_000L) {
                slow = 0;
            }
        }
        lastNs = now;
    }

    @Override
    protected void onDraw(Canvas c) {
        adapt();
        c.drawColor(Color.BLACK);
        int w = getWidth(), h = getHeight();

        p.setStyle(Paint.Style.FILL);
        p.setShader(bgGlow);
        p.setAlpha(UiUtil.alpha(255 * ease(UiUtil.seg(t, 0f, 0.18f))));
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);

        float a = UiUtil.seg(t, 0.15f, 0.27f);
        if (a > 0f) {
            float pop = overshoot.getInterpolation(UiUtil.seg(t, 0.15f, 0.38f));
            float g = 0.35f + 0.65f * UiUtil.seg(t, 0.38f, 0.58f) + burstPulse();
            c.save();
            c.scale(pop, pop, cx, cy);
            drawPlate(c, a, g);
            drawTitle(c, a);
            c.restore();
        }
        drawBurst(c, w, h);
    }

    private float burstPulse() {
        float bp = UiUtil.seg(t, 0.55f, 0.80f);
        return (bp > 0f && bp < 1f) ? (float) Math.sin(Math.PI * bp) * 0.8f : 0f;
    }

    private void drawPlate(Canvas c, float a, float g) {
        float rad = plate.height() * 0.26f;
        p.setStyle(Paint.Style.FILL);
        p.setShader(plateFill);
        p.setAlpha(UiUtil.alpha(255 * a));
        c.drawRoundRect(plate, rad, rad, p);
        p.setShader(null);

        p.setStyle(Paint.Style.STROKE);
        if (tier > PerfTier.LOW) {
            p.setColor(RED);
            p.setStrokeWidth(10 * dp);
            p.setAlpha(UiUtil.alpha(22 * g * a));
            c.drawRoundRect(plate, rad, rad, p);
            p.setStrokeWidth(5 * dp);
            p.setAlpha(UiUtil.alpha(50 * g * a));
            c.drawRoundRect(plate, rad, rad, p);
        }
        p.setColor(RED);
        p.setStrokeWidth(1.6f * dp);
        p.setAlpha(UiUtil.alpha(255 * Math.min(1f, 0.6f + 0.4f * g) * a));
        c.drawRoundRect(plate, rad, rad, p);

        float sp = UiUtil.seg(t, 0.38f, 0.66f);
        if (tier > PerfTier.LOW && sp > 0f && sp < 1f) {
            clip.reset();
            clip.addRoundRect(plate, rad, rad, Path.Direction.CW);
            c.save();
            c.clipPath(clip);
            c.translate(plate.left - bandW + (plate.width() + bandW) * sp, plate.top);
            c.skew(-0.3f, 0f);
            p.setStyle(Paint.Style.FILL);
            p.setShader(sweep);
            p.setAlpha(255);
            c.drawRect(0, 0, bandW, plate.height(), p);
            p.setShader(null);
            c.restore();
        }
    }

    private void drawTitle(Canvas c, float a) {
        float e = UiUtil.seg(t, 0.66f, 1f);
        float base = plate.centerY() - (tp.ascent() + tp.descent()) / 2f;
        tp.setStyle(Paint.Style.FILL);
        tp.setColor(0xFFFFFFFF);
        tp.setAlpha(UiUtil.alpha(235 * a * (1f - 0.65f * e)));
        c.drawText(TITLE, cx, base, tp);
        if (e <= 0f) return;

        float reveal = Math.min(1f, e * 1.15f);
        float edgeX = plate.left + plate.width() * reveal;
        c.save();
        c.clipRect(plate.left, plate.top, edgeX, plate.bottom);
        tp.setColor(0xFF000000); tp.setAlpha(200);
        c.drawText(TITLE, cx - 1.6f * dp, base - 1.6f * dp, tp);   // inner shadow
        tp.setColor(RED); tp.setAlpha(230);
        c.drawText(TITLE, cx + 1.2f * dp, base + 1.2f * dp, tp);   // lit lower edge
        tp.setColor(0xFF5A0010); tp.setAlpha(255);
        c.drawText(TITLE, cx, base, tp);                           // engraved fill
        c.restore();
        if (e < 1f && tier > PerfTier.LOW) sparks(c, edgeX, plate.centerY());
    }

    private void sparks(Canvas c, float x, float y) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.4f * dp);
        p.setColor(RED);
        for (int i = 0; i < 6; i++) {
            double ang = Math.sin(t * 80 + i * 12.9898) * Math.PI;
            float len = (5 + 9 * (float) Math.abs(Math.sin(t * 55 + i * 4.1))) * dp;
            p.setAlpha(UiUtil.alpha(220 * (1f - 0.12f * i)));
            c.drawLine(x, y, x + (float) Math.cos(ang) * len, y + (float) Math.sin(ang) * len, p);
        }
    }

    private void drawBurst(Canvas c, int w, int h) {
        float bp = UiUtil.seg(t, 0.55f, 0.80f);
        if (bp <= 0f || bp >= 1f) return;
        p.setStyle(Paint.Style.FILL);
        p.setShader(flashGlow);
        p.setAlpha(UiUtil.alpha(150 * (float) Math.sin(Math.PI * bp)));
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);
        if (ang.length == 0) return;
        float r = (1f - (1f - bp) * (1f - bp)) * maxR;
        p.setColor(RED);
        for (int i = 0; i < ang.length; i++) {
            float rr = r * spd[i];
            p.setAlpha(UiUtil.alpha(255 * (1f - bp)));
            c.drawCircle(cx + (float) Math.cos(ang[i]) * rr, cy + (float) Math.sin(ang[i]) * rr,
                    sz[i] * (1f - 0.5f * bp), p);
        }
    }

    private static float ease(float x) { return x * x * (3f - 2f * x); }
}
