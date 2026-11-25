package com.y9pro.v5;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import org.json.JSONException;
import org.json.JSONObject;

/** The decrypted + signature-verified remote config. Data only: nothing in it is ever executed. */
final class AppConfig {
    static final class Link {
        final String label, url;
        Link(String label, String url) { this.label = label; this.url = url; }
    }

    static final class Btn {
        final String label;
        final boolean enabled;
        Btn(String label, boolean enabled) { this.label = label; this.enabled = enabled; }
    }

    long latestVersionCode;
    String updTitle = "", updDesc = "", updButton = "", updUrl = "";
    String passSalt = "", passHash = "";
    int passIter = 120000;
    Link getPass, howTo;
    Btn start, explanations, info;

    static AppConfig parse(String json) throws JSONException {
        JSONObject o = new JSONObject(json);
        AppConfig c = new AppConfig();

        JSONObject app = o.optJSONObject("app");
        if (app != null) {
            c.latestVersionCode = app.optLong("latest_version_code", 0);
            JSONObject u = app.optJSONObject("update");
            if (u != null) {
                c.updTitle = u.optString("title", "");
                c.updDesc = u.optString("description", "");
                c.updButton = u.optString("button", "");
                c.updUrl = u.optString("url", "");
            }
        }

        JSONObject pw = o.optJSONObject("password");
        if (pw != null) {
            c.passSalt = pw.optString("salt", "");
            c.passHash = pw.optString("hash", "");
            c.passIter = pw.optInt("iter", 120000);
        }

        JSONObject pl = o.optJSONObject("pass_links");
        c.getPass = link(pl == null ? null : pl.optJSONObject("get_pass"), "Get Pass");
        c.howTo = link(pl == null ? null : pl.optJSONObject("how_to"), "How To Get Pass");

        JSONObject hb = o.optJSONObject("home_buttons");
        c.start = btn(hb == null ? null : hb.optJSONObject("start"), "START");
        c.explanations = btn(hb == null ? null : hb.optJSONObject("explanations"), "Explanations");
        c.info = btn(hb == null ? null : hb.optJSONObject("info"), "YOUSSEF 999 INFO");
        return c;
    }

    private static Link link(JSONObject o, String def) {
        if (o == null) return new Link(def, "");
        return new Link(o.optString("label", def), o.optString("url", ""));
    }

    private static Btn btn(JSONObject o, String def) {
        if (o == null) return new Btn(def, true);
        return new Btn(o.optString("label", def), o.optBoolean("enabled", true));
    }

    boolean passwordConfigured() {
        return !passHash.isEmpty() && !passSalt.isEmpty();
    }

    /** Slow on purpose (PBKDF2). Call from a background thread. */
    boolean checkPassword(char[] pw) throws GeneralSecurityException {
        byte[] h = Crypto.pbkdf2(pw, Crypto.b64(passSalt), passIter);
        return MessageDigest.isEqual(h, Crypto.b64(passHash));
    }
}
