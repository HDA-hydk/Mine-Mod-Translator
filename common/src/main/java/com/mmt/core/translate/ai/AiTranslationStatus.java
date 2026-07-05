package com.mmt.core.translate.ai;

public enum AiTranslationStatus {
    TRANSLATED,
    UNTRANSLATED;

    public static AiTranslationStatus fromString(String value) {
        if (value == null) {
            return UNTRANSLATED;
        }
        switch (value.trim().toUpperCase()) {
            case "TRANSLATED":
                return TRANSLATED;
            case "UNTRANSLATED":
            default:
                return UNTRANSLATED;
        }
    }
}