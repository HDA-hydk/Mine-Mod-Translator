package com.mmt.core.config;

import com.mmt.core.api.IPlatformAdapter;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 配置管理器
 * 负责管理配置的加载、保存和参数优先级
 * 参数优先级：命令行参数 > config.txt > 内置默认值
 */
public class ConfigManager {
    private static ConfigManager INSTANCE;

    private MmtConfig config;
    private final IPlatformAdapter platformAdapter;
    private final Map<String, String> commandOverrides = new HashMap<>();
    private Path configPath;

    private ConfigManager(IPlatformAdapter adapter) {
        this.platformAdapter = adapter;
    }

    /**
     * 获取单例实例
     */
    public static ConfigManager getInstance(IPlatformAdapter adapter) {
        if (INSTANCE == null) {
            INSTANCE = new ConfigManager(adapter);
        }
        return INSTANCE;
    }

    /**
     * 获取单例实例（已初始化）
     */
    public static ConfigManager getInstance() {
        return INSTANCE;
    }

    /**
     * 初始化配置管理器
     */
    public void init() {
        this.configPath = getConfigPath();
        this.config = ConfigLoader.load(configPath);

        // 如果配置文件不存在，创建默认配置
        if (!java.nio.file.Files.exists(configPath)) {
            try {
                ConfigLoader.save(config, configPath);
            } catch (Exception e) {
                // 创建失败不影响游戏运行
            }
        }
    }

    /**
     * 获取配置文件路径
     * @return 配置文件路径
     */
    public Path getConfigPath() {
        if (configPath != null) {
            return configPath;
        }
        return platformAdapter.getGameVersionDir()
                .resolve("mmt")
                .resolve("config")
                .resolve("config.txt");
    }

    /**
     * 获取配置数据
     * @return 配置数据类
     */
    public MmtConfig getConfig() {
        return config;
    }

    /**
     * 设置命令行参数覆盖
     * @param key 参数键名
     * @param value 参数值
     */
    public void setCommandOverride(String key, String value) {
        commandOverrides.put(key, value);
    }

    /**
     * 清除命令行参数覆盖
     * @param key 参数键名
     */
    public void clearCommandOverride(String key) {
        commandOverrides.remove(key);
    }

    /**
     * 清除所有命令行参数覆盖
     */
    public void clearAllCommandOverrides() {
        commandOverrides.clear();
    }

    /**
     * 获取参数值（考虑优先级）
     * @param key 参数键名
     * @param defaultValue 默认值
     * @return 参数值
     */
    public String getString(String key, String defaultValue) {
        // 命令行参数优先级最高
        if (commandOverrides.containsKey(key)) {
            return commandOverrides.get(key);
        }

        // 其次是配置文件
        String configValue = getConfigValueAsString(key);
        if (configValue != null && !configValue.isEmpty()) {
            return configValue;
        }

        // 最后是默认值
        return defaultValue;
    }

