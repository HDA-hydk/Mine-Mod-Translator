package com.mmt.core.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 语言代码工具类
 * 提供语言代码归一化、显示名等功能
 */
public final class LangUtil {
    private static final Map<String, String> LANG_DISPLAY_NAMES = new HashMap<>();
    private static final Map<String, String> LANG_ALIASES = new HashMap<>();

    static {
        // 常见语言代码和显示名
        LANG_DISPLAY_NAMES.put("en_us", "English (US)");
        LANG_DISPLAY_NAMES.put("en_gb", "English (UK)");
        LANG_DISPLAY_NAMES.put("zh_cn", "简体中文");
        LANG_DISPLAY_NAMES.put("zh_tw", "繁体中文");
        LANG_DISPLAY_NAMES.put("ja_jp", "日本語");
        LANG_DISPLAY_NAMES.put("ko_kr", "한국어");
        LANG_DISPLAY_NAMES.put("ru_ru", "Русский");
        LANG_DISPLAY_NAMES.put("de_de", "Deutsch");
        LANG_DISPLAY_NAMES.put("fr_fr", "Français");
        LANG_DISPLAY_NAMES.put("es_es", "Español");
        LANG_DISPLAY_NAMES.put("pt_br", "Português (Brasil)");
        LANG_DISPLAY_NAMES.put("it_it", "Italiano");
        LANG_DISPLAY_NAMES.put("pl_pl", "Polski");
        LANG_DISPLAY_NAMES.put("nl_nl", "Nederlands");
        LANG_DISPLAY_NAMES.put("sv_se", "Svenska");
        LANG_DISPLAY_NAMES.put("no_no", "Norsk");
        LANG_DISPLAY_NAMES.put("da_dk", "Dansk");
        LANG_DISPLAY_NAMES.put("fi_fi", "Suomi");
        LANG_DISPLAY_NAMES.put("cs_cz", "Čeština");
        LANG_DISPLAY_NAMES.put("hu_hu", "Magyar");
        LANG_DISPLAY_NAMES.put("ro_ro", "Română");
        LANG_DISPLAY_NAMES.put("bg_bg", "Български");
        LANG_DISPLAY_NAMES.put("sr_sr", "Српски");
        LANG_DISPLAY_NAMES.put("tr_tr", "Türkçe");
        LANG_DISPLAY_NAMES.put("th_th", "ไทย");
        LANG_DISPLAY_NAMES.put("vi_vn", "Tiếng Việt");
        LANG_DISPLAY_NAMES.put("id_id", "Bahasa Indonesia");
        LANG_DISPLAY_NAMES.put("ar_sa", "العربية");
        LANG_DISPLAY_NAMES.put("he_il", "עברית");
        LANG_DISPLAY_NAMES.put("hi_in", "हिन्दी");
        LANG_DISPLAY_NAMES.put("ur_pk", "اردو");
        LANG_DISPLAY_NAMES.put("fa_ir", "فارسی");
        LANG_DISPLAY_NAMES.put("el_gr", "Ελληνικά");
        LANG_DISPLAY_NAMES.put("uk_ua", "Українська");
        LANG_DISPLAY_NAMES.put("be_by", "Беларуская");
        LANG_DISPLAY_NAMES.put("ka_ge", "ქართული");
        LANG_DISPLAY_NAMES.put("hy_am", "Հայերեն");
        LANG_DISPLAY_NAMES.put("et_ee", "Eesti");
        LANG_DISPLAY_NAMES.put("lv_lv", "Latviešu");
        LANG_DISPLAY_NAMES.put("lt_lt", "Lietuvių");
        LANG_DISPLAY_NAMES.put("sk_sk", "Slovenčina");
        LANG_DISPLAY_NAMES.put("sl_si", "Slovenščina");
        LANG_DISPLAY_NAMES.put("hr_hr", "Hrvatski");
        LANG_DISPLAY_NAMES.put("eu_es", "Euskara");
        LANG_DISPLAY_NAMES.put("ca_es", "Català");
        LANG_DISPLAY_NAMES.put("gl_es", "Galego");
        LANG_DISPLAY_NAMES.put("ast_es", "Asturianu");
        LANG_DISPLAY_NAMES.put("oc_oc", "Occitan");
        LANG_DISPLAY_NAMES.put("br_fr", "Brezhoneg");
        LANG_DISPLAY_NAMES.put("cy_gb", "Cymraeg");
        LANG_DISPLAY_NAMES.put("ga_ie", "Gaeilge");
        LANG_DISPLAY_NAMES.put("gd_gb", "Gàidhlig");
        LANG_DISPLAY_NAMES.put("gv_im", "Gaelg");
        LANG_DISPLAY_NAMES.put("mt_mt", "Malti");
        LANG_DISPLAY_NAMES.put("sq_al", "Shqip");
        LANG_DISPLAY_NAMES.put("mk_mk", "Македонски");
        LANG_DISPLAY_NAMES.put("bn_bd", "বাংলা");
        LANG_DISPLAY_NAMES.put("ta_in", "தமிழ்");
        LANG_DISPLAY_NAMES.put("te_in", "తెలుగు");
        LANG_DISPLAY_NAMES.put("kn_in", "ಕನ್ನಡ");
        LANG_DISPLAY_NAMES.put("ml_in", "മലയാളം");
        LANG_DISPLAY_NAMES.put("pa_in", "ਪੰਜਾਬੀ");
        LANG_DISPLAY_NAMES.put("gu_in", "ગુજરાતી");
        LANG_DISPLAY_NAMES.put("mr_in", "मराठी");
        LANG_DISPLAY_NAMES.put("or_in", "ଓଡ଼ିଆ");
        LANG_DISPLAY_NAMES.put("as_in", "অসমীয়া");
        LANG_DISPLAY_NAMES.put("ks_in", "कश्मीरी");
        LANG_DISPLAY_NAMES.put("ne_np", "नेपाली");
        LANG_DISPLAY_NAMES.put("bo_bt", "བོད་ཡིག");
        LANG_DISPLAY_NAMES.put("dz_bt", "རབ་ཅན་སོགས་ཀ");
        LANG_DISPLAY_NAMES.put("mn_mn", "Монгол");
        LANG_DISPLAY_NAMES.put("tt_ru", "Татар");
        LANG_DISPLAY_NAMES.put("kk_kz", "Қазақ");
        LANG_DISPLAY_NAMES.put("uz_uz", "Oʻzbek");
        LANG_DISPLAY_NAMES.put("ky_kg", "Кыргыз");
        LANG_DISPLAY_NAMES.put("tg_tj", "Тоҷикӣ");
        LANG_DISPLAY_NAMES.put("ps_af", "پښتو");
        LANG_DISPLAY_NAMES.put("ku_tr", "Kurdî");
        LANG_DISPLAY_NAMES.put("ug_cn", "Uyghur");
        LANG_DISPLAY_NAMES.put("si_lk", "සිංහල");
        LANG_DISPLAY_NAMES.put("my_mm", "မြန်မာ");
        LANG_DISPLAY_NAMES.put("km_kh", "ខ្មែរ");
        LANG_DISPLAY_NAMES.put("lo_la", "ພາສາລາວ");
        LANG_DISPLAY_NAMES.put("mn_cn", "蒙古文");
        LANG_DISPLAY_NAMES.put("li_nl", "Limburgs");
        LANG_DISPLAY_NAMES.put("brx_in", "बड़ो");

        // 语言代码别名映射（处理常见变体）
        LANG_ALIASES.put("en-us", "en_us");
        LANG_ALIASES.put("en", "en_us");
        LANG_ALIASES.put("zh", "zh_cn");
        LANG_ALIASES.put("zh-cn", "zh_cn");
        LANG_ALIASES.put("zh-cn", "zh_cn");
        LANG_ALIASES.put("zh-tw", "zh_tw");
        LANG_ALIASES.put("ja", "ja_jp");
        LANG_ALIASES.put("ja-jp", "ja_jp");
        LANG_ALIASES.put("ko", "ko_kr");
        LANG_ALIASES.put("ko-kr", "ko_kr");
        LANG_ALIASES.put("ru", "ru_ru");
        LANG_ALIASES.put("ru-ru", "ru_ru");
    }

