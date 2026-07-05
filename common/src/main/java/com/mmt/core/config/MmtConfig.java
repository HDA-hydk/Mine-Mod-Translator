package com.mmt.core.config;

/**
 * MMT 配置数据类
 * 存储所有配置项及其值
 */
public class MmtConfig {
    // ========== 语言设置 ==========
    private String targetLanguage = "";

    // ========== 提取器设置 ==========
    private String extractMode = "diff";
    private String extractBroadScope = "new";
    private String extractListMode = "blacklist";
    private String extractList = "";

    // ========== 自动流水线 ==========
    private boolean autoExtract = true;
    private boolean autoTranslate = true;
    private boolean autoPack = true;
    private boolean showJoinSummary = true;

    // ========== 翻译设置 ==========
    private String translateMethods = "I18n-dict,AI-manual";

    // ========== AI 翻译文件设置 ==========
    private int aiFileMaxSize = -1;
    private boolean aiSplitOversizedMod = false;

    // ========== AI 自动翻译 API ==========
    private String aiApiUrl = "";
    private String aiApiKey = "";
    private String aiApiModel = "";
    private int requestCharLimit = 0;
    private int requestIntervalMs = 500;

    // ========== 机翻 API ==========
    private String mtApiUrl = "";
    private String mtApiKey = "";
    private String mtApiModel = "";

    // ========== I18n-dict 词典 ==========
    private String dictVersion = "mini";
    private String dictDownloadUrl = "";

    // ========== 打包器设置 ==========
    private String packOutputDir = "";

    // ========== 调试 ==========
    private int debug = 0;

    // Getters and Setters
    public String getTargetLanguage() {
        return targetLanguage;
    }

    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = targetLanguage;
    }

    public String getExtractMode() {
        return extractMode;
    }

    public void setExtractMode(String extractMode) {
        this.extractMode = extractMode;
    }

    public String getExtractBroadScope() {
        return extractBroadScope;
    }

    public void setExtractBroadScope(String extractBroadScope) {
        this.extractBroadScope = extractBroadScope;
    }

    public String getExtractListMode() {
        return extractListMode;
    }

    public void setExtractListMode(String extractListMode) {
        this.extractListMode = extractListMode;
    }

    public String getExtractList() {
        return extractList;
    }

    public void setExtractList(String extractList) {
        this.extractList = extractList;
    }

    public boolean isAutoExtract() {
        return autoExtract;
    }

    public void setAutoExtract(boolean autoExtract) {
        this.autoExtract = autoExtract;
    }

    public boolean isAutoTranslate() {
        return autoTranslate;
    }

    public void setAutoTranslate(boolean autoTranslate) {
        this.autoTranslate = autoTranslate;
    }

    public boolean isAutoPack() {
        return autoPack;
    }

    public void setAutoPack(boolean autoPack) {
        this.autoPack = autoPack;
    }

    public boolean isShowJoinSummary() {
        return showJoinSummary;
    }

    public void setShowJoinSummary(boolean showJoinSummary) {
        this.showJoinSummary = showJoinSummary;
    }

    public String getTranslateMethods() {
        return translateMethods;
    }

    public void setTranslateMethods(String translateMethods) {
        this.translateMethods = translateMethods;
    }

    public int getAiFileMaxSize() {
        return aiFileMaxSize;
    }

    public void setAiFileMaxSize(int aiFileMaxSize) {
        this.aiFileMaxSize = aiFileMaxSize;
    }

    public boolean isAiSplitOversizedMod() {
        return aiSplitOversizedMod;
    }

    public void setAiSplitOversizedMod(boolean aiSplitOversizedMod) {
        this.aiSplitOversizedMod = aiSplitOversizedMod;
    }

    public String getAiApiUrl() {
        return aiApiUrl;
    }

    public void setAiApiUrl(String aiApiUrl) {
        this.aiApiUrl = aiApiUrl;
    }

    public String getAiApiKey() {
        return aiApiKey;
    }

    public void setAiApiKey(String aiApiKey) {
        this.aiApiKey = aiApiKey;
    }

    public String getAiApiModel() {
        return aiApiModel;
    }

    public void setAiApiModel(String aiApiModel) {
        this.aiApiModel = aiApiModel;
    }

    public int getRequestCharLimit() {
        return requestCharLimit;
    }

    public void setRequestCharLimit(int requestCharLimit) {
        this.requestCharLimit = requestCharLimit;
    }

    public int getRequestIntervalMs() {
        return requestIntervalMs;
    }

    public void setRequestIntervalMs(int requestIntervalMs) {
        this.requestIntervalMs = requestIntervalMs;
    }

    public String getMtApiUrl() {
        return mtApiUrl;
    }

    public void setMtApiUrl(String mtApiUrl) {
        this.mtApiUrl = mtApiUrl;
    }

    public String getMtApiKey() {
        return mtApiKey;
    }

    public void setMtApiKey(String mtApiKey) {
        this.mtApiKey = mtApiKey;
    }

    public String getMtApiModel() {
        return mtApiModel;
    }

    public void setMtApiModel(String mtApiModel) {
        this.mtApiModel = mtApiModel;
    }

    public String getDictVersion() {
        return dictVersion;
    }

    public void setDictVersion(String dictVersion) {
        this.dictVersion = dictVersion;
    }

    public String getDictDownloadUrl() {
        return dictDownloadUrl;
    }

    public void setDictDownloadUrl(String dictDownloadUrl) {
        this.dictDownloadUrl = dictDownloadUrl;
    }

    public String getPackOutputDir() {
        return packOutputDir;
    }

    public void setPackOutputDir(String packOutputDir) {
        this.packOutputDir = packOutputDir;
    }

    public int getDebug() {
        return debug;
    }

    public void setDebug(int debug) {
        this.debug = debug;
    }
}