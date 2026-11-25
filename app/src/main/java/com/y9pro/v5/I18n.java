package com.y9pro.v5;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** UI texts in the 6 supported languages. Order everywhere: ar, en, es, vi, hi, pt (Brazilian). */
final class I18n {
    static final String[] CODES = {"ar", "en", "es", "vi", "hi", "pt"};
    static final String[] FLAGS = {
            "\uD83C\uDDEA\uD83C\uDDEC", // Egypt
            "\uD83C\uDDFA\uD83C\uDDF8", // USA
            "\uD83C\uDDEA\uD83C\uDDF8", // Spain
            "\uD83C\uDDFB\uD83C\uDDF3", // Vietnam
            "\uD83C\uDDEE\uD83C\uDDF3", // India
            "\uD83C\uDDE7\uD83C\uDDF7"  // Brazil
    };

    private static final Map<String, String[]> M = new HashMap<>();

    private static void put(String k, String ar, String en, String es, String vi, String hi, String pt) {
        M.put(k, new String[]{ar, en, es, vi, hi, pt});
    }

    static {
        put("net_title", "لا يوجد اتصال بالإنترنت", "No Internet Connection", "Sin conexión a Internet",
                "Không có kết nối Internet", "इंटरनेट कनेक्शन नहीं है", "Sem conexão com a Internet");
        put("net_msg", "تحقق من اتصالك بالإنترنت ثم حاول مرة أخرى.", "Please check your connection and try again.",
                "Comprueba tu conexión e inténtalo de nuevo.", "Vui lòng kiểm tra kết nối của bạn và thử lại.",
                "कृपया अपना कनेक्शन जाँचें और पुनः प्रयास करें।", "Verifique sua conexão e tente novamente.");
        put("cfg_title", "تعذّر تحميل الإعدادات", "Couldn't Load Settings", "No se pudo cargar la configuración",
                "Không thể tải cấu hình", "सेटिंग्स लोड नहीं हो सकीं", "Não foi possível carregar as configurações");
        put("cfg_msg", "تأكد من الإنترنت ثم أعد المحاولة.", "Check your internet and try again.",
                "Comprueba tu Internet e inténtalo de nuevo.", "Kiểm tra Internet và thử lại.",
                "इंटरनेट जाँचें और पुनः प्रयास करें।", "Verifique sua Internet e tente novamente.");
        put("retry", "إعادة المحاولة", "Retry", "Reintentar", "Thử lại", "पुनः प्रयास करें", "Tentar novamente");
        put("pw_title", "أدخل كلمة المرور", "Enter Password", "Introduce la contraseña",
                "Nhập mật khẩu", "पासवर्ड दर्ज करें", "Digite a senha");
        put("pw_hint", "كلمة المرور", "Password", "Contraseña", "Mật khẩu", "पासवर्ड", "Senha");
        put("confirm", "تأكيد", "Confirm", "Confirmar", "Xác nhận", "पुष्टि करें", "Confirmar");
        put("wrong", "كلمة المرور غير صحيحة. حاول مرة أخرى.", "Wrong password. Please try again.",
                "Contraseña incorrecta. Inténtalo de nuevo.", "Mật khẩu không đúng. Vui lòng thử lại.",
                "गलत पासवर्ड। कृपया पुनः प्रयास करें।", "Senha incorreta. Tente novamente.");
    }

    private I18n() {}

    static int index(String lang) {
        for (int i = 0; i < CODES.length; i++) if (CODES[i].equals(lang)) return i;
        return 1;
    }

    static String t(String lang, String key) {
        String[] r = M.get(key);
        return r == null ? key : r[index(lang)];
    }

    static boolean rtl(String lang) {
        return "ar".equals(lang);
    }

    static String deviceLang() {
        String l = Locale.getDefault().getLanguage();
        for (String c : CODES) if (c.equals(l)) return c;
        return "en";
    }
}
