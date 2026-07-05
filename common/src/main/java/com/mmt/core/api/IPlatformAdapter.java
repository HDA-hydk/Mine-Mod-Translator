package com.mmt.core.api;

import java.nio.file.Path;
import java.util.List;

/**
 * 平台适配器主接口
 * 各加载器需要实现此接口，提供平台相关的功能
 */
public interface IPlatformAdapter extends IResourceAdapter {
    /**
     * 获取平台名称
     * @return 平台名称（forge / fabric / neoforge）
     */
    String getPlatformName();

    /**
     * 获取游戏版本目录
     * @return 游戏版本目录路径（如 .minecraft/versions/1.20.1/）
     */
    Path getGameVersionDir();

    /**
     * 获取游戏运行目录
     * @return 游戏运行目录路径（如 .minecraft/）
     */
    Path getGameRunDir();

    /**
     * 获取当前游戏语言代码
     * @return 当前游戏语言代码（如 zh_cn）
     */
    String getCurrentLanguage();

    /**
     * 获取当前 Minecraft 版本
     * @return Minecraft 版本字符串（如 1.20.1）
     */
    String getMinecraftVersion();

    /**
     * 获取所有已加载模组的 ID 列表
     * @return 模组 ID 列表
     */
    List<String> getLoadedModIds();

    /**
     * 向玩家发送聊天消息（线程安全，可在后台线程调用）
     * @param message 纯文本消息内容
     */
    void sendChatMessage(String message);

    /**
     * 向玩家发送聊天消息，其中包含一个可点击的文件路径片段
     * @param prefix 路径前的文本
     * @param filePath 可点击的文件/文件夹路径（点击后在系统中打开）
     * @param suffix 路径后的文本
     */
    void sendChatMessageWithFilePath(String prefix, String filePath, String suffix);
}