package com.y9pro.v5;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/**
 * Downloads config.enc.json, checks the ECDSA signature FIRST, then rejects older versions (anti-rollback),
 * then decrypts with AES-GCM. Any failure = the config is ignored and the last good one stays in use.
 *
 * Envelope: {"v":1,"seq":<unix time>,"iv":"b64","ct":"b64 (cipher+tag)","sig":"b64 DER"}
 * Signed bytes: "y9v5." + seq + "." + iv + "." + ct
 */
final class ConfigManager {
    interface Callback { void done(AppConfig cfgOrNull); }

    private static final ExecutorService EX = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final String CACHE = "config.cache.json";
    private static final int MAX_BYTES = 256 * 1024;

    private ConfigManager() {}

    /** Last good config from disk (re-verified), or null. Fast enough for the main thread. */
    static AppConfig loadCached(Context c) {
        try {
            File f = new File(c.getFilesDir(), CACHE);
            if (!f.exists()) return null;
            return open(c, new String(readAll(new FileInputStream(f)), StandardCharsets.UTF_8), false);
        } catch (Exception e) {
            return null;
        }
    }

    static void refresh(Context c, Callback cb) {
        final Context app = c.getApplicationContext();
        EX.execute(() -> {
            AppConfig out = null;
            try {
                String raw = download(Keys.CONFIG_URL + "?t=" + (System.currentTimeMillis() / 60000L));
                out = open(app, raw, true);
                try (FileOutputStream fo = new FileOutputStream(new File(app.getFilesDir(), CACHE))) {
                    fo.write(raw.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception ignored) {
                // keep using the cached config
            }
            final AppConfig r = out;
            MAIN.post(() -> cb.done(r));
        });
    }

    private static AppConfig open(Context c, String envelope, boolean enforceSeq) throws Exception {
        JSONObject e = new JSONObject(envelope);
        if (e.getInt("v") != 1) throw new GeneralSecurityException("unsupported version");
        long seq = e.getLong("seq");
        String iv = e.getString("iv"), ct = e.getString("ct"), sig = e.getString("sig");

        byte[] signed = ("y9v5." + seq + "." + iv + "." + ct).getBytes(StandardCharsets.UTF_8);
        if (!Crypto.verify(Crypto.b64(Keys.SIGN_PUBLIC_KEY_B64), signed, Crypto.b64(sig)))
            throw new GeneralSecurityException("bad signature");
        if (enforceSeq && seq < Prefs.lastSeq(c)) throw new GeneralSecurityException("rollback");

        byte[] plain = Crypto.decrypt(Crypto.b64(Keys.AES_KEY_B64), Crypto.b64(iv), Crypto.b64(ct));
        AppConfig cfg = AppConfig.parse(new String(plain, StandardCharsets.UTF_8));
        if (enforceSeq) Prefs.setLastSeq(c, seq);
        return cfg;
    }

    private static String download(String url) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(url).openConnection();
        try {
            h.setConnectTimeout(10000);
            h.setReadTimeout(10000);
            h.setUseCaches(false);
            h.setRequestProperty("Cache-Control", "no-cache");
            if (h.getResponseCode() != 200) throw new java.io.IOException("HTTP " + h.getResponseCode());
            return new String(readAll(h.getInputStream()), StandardCharsets.UTF_8);
        } finally {
            h.disconnect();
        }
    }

    private static byte[] readAll(InputStream in) throws java.io.IOException {
        try (InputStream is = in) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                bo.write(buf, 0, n);
                if (bo.size() > MAX_BYTES) throw new java.io.IOException("too large");
            }
            return bo.toByteArray();
        }
    }
}
