package com.mmt.core.log;

/**
 * 日志级别枚举
 */
public enum LogLevel {
    INFO(0),
    WARN(1),
    DEBUG(2);

    private final int level;

    LogLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    /**
     * 判断是否应该输出此级别的日志
     * @param debugMode 当前调试模式（0=INFO/WARN，1=DEBUG）
     * @return 是否应该输出
     */
    public boolean shouldLog(int debugMode) {
        if (this == DEBUG) {
            return debugMode >= 1;
        }
        return true;
    }
}