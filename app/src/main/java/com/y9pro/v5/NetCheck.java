package com.y9pro.v5;

import android.os.Handler;
import android.os.Looper;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Real connectivity check: an actual HTTPS request, on a background thread (never on the UI thread). */
final class NetCheck {
    interface Callback { void done(boolean online); }

    private static final ExecutorService EX = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final String[] PROBES = {
            "https://www.gstatic.com/generate_204",
            "https://cp.cloudflare.com/generate_204",
            "https://raw.githubusercontent.com/"
    };

    private NetCheck() {}

    static void run(Callback cb) {
        EX.execute(() -> {
            boolean ok = false;
            for (String u : PROBES) {
                if (probe(u)) { ok = true; break; }
            }
            final boolean r = ok;
            MAIN.post(() -> cb.done(r));
        });
    }

    private static boolean probe(String url) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(3500);
            c.setReadTimeout(3500);
            c.setInstanceFollowRedirects(false);
            c.setUseCaches(false);
            int code = c.getResponseCode();
            return code >= 200 && code < 400 || code == 404; // any HTTP answer proves we are online
        } catch (Exception e) {
            return false;
        } finally {
            if (c != null) c.disconnect();
        }
    }
}
