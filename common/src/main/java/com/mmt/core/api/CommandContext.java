package com.mmt.core.api;

import java.util.List;

/**
 * 命令执行上下文
 * 包含命令执行所需的所有信息
 */
public class CommandContext {
    // 执行者名称
    private final String senderName;

    // 是否为 OP（管理员）
    private final boolean isOp;

    // 命令参数列表
    private final List<String> arguments;

    // 目标语言代码（可选）
    private final String targetLanguage;

    // 平台适配器
    private final IPlatformAdapter platformAdapter;

    public CommandContext(String senderName, boolean isOp, List<String> arguments,
                          String targetLanguage, IPlatformAdapter platformAdapter) {
        this.senderName = senderName;
        this.isOp = isOp;
        this.arguments = arguments;
        this.targetLanguage = targetLanguage;
        this.platformAdapter = platformAdapter;
    }

    public String getSenderName() {
        return senderName;
    }

    public boolean isOp() {
        return isOp;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public String getArgument(int index) {
        if (index >= 0 && index < arguments.size()) {
            return arguments.get(index);
        }
        return null;
    }

    public int getArgumentCount() {
        return arguments.size();
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public IPlatformAdapter getPlatformAdapter() {
        return platformAdapter;
    }
}