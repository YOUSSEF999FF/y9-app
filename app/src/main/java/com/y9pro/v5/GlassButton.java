package com.y9pro.v5;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import java.util.Random;

/**
 * Glass button. On touch: 3D press (scale + tilt), red flash, ripple from the touch point, short red
 * "electric" arcs along the edge, spring back on release. Everything starts at ACTION_DOWN.
 */
final class GlassButton extends TextView {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Path clip = new Path(), bolt = new Path();
    private final Random rnd = new Random();
    private final float dp;
    private final int tier;
    private float rx, ry, rp = 1f, flash, boltA, glow = 1f;
    private ValueAnimator rippleA, flashA, boltAnim;
    private LinearGradient fill;

    GlassButton(Context c, int tier, boolean primary) {
        super(c);
        this.tier = tier;
        dp = c.getResources().getDisplayMetrics().density;
        setGravity(Gravity.CENTER);
        setTextColor(0xFFFFFFFF);
        setTypeface(Typeface.DEFAULT_BOLD);
        setLetterSpacing(0.05f);
        setTextSize(primary ? 17f : 13f);
        setMaxLines(1);
        setEllipsize(TextUtils.TruncateAt.END);
        setClickable(true);
        setFocusable(true);
        setWillNotDraw(false);
        setPadding(Math.round(14 * dp), 0, Math.round(14 * dp), 0);
        setCameraDistance(dp * 8000f);
    }

    void setGlow(float g) { glow = g; invalidate(); }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        r.set(2 * dp, 2 * dp, w - 2 * dp, h - 2 * dp);
        fill = new LinearGradient(0, 0, 0, h, 0x66C1122B, 0xAA120306, Shader.TileMode.CLAMP);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (isEnabled()) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: down(e.getX(), e.getY()); break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: up(); break;
                default: break;
            }
        }
        return super.onTouchEvent(e);
    }

    private void down(float x, float y) {
        rx = x; ry = y;
        if (rippleA != null) rippleA.cancel();
        rippleA = UiUtil.va(0f, 1f, 380, new DecelerateInterpolator(), v -> { rp = v; invalidate(); });
        if (flashA != null) flashA.cancel();
        flashA = UiUtil.va(1f, 0f, 220, null, v -> { flash = v; invalidate(); });
        if (tier > PerfTier.LOW) {
            buildBolt();
            if (boltAnim != null) boltAnim.cancel();
            boltAnim = UiUtil.va(1f, 0f, 260, null, v -> { boltA = v; invalidate(); });
        }
        float w = Math.max(1, getWidth()), h = Math.max(1, getHeight());
        animate().scaleX(0.95f).scaleY(0.95f)
                .rotationX(-(y / h - 0.5f) * 10f).rotationY((x / w - 0.5f) * 10f)
                .setDuration(70).setInterpolator(null).start();
    }

    private void up() {
        animate().scaleX(1f).scaleY(1f).rotationX(0f).rotationY(0f)
                .setDuration(280).setInterpolator(new OvershootInterpolator(2.2f)).start();
    }

    private void buildBolt() {
        bolt.reset();
        float w = getWidth(), h = getHeight();
        for (int k = 0; k < 2; k++) {
            int edge = rnd.nextInt(4);
            float len = 0.35f + rnd.nextFloat() * 0.3f, s = rnd.nextFloat() * (1f - len);
            for (int i = 0; i <= 7; i++) {
                float f = s + len * i / 7f;
                float j = (rnd.nextFloat() - 0.5f) * 6 * dp;
                float x, y;
                if (edge == 0) { x = w * f; y = j; }
                else if (edge == 1) { x = w * f; y = h + j; }
                else if (edge == 2) { x = j; y = h * f; }
                else { x = w + j; y = h * f; }
                if (i == 0) bolt.moveTo(x, y); else bolt.lineTo(x, y);
            }
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        float rad = Math.min(getHeight() / 2f, 20 * dp);
        p.setStyle(Paint.Style.FILL);
        p.setShader(fill);
        c.drawRoundRect(r, rad, rad, p);
        p.setShader(null);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(dp);
        p.setColor(0x2EFFFFFF);
        r.inset(2.5f * dp, 2.5f * dp);
        c.drawRoundRect(r, rad - 2.5f * dp, rad - 2.5f * dp, p);
        r.inset(-2.5f * dp, -2.5f * dp);

        if (tier > PerfTier.LOW) {
            p.setColor(0xFFFF2A3D);
            p.setStrokeWidth(8 * dp);
            p.setAlpha(UiUtil.alpha(30 * glow));
            c.drawRoundRect(r, rad, rad, p);
        }
        p.setColor(0xFFFF2A3D);
        p.setStrokeWidth(1.5f * dp);
        p.setAlpha(UiUtil.alpha(255 * Math.min(1f, 0.5f + 0.5f * glow)));
        c.drawRoundRect(r, rad, rad, p);

        if (rp < 1f || flash > 0f) {
            clip.reset();
            clip.addRoundRect(r, rad, rad, Path.Direction.CW);
            c.save();
            c.clipPath(clip);
            p.setStyle(Paint.Style.FILL);
            if (flash > 0f) {
                p.setColor(0xFFFF2A3D);
                p.setAlpha(UiUtil.alpha(90 * flash));
                c.drawRect(r, p);
            }
            if (rp < 1f) {
                p.setColor(0xFFFF5A68);
                p.setAlpha(UiUtil.alpha(110 * (1f - rp)));
                c.drawCircle(rx, ry, rp * Math.max(getWidth(), getHeight()) * 1.1f, p);
            }
            c.restore();
        }

        if (boltA > 0f) {
            p.setStyle(Paint.Style.STROKE);
            p.setColor(0xFFFF2A3D);
            p.setStrokeWidth(4 * dp);
            p.setAlpha(UiUtil.alpha(70 * boltA));
            c.drawPath(bolt, p);
            p.setStrokeWidth(1.4f * dp);
            p.setColor(0xFFFFB3BA);
            p.setAlpha(UiUtil.alpha(255 * boltA));
            c.drawPath(bolt, p);
        }
        super.onDraw(c);
    }
}
