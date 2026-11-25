package com.y9pro.v5;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("y9", Context.MODE_PRIVATE);
    }

    static String lang(Context c) {
        String l = sp(c).getString("lang", null);
        return l != null ? l : I18n.deviceLang();
    }

    static void setLang(Context c, String l) {
        sp(c).edit().putString("lang", l).apply();
    }

    /** The password hash (from the signed config) that this device has already unlocked. */
    static String verified(Context c) {
        return sp(c).getString("verified", "");
    }

    static void setVerified(Context c, String hash) {
        sp(c).edit().putString("verified", hash).apply();
    }

    static long lastSeq(Context c) {
        return sp(c).getLong("seq", 0L);
    }

    static void setLastSeq(Context c, long seq) {
        sp(c).edit().putLong("seq", seq).apply();
    }
}
