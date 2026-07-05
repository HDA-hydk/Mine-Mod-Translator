package com.mmt.core.api;

/**
 * 生命周期事件处理器接口
 * 用于处理模组初始化、游戏启动完成等事件
 */
public interface ILifecycleHandler {
    /**
     * 模组初始化时调用
     * 用于设置日志、配置系统等基础组件
     */
    void onModInit();

    /**
     * 游戏启动完成时调用
     * 用于执行自动流水线等
     */
    void onGameStarted();

    /**
     * 资源重载时调用
     * 用于更新资源包状态
     */
    void onResourceReload();
}