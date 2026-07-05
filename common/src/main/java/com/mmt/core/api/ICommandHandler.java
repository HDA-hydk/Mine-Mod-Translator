package com.mmt.core.api;

import java.util.List;

/**
 * 命令处理器接口
 * 定义命令执行和自动补全的行为
 */
public interface ICommandHandler {
    /**
     * 执行命令
     * @param context 命令执行上下文
     * @return 执行结果消息
     */
    String execute(CommandContext context);

    /**
     * 获取命令参数自动补全建议
     * @param context 命令上下文（包含已输入的参数）
     * @return 补全建议列表
     */
    List<String> getSuggestions(CommandContext context);
}