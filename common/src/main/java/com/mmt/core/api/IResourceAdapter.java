package com.mmt.core.api;

import com.mmt.core.extract.model.ModLangFile;

import java.util.List;
import java.util.Map;

/**
 * 资源适配接口
 * 各加载器需要实现此接口，提供语言文件枚举和读取功能
 */
public interface IResourceAdapter {
    /**
     * 获取所有已加载模组的语言文件
     * @return modId → 语言文件列表的映射
     */
    Map<String, List<ModLangFile>> getAllModLangFiles();

    /**
     * 获取指定模组的语言文件
     * @param modId 模组 ID
     * @return 语言文件列表
     */
    List<ModLangFile> getModLangFiles(String modId);

    /**
     * 获取指定模组、指定语言的语言文件
     * @param modId 模组 ID
     * @param language 语言代码
     * @return 语言文件，不存在返回 null
     */
    ModLangFile getModLangFile(String modId, String language);

    /**
     * 判断指定语言文件是否存在
     * @param modId 模组 ID
     * @param language 语言代码
     * @return 是否存在
     */
    boolean hasLangFile(String modId, String language);

    /**
     * 获取所有已加载模组的 ID
     * @return 模组 ID 列表
     */
    List<String> getLoadedModIds();

    /**
     * 获取模组的命名空间列表
     * @param modId 模组 ID
     * @return 命名空间列表
     */
    List<String> getModNamespaces(String modId);
}