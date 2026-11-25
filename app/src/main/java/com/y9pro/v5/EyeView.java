package com.y9pro.v5;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Show/hide-password icon (drawn, so no image assets are needed). */
final class EyeView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path eye = new Path();
    private boolean shown;

    EyeView(Context c) {
        super(c);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(1.7f * c.getResources().getDisplayMetrics().density);
        p.setColor(0xCCFFFFFF);
    }

    boolean isRevealed() { return shown; }

    void toggle() { shown = !shown; invalidate(); }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        eye.reset();
        eye.moveTo(0.08f * w, 0.5f * h);
        eye.quadTo(0.5f * w, 0.05f * h, 0.92f * w, 0.5f * h);
        eye.quadTo(0.5f * w, 0.95f * h, 0.08f * w, 0.5f * h);
        c.drawPath(eye, p);
        c.drawCircle(0.5f * w, 0.5f * h, 0.14f * w, p);
        if (!shown) c.drawLine(0.2f * w, 0.85f * h, 0.8f * w, 0.15f * h, p);
    }
}
