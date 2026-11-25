package com.y9pro.v5;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Glass dialog: title + message + Retry, optionally with the 6-language bar (used for "no internet"). */
final class NoticeDialog extends FrameLayout {
    private final String titleKey, msgKey;
    private final Runnable onRetry;
    private final View scrim;
    private final GlassCardView card;
    private final TextView title, msg;
    private final GlassButton retry;
    private final TextView[] chips = new TextView[I18n.CODES.length];
    private final float dp;
    private String lang;
    private boolean busy;

    NoticeDialog(Context c, int tier, String titleKey, String msgKey, boolean langBar, Runnable onRetry) {
        super(c);
        this.titleKey = titleKey;
        this.msgKey = msgKey;
        this.onRetry = onRetry;
        dp = c.getResources().getDisplayMetrics().density;
        lang = Prefs.lang(c);
        setClickable(true); // swallow touches behind the dialog

        scrim = new View(c);
        scrim.setBackgroundColor(Color.BLACK);
        scrim.setAlpha(0f);
        addView(scrim, new LayoutParams(-1, -1));

        card = new GlassCardView(c, tier);
        int w = Math.min((int) (c.getResources().getDisplayMetrics().widthPixels * 0.88f), UiUtil.dp(c, 380));
        addView(card, new LayoutParams(w, -2, Gravity.CENTER));

        if (langBar) {
            LinearLayout row = new LinearLayout(c);
            row.setGravity(Gravity.CENTER);
            for (int i = 0; i < chips.length; i++) {
                final int idx = i;
                TextView t = new TextView(c);
                t.setText(I18n.FLAGS[i]);
                t.setTextSize(21f);
                t.setGravity(Gravity.CENTER);
                t.setOnClickListener(v -> {
                    lang = I18n.CODES[idx];
                    Prefs.setLang(getContext(), lang);
                    refresh();
                    t.animate().scaleX(1.18f).scaleY(1.18f).setDuration(110)
                            .withEndAction(() -> t.animate().scaleX(1f).scaleY(1f).setDuration(160).start()).start();
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(UiUtil.dp(c, 42), UiUtil.dp(c, 42));
                lp.setMargins(UiUtil.dp(c, 3), 0, UiUtil.dp(c, 3), 0);
                row.addView(t, lp);
                chips[i] = t;
            }
            LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-2, -2);
            rl.bottomMargin = UiUtil.dp(c, 16);
            card.addView(row, rl);
        }

        title = text(c, 19f, 0xFFFFFFFF, true);
        card.addView(title, new LinearLayout.LayoutParams(-1, -2));
        msg = text(c, 14f, 0xCCFFFFFF, false);
        LinearLayout.LayoutParams ml = new LinearLayout.LayoutParams(-1, -2);
        ml.topMargin = UiUtil.dp(c, 8);
        card.addView(msg, ml);

        retry = new GlassButton(c, tier, true);
        retry.setOnClickListener(v -> { if (!busy && this.onRetry != null) this.onRetry.run(); });
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-1, UiUtil.dp(c, 52));
        bl.topMargin = UiUtil.dp(c, 20);
        card.addView(retry, bl);
        refresh();
    }

    private static TextView text(Context c, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return t;
    }

    private void refresh() {
        card.setLayoutDirection(I18n.rtl(lang) ? LAYOUT_DIRECTION_RTL : LAYOUT_DIRECTION_LTR);
        title.setText(I18n.t(lang, titleKey));
        msg.setText(I18n.t(lang, msgKey));
        retry.setText(busy ? "\u2026" : I18n.t(lang, "retry"));
        for (int i = 0; i < chips.length; i++) {
            if (chips[i] == null) continue;
            boolean sel = I18n.CODES[i].equals(lang);
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(sel ? 0x33FF2A3D : 0x14FFFFFF);
            d.setStroke(UiUtil.dp(getContext(), sel ? 2 : 1), sel ? 0xFFFF2A3D : 0x33FFFFFF);
            chips[i].setBackground(d);
            chips[i].setAlpha(sel ? 1f : 0.7f);
        }
    }

    void attachTo(ViewGroup parent) {
        parent.addView(this, new LayoutParams(-1, -1));
        scrim.animate().alpha(0.72f).setDuration(220).start();
        card.setAlpha(0f);
        card.setScaleX(0.8f);
        card.setScaleY(0.8f);
        card.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(420)
                .setInterpolator(new OvershootInterpolator(1.3f)).start();
        UiUtil.va(0f, 1f, 600, null, card::setGlow);
    }

    void setBusy(boolean b) {
        busy = b;
        retry.setEnabled(!b);
        refresh();
    }

    void fail() {
        setBusy(false);
        UiUtil.shake(card, UiUtil.dpf(getContext(), 10));
        UiUtil.va(1f, 0f, 600, null, card::setFlash);
    }

    void dismiss(Runnable end) {
        card.animate().alpha(0f).scaleX(0.92f).scaleY(0.92f).setDuration(220).start();
        scrim.animate().alpha(0f).setDuration(260).withEndAction(() -> {
            ViewGroup p = (ViewGroup) getParent();
            if (p != null) p.removeView(this);
            if (end != null) end.run();
        }).start();
    }
}
