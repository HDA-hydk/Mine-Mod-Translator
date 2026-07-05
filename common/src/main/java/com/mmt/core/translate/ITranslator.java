package com.mmt.core.translate;

import com.mmt.core.config.ConfigManager;
import com.mmt.core.data.model.translated.TranslatedData;
import com.mmt.core.data.path.PathHelper;
import com.mmt.core.log.MmtLogger;

/**
 * 翻译器接口
 * 所有翻译方式都需要实现此接口
 */
public interface ITranslator {
    /**
     * 获取翻译方法标识
     * @return 翻译方法枚举值
     */
    com.mmt.core.data.model.TranslateMethod getMethod();

    /**
     * 执行翻译
     * @param extractedData 待翻译数据（输入）
     * @param translatedData 已翻译数据（输入/输出）
     * @param targetLanguage 目标语言代码
     * @param configManager 配置管理器
     * @param pathHelper 路径工具
     * @param logger 日志工具
     * @return 翻译命中数量
     */
    int translate(com.mmt.core.data.model.extracted.ExtractedData extractedData,
                  TranslatedData translatedData,
                  String targetLanguage,
                  ConfigManager configManager,
                  PathHelper pathHelper,
                  MmtLogger logger);

    /**
     * 是否支持此翻译方式
     * @param configManager 配置管理器
     * @return 是否支持
     */
    boolean isSupported(ConfigManager configManager);
}