package com.y9pro.v5;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Page 2 password dialog. Intro: dim -> big glass shield pops -> bursts into a red flash -> glass card springs in
 * with a small shield on top. Wrong password: shake + red warning glow. Right password: onSuccess().
 * The password check (PBKDF2) runs off the UI thread.
 */
final class PasswordOverlay extends FrameLayout {
    interface Listener { void onSuccess(); }

    private final Listener listener;
    private final ExecutorService ex = Executors.newSingleThreadExecutor();
    private final String lang;
    private final View scrim, wash;
    private final GlassCardView card;
    private final ShieldView big;
    private final EditText input;
    private final EyeView eye;
    private final TextView err;
    private final GlassButton confirm, getPass, howTo;
    private AppConfig cfg;
    private boolean busy;
    private long lastLink;

    PasswordOverlay(Context c, int tier, AppConfig cfg, Listener l) {
        super(c);
        this.cfg = cfg;
        this.listener = l;
        this.lang = Prefs.lang(c);
        setClickable(true);
        setLayoutDirection(I18n.rtl(lang) ? LAYOUT_DIRECTION_RTL : LAYOUT_DIRECTION_LTR);

        scrim = new View(c);
        scrim.setBackgroundColor(Color.BLACK);
        scrim.setAlpha(0f);
        addView(scrim, new LayoutParams(-1, -1));

        wash = new View(c);
        wash.setBackgroundColor(0xFFB3001B);
        wash.setAlpha(0f);
        addView(wash, new LayoutParams(-1, -1));

        int bigSize = UiUtil.dp(c, 150);
        big = new ShieldView(c);
        big.setAlpha(0f);
        addView(big, new LayoutParams(bigSize, bigSize, Gravity.CENTER));

        card = new GlassCardView(c, tier);
        card.setVisibility(INVISIBLE);
        int w = Math.min((int) (c.getResources().getDisplayMetrics().widthPixels * 0.88f), UiUtil.dp(c, 380));
        addView(card, new LayoutParams(w, -2, Gravity.CENTER));

        ShieldView small = new ShieldView(c);
        card.addView(small, new LinearLayout.LayoutParams(UiUtil.dp(c, 58), UiUtil.dp(c, 58)));

        TextView title = new TextView(c);
        title.setText(I18n.t(lang, "pw_title"));
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(19f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-1, -2);
        tl.topMargin = UiUtil.dp(c, 8);
        card.addView(title, tl);

        FrameLayout field = new FrameLayout(c);
        input = new EditText(c);
        input.setHint(I18n.t(lang, "pw_hint"));
        input.setHintTextColor(0x88FFFFFF);
        input.setTextColor(0xFFFFFFFF);
        input.setTextSize(16f);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setTransformationMethod(PasswordTransformationMethod.getInstance());
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x14FFFFFF);
        bg.setCornerRadius(UiUtil.dpf(c, 14));
        bg.setStroke(UiUtil.dp(c, 1), 0x66FF2A3D);
        input.setBackground(bg);
        input.setPadding(UiUtil.dp(c, 16), 0, UiUtil.dp(c, 48), 0);
        input.setOnEditorActionListener((v, a, e) -> { submit(); return true; });
        field.addView(input, new LayoutParams(-1, -1));

        eye = new EyeView(c);
        eye.setOnClickListener(v -> {
            eye.toggle();
            input.setTransformationMethod(eye.isRevealed()
                    ? HideReturnsTransformationMethod.getInstance() : PasswordTransformationMethod.getInstance());
            input.setSelection(input.length());
        });
        LayoutParams el = new LayoutParams(UiUtil.dp(c, 24), UiUtil.dp(c, 24), Gravity.END | Gravity.CENTER_VERTICAL);
        el.setMarginEnd(UiUtil.dp(c, 14));
        field.addView(eye, el);
        LinearLayout.LayoutParams fl = new LinearLayout.LayoutParams(-1, UiUtil.dp(c, 52));
        fl.topMargin = UiUtil.dp(c, 16);
        card.addView(field, fl);

        err = new TextView(c);
        err.setText(I18n.t(lang, "wrong"));
        err.setTextColor(0xFFFF6B78);
        err.setTextSize(13f);
        err.setGravity(Gravity.CENTER);
        err.setVisibility(INVISIBLE);
        LinearLayout.LayoutParams ell = new LinearLayout.LayoutParams(-1, -2);
        ell.topMargin = UiUtil.dp(c, 8);
        card.addView(err, ell);