    /**
     * 获取整数参数值（考虑优先级）
     * @param key 参数键名
     * @param defaultValue 默认值
     * @return 参数值
     */
    public int getInt(String key, int defaultValue) {
        String value = getString(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取布尔参数值（考虑优先级）
     * @param key 参数键名
     * @param defaultValue 默认值
     * @return 参数值
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    /**
     * 获取目标语言（特殊处理：留空时返回当前游戏语言）
     * @param cmdOverride 命令行覆盖值（可为 null）
     * @return 目标语言代码
     */
    public String getTargetLanguage(String cmdOverride) {
        // 命令行覆盖
        if (cmdOverride != null && !cmdOverride.isEmpty()) {
            return cmdOverride;
        }

        // 配置文件
        String configValue = config.getTargetLanguage();
        if (configValue != null && !configValue.isEmpty()) {
            return configValue;
        }

        // 默认值：当前游戏语言
        return platformAdapter.getCurrentLanguage();
    }

    /**
     * 获取提取模式
     * @param cmdOverride 命令行覆盖值（可为 null）
     * @return 提取模式
     */
    public String getExtractMode(String cmdOverride) {
        if (cmdOverride != null && !cmdOverride.isEmpty()) {
            return cmdOverride;
        }
        String mode = config.getExtractMode();
        // 验证模式有效性
        if (mode.equals("diff") || mode.equals("diff_same") || mode.equals("full")) {
            return mode;
        }
        return "diff";
    }

    /**
     * 获取自动提取配置
     * @param cmdOverride 命令行覆盖值（可为 null）
     * @return 是否自动提取
     */
    public boolean getAutoExtract(Boolean cmdOverride) {
        if (cmdOverride != null) {
            return cmdOverride;
        }
        return config.isAutoExtract();
    }

    /**
     * 获取自动翻译配置
     * @param cmdOverride 命令行覆盖值（可为 null）
     * @return 是否自动翻译
     */
    public boolean getAutoTranslate(Boolean cmdOverride) {
        if (cmdOverride != null) {
            return cmdOverride;
        }
        return config.isAutoTranslate();
    }

    /**
     * 获取自动打包配置
     * @param cmdOverride 命令行覆盖值（可为 null）
     * @return 是否自动打包
     */
    public boolean getAutoPack(Boolean cmdOverride) {
        if (cmdOverride != null) {
            return cmdOverride;
        }
        return config.isAutoPack();
    }

    /**
     * 获取调试模式
     * @return 调试级别（0=INFO/WARN，1=DEBUG）
     */
    public int getDebug() {
        String override = commandOverrides.get("debug");
        if (override != null) {
            try {
                return Integer.parseInt(override);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return config.getDebug();
    }

    /**
     * 保存配置到文件
     * @throws Exception 保存失败时抛出
     */
    public void save() throws Exception {
        ConfigLoader.save(config, configPath);
    }

    /**
     * 重新加载配置
     */
    public void reload() {
        this.config = ConfigLoader.load(configPath);
    }

    /**
     * 设置字符串配置值
     * @param key 参数键名
     * @param value 参数值
     */
    public void setString(String key, String value) {
        setConfigValue(key, value);
    }

    /**
     * 更新配置值
     * @param key 参数键名
     * @param value 参数值
     */
    public void setConfigValue(String key, String value) {
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
                config.setAutoExtract(Boolean.parseBoolean(value));
                break;
            case "auto_translate":
                config.setAutoTranslate(Boolean.parseBoolean(value));
                break;
            case "auto_pack":
                config.setAutoPack(Boolean.parseBoolean(value));
                break;
            case "show_join_summary":
                config.setShowJoinSummary(Boolean.parseBoolean(value));
                break;
            case "translate_methods":
                config.setTranslateMethods(value);
                break;
            case "ai_file_max_size":
                config.setAiFileMaxSize(Integer.parseInt(value));
                break;
            case "ai_split_oversized_mod":
                config.setAiSplitOversizedMod(Boolean.parseBoolean(value));
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
                config.setRequestCharLimit(Integer.parseInt(value));
                break;
            case "request_interval_ms":
                config.setRequestIntervalMs(Integer.parseInt(value));
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
                config.setDebug(Integer.parseInt(value));
                break;
            default:
                break;
        }
    }

    /**
     * 将配置值转换为字符串
     */
    private String getConfigValueAsString(String key) {
        switch (key) {
            case "target_language": return config.getTargetLanguage();
            case "extract_mode": return config.getExtractMode();
            case "extract_broad_scope": return config.getExtractBroadScope();
            case "extract_list_mode": return config.getExtractListMode();
            case "extract_list": return config.getExtractList();
            case "auto_extract": return String.valueOf(config.isAutoExtract());
            case "auto_translate": return String.valueOf(config.isAutoTranslate());
            case "auto_pack": return String.valueOf(config.isAutoPack());
            case "show_join_summary": return String.valueOf(config.isShowJoinSummary());
            case "translate_methods": return config.getTranslateMethods();
            case "ai_file_max_size": return String.valueOf(config.getAiFileMaxSize());
            case "ai_split_oversized_mod": return String.valueOf(config.isAiSplitOversizedMod());
            case "ai_api_url": return config.getAiApiUrl();
            case "ai_api_key": return config.getAiApiKey();
            case "ai_api_model": return config.getAiApiModel();
            case "request_char_limit": return String.valueOf(config.getRequestCharLimit());
            case "request_interval_ms": return String.valueOf(config.getRequestIntervalMs());
            case "mt_api_url": return config.getMtApiUrl();
            case "mt_api_key": return config.getMtApiKey();
            case "mt_api_model": return config.getMtApiModel();
            case "dict_version": return config.getDictVersion();
            case "dict_download_url": return config.getDictDownloadUrl();
            case "pack_output_dir": return config.getPackOutputDir();
            case "debug": return String.valueOf(config.getDebug());
            default: return null;
        }
    }
}