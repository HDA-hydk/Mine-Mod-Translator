package com.mmt.core.extract.model;

/**
 * 语言文件格式枚举
 */
public enum LangFormat {
    JSON,
    PROPERTIES;

    /**
     * 从文件路径推断格式
     */
    public static LangFormat fromPath(String path) {
        if (path == null || path.isEmpty()) {
            return JSON;
        }
        String lower = path.toLowerCase();
        if (lower.endsWith(".properties") || lower.endsWith(".lang")) {
            return PROPERTIES;
        }
        return JSON;
    }

    /**
     * 获取对应格式的文件名
     */
    public String getFileName(String language) {
        if (this == PROPERTIES) {
            return language + ".lang";
        }
        return language + ".json";
    }
}