        confirm = new GlassButton(c, tier, true);
        confirm.setText(I18n.t(lang, "confirm"));
        confirm.setOnClickListener(v -> submit());
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(-1, UiUtil.dp(c, 50));
        cl.topMargin = UiUtil.dp(c, 8);
        card.addView(confirm, cl);

        View div = new View(c);
        div.setBackgroundColor(0x1FFFFFFF);
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(-1, UiUtil.dp(c, 1));
        dl.topMargin = UiUtil.dp(c, 18);
        dl.bottomMargin = UiUtil.dp(c, 14);
        card.addView(div, dl);

        LinearLayout links = new LinearLayout(c);
        getPass = new GlassButton(c, tier, false);
        howTo = new GlassButton(c, tier, false);
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, UiUtil.dp(c, 44), 1f);
        l1.setMarginEnd(UiUtil.dp(c, 4));
        LinearLayout.LayoutParams l2 = new LinearLayout.LayoutParams(0, UiUtil.dp(c, 44), 1f);
        l2.setMarginStart(UiUtil.dp(c, 4));
        links.addView(getPass, l1);
        links.addView(howTo, l2);
        card.addView(links, new LinearLayout.LayoutParams(-1, -2));
        getPass.setOnClickListener(v -> openLink(this.cfg.getPass.url));
        howTo.setOnClickListener(v -> openLink(this.cfg.howTo.url));
        applyLabels();
    }

    private void openLink(String url) {
        long n = android.os.SystemClock.elapsedRealtime();
        if (n - lastLink < 800) return; // ignore rapid repeated taps
        lastLink = n;
        UiUtil.openExternal(getContext(), url);
    }

    private void applyLabels() {
        getPass.setText(cfg.getPass.label);
        howTo.setText(cfg.howTo.label);
    }

    /** A fresher config arrived while the dialog is open (new password / new links). */
    void setConfig(AppConfig c) {
        cfg = c;
        applyLabels();
    }

    void show() {
        scrim.animate().alpha(0.62f).setDuration(220).start();
        big.setScaleX(0.1f);
        big.setScaleY(0.1f);
        big.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(650)
                .setInterpolator(new OvershootInterpolator(1.5f)).start();
        UiUtil.va(0f, 1f, 900, null, big::setShine);
        postDelayed(this::burst, 950);
    }

    private void burst() {
        big.animate().scaleX(1.7f).scaleY(1.7f).alpha(0f).setDuration(320)
                .setInterpolator(new AccelerateInterpolator()).withEndAction(() -> big.setVisibility(GONE)).start();
        UiUtil.va(0f, 1f, 380, null, p -> wash.setAlpha(0.35f * (float) Math.sin(Math.PI * p)));
        card.setVisibility(VISIBLE);
        card.setAlpha(0f);
        card.setScaleX(0.82f);
        card.setScaleY(0.82f);
        card.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(460)
                .setInterpolator(new OvershootInterpolator(1.3f)).start();
        UiUtil.va(0f, 1f, 700, null, card::setGlow);
    }

    private void submit() {
        if (busy) return;
        int n = input.length();
        if (n == 0) { fail(); return; }
        final char[] pw = new char[n];
        input.getText().getChars(0, n, pw, 0);
        busy = true;
        confirm.setEnabled(false);
        confirm.setText("\u2026");
        final AppConfig c = cfg;
        ex.execute(() -> {
            boolean ok = false;
            try { ok = c.checkPassword(pw); } catch (Exception ignored) { }
            Arrays.fill(pw, '\0');
            final boolean r = ok;
            post(() -> {
                busy = false;
                confirm.setEnabled(true);
                confirm.setText(I18n.t(lang, "confirm"));
                if (r) listener.onSuccess(); else fail();
            });
        });
    }

    private void fail() {
        err.setVisibility(VISIBLE);
        UiUtil.shake(card, UiUtil.dpf(getContext(), 10));
        UiUtil.va(1f, 0f, 650, null, card::setFlash);
    }

    void dismiss(Runnable end) {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        card.animate().alpha(0f).scaleX(0.92f).scaleY(0.92f).setDuration(220).start();
        scrim.animate().alpha(0f).setDuration(260).withEndAction(() -> {
            ex.shutdown();
            ViewGroup p = (ViewGroup) getParent();
            if (p != null) p.removeView(this);
            if (end != null) end.run();
        }).start();
    }
}
