package com.mmt.core;

/**
 * MMT 模组常量类
 * 包含模组基本信息：modid、版本号、名称等
 */
public final class Constants {
    // 模组 ID
    public static final String MOD_ID = "mmt";

    // 模组名称
    public static final String MOD_NAME = "Mine Mod Translator";

    // 模组版本
    public static final String MOD_VERSION = "1.0.0";

    // 命令前缀（不带斜杠）
    public static final String COMMAND_PREFIX = "mmt";

    // 私有构造函数，防止实例化
    private Constants() {
        throw new AssertionError("Constants 类不应被实例化");
    }
}