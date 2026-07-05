package com.mmt.core.extract;

/**
 * 提取模式枚举
 */
public enum ExtractMode {
    DIFF("diff"),
    DIFF_SAME("diff_same"),
    FULL("full");

    private final String name;

    ExtractMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ExtractMode fromString(String name) {
        if (name == null || name.isEmpty()) {
            return DIFF;
        }
        try {
            return ExtractMode.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 尝试处理带下划线的情况
            String normalized = name.toUpperCase().replace("-", "_");
            try {
                return ExtractMode.valueOf(normalized);
            } catch (IllegalArgumentException ex) {
                return DIFF;
            }
        }
    }
}