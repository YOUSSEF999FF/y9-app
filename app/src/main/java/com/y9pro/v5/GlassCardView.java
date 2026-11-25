package com.y9pro.v5;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.Gravity;
import android.widget.LinearLayout;

/** Dark translucent "glass" card with a neon-red edge. Fake glow = 2 cheap wide strokes (no blur filters). */
final class GlassCardView extends LinearLayout {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final float dp;
    private final int tier;
    private float glow = 1f, flash = 0f;
    private LinearGradient fill;

    GlassCardView(Context c, int tier) {
        super(c);
        this.tier = tier;
        dp = c.getResources().getDisplayMetrics().density;
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        setWillNotDraw(false);
        int pad = Math.round(22 * dp);
        setPadding(pad, pad, pad, pad);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        r.set(2 * dp, 2 * dp, w - 2 * dp, h - 2 * dp);
        fill = new LinearGradient(0, 0, 0, h, 0xD0140508, 0xD0060102, Shader.TileMode.CLAMP);
    }

    void setGlow(float g) { glow = g; invalidate(); }

    void setFlash(float f) { flash = f; invalidate(); }

    @Override
    protected void onDraw(Canvas c) {
        float rad = 24 * dp;
        p.setStyle(Paint.Style.FILL);
        p.setShader(fill);
        c.drawRoundRect(r, rad, rad, p);
        p.setShader(null);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp);
        p.setColor(0x26FFFFFF); // inner glass highlight
        r.inset(3 * dp, 3 * dp);
        c.drawRoundRect(r, rad - 3 * dp, rad - 3 * dp, p);
        r.inset(-3 * dp, -3 * dp);

        float g = glow * (0.7f + flash * 1.5f);
        if (tier > PerfTier.LOW) {
            p.setColor(0xFFFF2A3D);
            p.setStrokeWidth(12 * dp);
            p.setAlpha(UiUtil.alpha(22 * g));
            c.drawRoundRect(r, rad, rad, p);
            p.setStrokeWidth(6 * dp);
            p.setAlpha(UiUtil.alpha(46 * g));
            c.drawRoundRect(r, rad, rad, p);
        }
        p.setColor(0xFFFF2A3D);
        p.setStrokeWidth(1.6f * dp);
        p.setAlpha(UiUtil.alpha(255 * Math.min(1f, glow * (0.75f + flash))));
        c.drawRoundRect(r, rad, rad, p);
    }
}
