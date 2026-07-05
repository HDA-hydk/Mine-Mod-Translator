package com.mmt.core.i18n;

import com.mmt.core.log.MmtLogger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class I18nManager {
    private static I18nManager instance;

    private final Map<String, Properties> languageMap = new HashMap<>();
    private String currentLanguage = "en_us";
    private final MmtLogger logger;

    private I18nManager(MmtLogger logger) {
        this.logger = logger;
        loadBuiltInLanguages();
    }

    public static synchronized I18nManager getInstance(MmtLogger logger) {
        if (instance == null) {
            instance = new I18nManager(logger);
        }
        return instance;
    }

    public void setLanguage(String language) {
        if (language == null || language.isEmpty()) {
            return;
        }
        String normalized = normalizeLang(language);
        if (languageMap.containsKey(normalized)) {
            this.currentLanguage = normalized;
        } else if (languageMap.containsKey("en_us")) {
            this.currentLanguage = "en_us";
        }
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public java.util.Set<String> getLoadedLanguages() {
        return languageMap.keySet();
    }

    public String t(String key) {
        return translate(key, (Object[]) null);
    }

    public String t(String key, Object... args) {
        return translate(key, args);
    }

    public String translate(String key, Object... args) {
        Properties props = languageMap.get(currentLanguage);
        if (props == null) {
            props = languageMap.get("en_us");
        }
        if (props == null) {
            return key;
        }

        String value = props.getProperty(key);
        if (value == null) {
            Properties enProps = languageMap.get("en_us");
            if (enProps != null) {
                value = enProps.getProperty(key);
            }
            if (value == null) {
                return key;
            }
        }

        if (args != null && args.length > 0) {
            try {
                return String.format(value, args);
            } catch (Exception e) {
                return value;
            }
        }

        return value;
    }

    private void loadBuiltInLanguages() {
        loadLanguage("en_us");
        loadLanguage("zh_cn");
        loadLanguage("zh_tw");
    }

    private void loadLanguage(String langCode) {
        try {
            String resourcePath = "/assets/mmt/lang/" + langCode + ".properties";
            InputStream is = I18nManager.class.getResourceAsStream(resourcePath);
            if (is == null) {
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream("assets/mmt/lang/" + langCode + ".properties");
            }
            if (is == null) {
                logger.debug("Language file not found: " + resourcePath);
                return;
            }

            Properties props = new Properties();
            props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
            languageMap.put(langCode, props);
            is.close();

            logger.debug("Loaded language: " + langCode + " (" + props.size() + " keys)");
        } catch (Exception e) {
            logger.warn("Failed to load language: " + langCode + " - " + e.getMessage());
        }
    }

    private String normalizeLang(String lang) {
        if (lang == null) {
            return "en_us";
        }
        return lang.toLowerCase().replace('-', '_');
    }
}
