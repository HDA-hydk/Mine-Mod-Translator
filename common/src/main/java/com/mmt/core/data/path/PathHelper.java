package com.mmt.core.data.path;

import com.mmt.core.api.IPlatformAdapter;

import java.nio.file.Path;

/**
 * 路径工具类
 * 提供 MMT 所有数据文件的路径解析
 */
public class PathHelper {
    private final IPlatformAdapter platformAdapter;

    public PathHelper(IPlatformAdapter adapter) {
        this.platformAdapter = adapter;
    }

    /**
     * 获取游戏版本目录路径
     */
    public Path getGameVersionDir() {
        return platformAdapter.getGameVersionDir();
    }

    /**
     * 获取游戏运行目录路径
     */
    public Path getGameRunDir() {
        return platformAdapter.getGameRunDir();
    }

    /**
     * 获取 MMT 根目录
     */
    public Path getMmtDir() {
        return getGameVersionDir().resolve("mmt");
    }

    /**
     * 获取配置目录
     */
    public Path getConfigDir() {
        return getMmtDir().resolve("config");
    }

    /**
     * 获取配置文件路径
     */
    public Path getConfigPath() {
        return getConfigDir().resolve("config.txt");
    }

    /**
     * 获取日志目录
     */
    public Path getLogsDir() {
        return getMmtDir().resolve("logs");
    }

    /**
     * 获取游戏资源包目录（Minecraft 识别的标准位置）
     */
    public Path getResourcePacksDir() {
        return getGameVersionDir().resolve("resourcepacks");
    }

    /**
     * 获取特定目标语言的资源包目录
     */
    public Path getResourcePackDir(String targetLanguage) {
        return getResourcePacksDir().resolve("MMT_" + targetLanguage);
    }

    /**
     * 获取数据文件目录（所有语言共用）
     */
    public Path getDataDir() {
        return getMmtDir().resolve("data");
    }

    /**
     * 获取 extracted.json 路径
     */
    public Path getExtractedPath() {
        return getDataDir().resolve("extracted.json");
    }

    /**
     * 获取 translated.json 路径
     */
    public Path getTranslatedPath() {
        return getDataDir().resolve("translated.json");
    }

    /**
     * 获取 packed.json 路径
     */
    public Path getPackedPath() {
        return getDataDir().resolve("packed.json");
    }

    /**
     * 获取 AI 翻译文件目录
     * AI 翻译文件直接位于 MMT 根目录下
     */
    public Path getAiTranslationDir() {
        return getMmtDir();
    }

    /**
     * 获取特定语言特定编号的 AI 翻译文件路径
     * 文件名格式：AItranslation_[目标语言]_NN.txt
     */
    public Path getAiTranslationFile(String targetLanguage, String fileNumber) {
        return getMmtDir().resolve("AItranslation_" + targetLanguage + "_" + fileNumber + ".txt");
    }

    /**
     * 获取特定语言的 AI 翻译结果汇总文件路径
     * 文件名格式：AItranslationResult_[目标语言].txt
     */
    public Path getAiResultFile(String targetLanguage) {
        return getMmtDir().resolve("AItranslationResult_" + targetLanguage + ".txt");
    }

    /**
     * 获取 AI 文件归档目录
     */
    public Path getArchiveAiDir() {
        return getMmtDir().resolve("archive").resolve("ai");
    }

    /**
     * 获取解析失败片段存档目录
     */
    public Path getFailedPastesDir() {
        return getMmtDir().resolve("failed_pastes");
    }

    /**
     * 获取特定 AI 翻译文件路径（兼容旧接口，无语言参数）
     * @deprecated 使用 getAiTranslationFile(targetLanguage, fileNumber) 代替
     */
    @Deprecated
    public Path getAiTranslationPath(String fileName) {
        return getAiTranslationDir().resolve(fileName);
    }

    /**
     * 获取 i18n-dic 词典目录
     */
    public Path getI18nDicDir() {
        return getMmtDir().resolve("i18n-dic");
    }

    /**
     * 获取 I18n-dict 命中统计路径
     */
    public Path getDictStatsPath() {
        return getDataDir().resolve("dict_stats.json");
    }

    /**
     * 确保目录存在
     */
    public void ensureDirExists(Path dir) {
        if (!java.nio.file.Files.exists(dir)) {
            try {
                java.nio.file.Files.createDirectories(dir);
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
