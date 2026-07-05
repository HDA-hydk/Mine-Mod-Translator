package com.mmt.core.data.model;

/**
 * 翻译方法枚举
 * 包含优先级比较逻辑
 * 优先级从高到低：用户翻译 > I18n-dict > AI(manual/auto同级) > 机翻
 * 数值越小优先级越高
 */
public enum TranslateMethod {
    USER_TRANSLATED(0),
    I18N_DICT(1),
    AI_MANUAL(2),
    AI_AUTO(2),
    MT(3);

    private final int priority;

    TranslateMethod(int priority) {
        this.priority = priority;
    }

    /**
     * 获取优先级值
     * 数值越小优先级越高
     */
    public int getPriority() {
        return priority;
    }

    /**
     * 比较优先级
     * @param other 另一个翻译方法
     * @return 负数：当前优先级更高；正数：other 优先级更高；0：同级
     */
    public int comparePriority(TranslateMethod other) {
        return Integer.compare(this.priority, other.priority);
    }

    /**
     * 判断是否与另一个方法同级
     */
    public boolean isSamePriority(TranslateMethod other) {
        return this.priority == other.priority;
    }

    /**
     * 判断当前方法优先级是否高于另一个方法
     */
    public boolean isHigherPriority(TranslateMethod other) {
        return this.priority < other.priority;
    }

    /**
     * 判断当前方法优先级是否低于另一个方法
     */
    public boolean isLowerPriority(TranslateMethod other) {
        return this.priority > other.priority;
    }

    /**
     * 从字符串解析翻译方法
     * @param name 方法名（大小写不敏感）
     * @return 对应的翻译方法，解析失败返回 null
     */
    public static TranslateMethod fromString(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        try {
            return TranslateMethod.valueOf(name.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 获取显示名称（用于日志和命令输出）
     */
    public String getDisplayName() {
        switch (this) {
            case I18N_DICT: return "I18n-dict";
            case AI_MANUAL: return "AI-manual";
            case AI_AUTO: return "AI-auto";
            case MT: return "MT";
            case USER_TRANSLATED: return "user-translated";
            default: return name().toLowerCase();
        }
    }
}