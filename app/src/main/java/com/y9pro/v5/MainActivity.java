package com.y9pro.v5;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * Page 2: video background -> password dialog (shield intro) -> 3 glass buttons.
 * The password is remembered per password-hash: when the hash in the signed config changes, it is asked again.
 */
public class MainActivity extends Activity {
    private FrameLayout root;
    private CoverVideoView video;
    private LinearLayout column;
    private GlassButton bStart, bExpl, bInfo;
    private PasswordOverlay pwd;
    private NoticeDialog notice;
    private AppConfig cfg;
    private boolean unlocked;
    private long lastClick;
    private int tier;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        UiUtil.immersive(this);
        tier = PerfTier.get(this);

        root = new FrameLayout(this);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF000000, 0xFF1A0208, 0xFF000000}));

        video = new CoverVideoView(this);
        root.addView(video, new FrameLayout.LayoutParams(-1, -1));
        int raw = getResources().getIdentifier("bg", "raw", getPackageName());
        if (raw != 0) video.setSource(raw);

        View dim = new View(this); // keeps the buttons readable without touching the video itself
        dim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x44000000, 0x22000000, 0x99000000}));
        root.addView(dim, new FrameLayout.LayoutParams(-1, -1));

        column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        column.setVisibility(View.INVISIBLE);
        bStart = addButton("START");
        bExpl = addButton("Explanations");
        bInfo = addButton("YOUSSEF 999 INFO");
        FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        cl.bottomMargin = UiUtil.dp(this, 90);
        root.addView(column, cl);

        bStart.setOnClickListener(v -> go("start"));
        bExpl.setOnClickListener(v -> go("explanations"));
        bInfo.setOnClickListener(v -> go("info"));
        setContentView(root);

        cfg = ConfigManager.loadCached(this);
        if (cfg != null) onConfig(cfg);
        fetch();
    }

    private GlassButton addButton(String label) {
        GlassButton g = new GlassButton(this, tier, true);
        g.setText(label);
        int w = Math.min((int) (getResources().getDisplayMetrics().widthPixels * 0.80f), UiUtil.dp(this, 330));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, UiUtil.dp(this, 58));
        lp.topMargin = UiUtil.dp(this, 16);
        column.addView(g, lp);
        return g;
    }

    private void fetch() {
        ConfigManager.refresh(this, fresh -> {
            if (isFinishing() || isDestroyed()) return;
            if (fresh != null) { dismissNotice(); onConfig(fresh); }
            else if (cfg == null) showConfigError();
        });
    }

    private void showConfigError() {
        if (notice != null) { notice.fail(); return; }
        notice = new NoticeDialog(this, tier, "cfg_title", "cfg_msg", false, () -> {
            notice.setBusy(true);
            fetch();
        });
        notice.attachTo(root);
    }

    private void dismissNotice() {
        if (notice == null) return;
        NoticeDialog n = notice;
        notice = null;
        n.dismiss(null);
    }

    private void onConfig(AppConfig c) {
        cfg = c;
        applyLabels();
        if (!c.passwordConfigured()) { showConfigError(); return; } // fail closed: never skip the password
        boolean remembered = c.passHash.equals(Prefs.verified(this));
        if (remembered) {
            if (!unlocked && pwd == null) reveal();
        } else {
            if (unlocked) lock();
            showPassword();
        }
    }

    private void applyLabels() {
        bStart.setText(cfg.start.label);
        bStart.setVisibility(cfg.start.enabled ? View.VISIBLE : View.GONE);
        bExpl.setText(cfg.explanations.label);
        bExpl.setVisibility(cfg.explanations.enabled ? View.VISIBLE : View.GONE);
        bInfo.setText(cfg.info.label);
        bInfo.setVisibility(cfg.info.enabled ? View.VISIBLE : View.GONE);
    }

    private void showPassword() {
        if (pwd != null) { pwd.setConfig(cfg); return; }
        pwd = new PasswordOverlay(this, tier, cfg, () -> {
            Prefs.setVerified(this, cfg.passHash);
            pwd.dismiss(() -> { pwd = null; reveal(); });
        });
        root.addView(pwd, new FrameLayout.LayoutParams(-1, -1));
        pwd.show();
    }

    private void lock() {
        unlocked = false;
        column.setVisibility(View.INVISIBLE);
    }

    /** Buttons appear only after a successful password check: Fade & Scale, small stagger, red glow. */
    private void reveal() {
        unlocked = true;
        column.setVisibility(View.VISIBLE);
        GlassButton[] bs = {bStart, bExpl, bInfo};
        for (int i = 0; i < bs.length; i++) {
            final GlassButton g = bs[i];
            if (g.getVisibility() == View.GONE) continue;
            g.setAlpha(0f);
            g.setScaleX(0.9f);
            g.setScaleY(0.9f);
            g.setTranslationY(UiUtil.dp(this, 18));
            g.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                    .setStartDelay(140L * i).setDuration(420).setInterpolator(new OvershootInterpolator(1.1f)).start();
            UiUtil.va(0f, 1f, 700 + 140L * i, null, g::setGlow);
        }
    }

    /** Pages 3+ come in the next build phases; for now the buttons only prove the lock + click guard work. */
    private void go(String which) {
        if (!unlocked) return;
        long n = SystemClock.elapsedRealtime();
        if (n - lastClick < 800) return; // no double execution on rapid taps
        lastClick = n;
        Toast.makeText(this, which + " \u2192 next build phase", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiUtil.immersive(this);
        video.resume();
    }

    @Override
    protected void onPause() {
        video.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        video.release();
        super.onDestroy();
    }
}
