package com.mmt.core.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置文件加载器
 * 负责从 config.txt 读取配置和保存配置
 */
public class ConfigLoader {
    private static final String COMMENT_PREFIX = "#";

    /**
     * 读取配置文件
     * @param configPath 配置文件路径
     * @return 配置数据类
     */
    public static MmtConfig load(Path configPath) {
        MmtConfig config = new MmtConfig();

        if (!Files.exists(configPath)) {
            return config;
        }

        try {
            List<String> lines = Files.readAllLines(configPath, StandardCharsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith(COMMENT_PREFIX)) {
                    continue;
                }

                // 行内注释处理
                int commentIndex = line.indexOf(COMMENT_PREFIX);
                if (commentIndex > 0) {
                    line = line.substring(0, commentIndex).trim();
                }

                // 解析键值对
                int equalsIndex = line.indexOf('=');
                if (equalsIndex <= 0) {
                    continue;
                }

                String key = line.substring(0, equalsIndex).trim();
                String value = line.substring(equalsIndex + 1).trim();

                setConfigValue(config, key, value);
            }
        } catch (IOException e) {
            // 配置文件读取失败，返回默认配置
        }

        return config;
    }

    /**
     * 保存配置到文件
     * @param config 配置数据类
     * @param configPath 配置文件路径
     * @throws IOException 保存失败时抛出
     */
    public static void save(MmtConfig config, Path configPath) throws IOException {
        Path parentDir = configPath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        List<String> lines = new ArrayList<>();
        lines.add("# Mine Mod Translator 配置文件");
        lines.add("# 路径：<版本>/mmt/config/config.txt");
        lines.add("# 参数优先级：命令行 > config.txt > 内置默认值");
        lines.add("");

        // ========== 语言设置 ==========
        lines.add("# ========== 语言设置 ==========");
        lines.add("# 缺省目标语言（留空则使用当前游戏语言）");
        lines.add("target_language=" + config.getTargetLanguage());
        lines.add("");

        // ========== 提取器设置 ==========
        lines.add("# ========== 提取器设置 ==========");
        lines.add("# 提取模式：diff(仅差值键) / diff_same(差值+相同值) / full(全量)");
        lines.add("extract_mode=" + config.getExtractMode());
        lines.add("# 候选模组大范围：new(仅未记录的新模组) / all(所有已加载模组)");
        lines.add("extract_broad_scope=" + config.getExtractBroadScope());
        lines.add("# 名单模式：blacklist(排除) / whitelist(仅包含)");
        lines.add("extract_list_mode=" + config.getExtractListMode());
        lines.add("# 名单列表（逗号分隔，支持 modId 和模组文件名）");
        lines.add("extract_list=" + config.getExtractList());
        lines.add("");

        // ========== 自动流水线 ==========
        lines.add("# ========== 自动流水线 ==========");
        lines.add("# 游戏启动后自动执行提取/翻译/打包");
        lines.add("auto_extract=" + config.isAutoExtract());
        lines.add("auto_translate=" + config.isAutoTranslate());
        lines.add("auto_pack=" + config.isAutoPack());
        lines.add("");

        // ========== 翻译设置 ==========
        lines.add("# ========== 翻译设置 ==========");
        lines.add("# 翻译方式优先级（逗号分隔，按顺序尝试）");
        lines.add("# 可选项：I18n-dict / AI-manual / AI-auto / MT");
        lines.add("translate_methods=" + config.getTranslateMethods());
        lines.add("");

        // ========== AI 翻译文件设置 ==========
        lines.add("# ========== AI 翻译文件设置 ==========");
        lines.add("# 单个 AItranslationNN.txt 最大大小(KB)，-1 为无限大");
        lines.add("ai_file_max_size=" + config.getAiFileMaxSize());
        lines.add("");

        // ========== AI 自动翻译 API ==========
        lines.add("# ========== AI 自动翻译 API ==========");
        lines.add("ai_api_url=" + config.getAiApiUrl());
        lines.add("ai_api_key=" + config.getAiApiKey());
        lines.add("ai_api_model=" + config.getAiApiModel());
        lines.add("# 单次请求字符上限，0=不限");
        lines.add("request_char_limit=" + config.getRequestCharLimit());
        lines.add("# 请求间隔(毫秒)");
        lines.add("request_interval_ms=" + config.getRequestIntervalMs());
        lines.add("");

        // ========== 机翻 API ==========
        lines.add("# ========== 机翻 API ==========");
        lines.add("mt_api_url=" + config.getMtApiUrl());
        lines.add("mt_api_key=" + config.getMtApiKey());
        lines.add("mt_api_model=" + config.getMtApiModel());
        lines.add("");

        // ========== I18n-dict 词典 ==========
        lines.add("# ========== I18n-dict 词典 ==========");
        lines.add("# 词典版本：mini(内置) / full(需下载)");
        lines.add("dict_version=" + config.getDictVersion());
        lines.add("# 全量词典下载地址（可选）");
        lines.add("dict_download_url=" + config.getDictDownloadUrl());
        lines.add("");

        // ========== 打包器设置 ==========
        lines.add("# ========== 打包器设置 ==========");
        lines.add("# 资源包输出目录（留空则输出到 <版本>/mmt/resourcepacks/）");
        lines.add("pack_output_dir=" + config.getPackOutputDir());
        lines.add("");

        // ========== 调试 ==========
        lines.add("# ========== 调试 ==========");
        lines.add("# 0=仅 INFO/WARN 到控制台，1=额外输出 DEBUG 到日志文件");
        lines.add("debug=" + config.getDebug());

        Files.write(configPath, lines, StandardCharsets.UTF_8);
    }

    /**
     * 根据键名设置配置值
     */
    private static void setConfigValue(MmtConfig config, String key, String value) {
        if (value == null) {
            value = "";
        }

        switch (key) {
            case "target_language":
                config.setTargetLanguage(value);
                break;
            case "extract_mode":
                config.setExtractMode(value);
                break;
            case "extract_broad_scope":
                config.setExtractBroadScope(value);
                break;
            case "extract_list_mode":
                config.setExtractListMode(value);
                break;
            case "extract_list":
                config.setExtractList(value);
                break;
            case "auto_extract":
                config.setAutoExtract(parseBoolean(value, true));
                break;
            case "auto_translate":
                config.setAutoTranslate(parseBoolean(value, true));
                break;
            case "auto_pack":
                config.setAutoPack(parseBoolean(value, true));
                break;
            case "translate_methods":
                config.setTranslateMethods(value);
                break;
            case "ai_file_max_size":
                config.setAiFileMaxSize(parseInt(value, -1));
                break;
            case "ai_api_url":
                config.setAiApiUrl(value);
                break;
            case "ai_api_key":
                config.setAiApiKey(value);
                break;
            case "ai_api_model":
                config.setAiApiModel(value);
                break;
            case "request_char_limit":
                config.setRequestCharLimit(parseInt(value, 0));
                break;
            case "request_interval_ms":
                config.setRequestIntervalMs(parseInt(value, 500));
                break;
            case "mt_api_url":
                config.setMtApiUrl(value);
                break;
            case "mt_api_key":
                config.setMtApiKey(value);
                break;
            case "mt_api_model":
                config.setMtApiModel(value);
                break;
            case "dict_version":
                config.setDictVersion(value);
                break;
            case "dict_download_url":
                config.setDictDownloadUrl(value);
                break;
            case "pack_output_dir":
                config.setPackOutputDir(value);
                break;
            case "debug":
                config.setDebug(parseInt(value, 0));
                break;
            default:
                // 未知配置项，忽略
                break;
        }
    }

    /**
     * 解析布尔值
     */
    private static boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * 解析整数值
     */
    private static int parseInt(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}