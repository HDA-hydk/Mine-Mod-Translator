package com.mmt.core.api;

/**
 * 命令注册器接口
 * 各加载器需要实现此接口，将公共层定义的命令注册到加载器命令系统
 */
public interface ICommandRegistrar {
    /**
     * 注册命令
     * @param commandName 命令名称（不带前缀）
     * @param handler 命令处理器
     */
    void registerCommand(String commandName, ICommandHandler handler);
}