    private LangUtil() {
        throw new AssertionError("LangUtil 类不应被实例化");
    }

    /**
     * 语言代码常量
     */
    public static final String EN_US = "en_us";
    public static final String ZH_CN = "zh_cn";
    public static final String ZH_TW = "zh_tw";
    public static final String JA_JP = "ja_jp";
    public static final String KO_KR = "ko_kr";
    public static final String RU_RU = "ru_ru";
    public static final String DE_DE = "de_de";
    public static final String FR_FR = "fr_fr";
    public static final String ES_ES = "es_es";

    /**
     * 归一化语言代码
     * 转换为小写 + 下划线格式
     * @param lang 输入语言代码
     * @return 归一化后的语言代码，输入为空返回默认值 en_us
     */
    public static String normalize(String lang) {
        if (lang == null || lang.isEmpty()) {
            return EN_US;
        }

        String normalized = lang.toLowerCase().replace("-", "_");

        if (LANG_ALIASES.containsKey(normalized)) {
            return LANG_ALIASES.get(normalized);
        }

        return normalized;
    }

    /**
     * 获取语言显示名
     * @param lang 语言代码
     * @return 显示名，如果未找到返回原始代码
     */
    public static String displayName(String lang) {
        if (lang == null || lang.isEmpty()) {
            return "Unknown";
        }

        String normalized = normalize(lang);
        String displayName = LANG_DISPLAY_NAMES.get(normalized);
        if (displayName != null) {
            return displayName;
        }

        return normalized;
    }

    /**
     * 判断是否为右到左语言
     * @param lang 语言代码
     * @return 是否为右到左语言
     */
    public static boolean isRightToLeft(String lang) {
        if (lang == null || lang.isEmpty()) {
            return false;
        }

        String normalized = normalize(lang);
        return normalized.startsWith("ar") ||
               normalized.startsWith("he") ||
               normalized.startsWith("ur") ||
               normalized.startsWith("fa") ||
               normalized.startsWith("ps") ||
               normalized.startsWith("ku") ||
               normalized.startsWith("ug");
    }

    /**
     * 判断语言代码是否有效
     * @param lang 语言代码
     * @return 是否有效
     */
    public static boolean isValid(String lang) {
        if (lang == null || lang.isEmpty()) {
            return false;
        }

        String normalized = normalize(lang);
        return LANG_DISPLAY_NAMES.containsKey(normalized);
    }

    /**
     * 获取默认语言代码
     * @return 默认语言代码 en_us
     */
    public static String getDefault() {
        return EN_US;
    }
}