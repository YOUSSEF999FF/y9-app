package com.y9pro.v5;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/** Red/black glass shield with a light "lock bar" (same shape language as the reference video). */
final class ShieldView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF bar = new RectF();
    private LinearGradient fill, shineGrad;
    private float x0, y0, s, bandW, shine = -1f;

    ShieldView(Context c) { super(c); }

    /** 0..1 moves a soft highlight across the shield; any value outside 0..1 hides it. */
    void setShine(float v) { shine = v; invalidate(); }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        s = Math.min(w, h);
        x0 = (w - s) / 2f;
        y0 = (h - s) / 2f;
        path.reset();
        path.moveTo(X(.5f), Y(.04f));
        path.lineTo(X(.88f), Y(.17f));
        path.lineTo(X(.88f), Y(.50f));
        path.cubicTo(X(.88f), Y(.74f), X(.70f), Y(.90f), X(.5f), Y(.98f));
        path.cubicTo(X(.30f), Y(.90f), X(.12f), Y(.74f), X(.12f), Y(.50f));
        path.lineTo(X(.12f), Y(.17f));
        path.close();
        bar.set(X(.455f), Y(.36f), X(.545f), Y(.62f));
        fill = new LinearGradient(0, y0, 0, y0 + s, 0x88FF2A3D, 0x44200006, Shader.TileMode.CLAMP);
        bandW = s * 0.45f;
        shineGrad = new LinearGradient(0, 0, bandW, 0, new int[]{0x00FFFFFF, 0x66FFFFFF, 0x00FFFFFF},
                null, Shader.TileMode.CLAMP);
    }

    private float X(float u) { return x0 + u * s; }

    private float Y(float v) { return y0 + v * s; }

    @Override
    protected void onDraw(Canvas c) {
        if (s <= 0) return;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setColor(0xFFFF2A3D);
        p.setStrokeWidth(s * 0.13f);
        p.setAlpha(40);
        c.drawPath(path, p); // soft outer glow

        p.setStyle(Paint.Style.FILL);
        p.setShader(fill);
        c.drawPath(path, p);
        p.setShader(null);

        if (shine >= 0f && shine <= 1f) {
            c.save();
            c.clipPath(path);
            c.translate(x0 - bandW + (s + bandW) * shine, y0);
            c.skew(-0.25f, 0f);
            p.setShader(shineGrad);
            c.drawRect(0, 0, bandW, s, p);
            p.setShader(null);
            c.restore();
        }

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(s * 0.055f);
        p.setColor(0xFFFFD9DD); // glass rim
        p.setAlpha(210);
        c.drawPath(path, p);
        p.setStrokeWidth(s * 0.02f);
        p.setColor(0xFFFF2A3D); // neon edge
        c.drawPath(path, p);

        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFFFD9DD); // lock bar
        p.setAlpha(235);
        c.drawRoundRect(bar, s * 0.02f, s * 0.02f, p);
    }
}
