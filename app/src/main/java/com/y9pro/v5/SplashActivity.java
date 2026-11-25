package com.y9pro.v5;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.FrameLayout;
import java.util.concurrent.atomic.AtomicBoolean;

/** Page 1: cinematic intro + real internet check. Goes to page 2 only when online (and only once). */
public class SplashActivity extends Activity {
    private FrameLayout root;
    private IntroView intro;
    private NoticeDialog dialog;
    private int tier;
    private boolean animDone, checked, online;
    private final AtomicBoolean navigated = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        UiUtil.immersive(this);
        tier = PerfTier.get(this);
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        intro = new IntroView(this, tier);
        root.addView(intro, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        intro.start(() -> { animDone = true; step(); });
        check();
    }

    private void check() {
        NetCheck.run(ok -> {
            if (isFinishing() || isDestroyed()) return;
            checked = true;
            online = ok;
            step();
        });
    }

    private void step() {
        if (!animDone || !checked) return;
        if (online) go(); else showDialog();
    }

    private void go() {
        if (!navigated.compareAndSet(false, true)) return;
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(R.anim.y9_in, R.anim.y9_out);
        finish();
    }

    private void showDialog() {
        if (dialog != null) return;
        dialog = new NoticeDialog(this, tier, "net_title", "net_msg", true, () -> {
            dialog.setBusy(true);
            NetCheck.run(ok -> {
                if (isFinishing() || isDestroyed()) return;
                if (ok) { dialog.setBusy(false); dialog.dismiss(this::go); }
                else dialog.fail();
            });
        });
        dialog.attachTo(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiUtil.immersive(this);
    }
}
