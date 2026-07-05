package com.mmt.core.extract;

import com.mmt.core.api.IResourceAdapter;
import com.mmt.core.config.ConfigManager;
import com.mmt.core.data.json.JsonUtil;
import com.mmt.core.data.model.extracted.ExtractedData;
import com.mmt.core.data.model.extracted.ExtractedModData;
import com.mmt.core.data.path.PathHelper;
import com.mmt.core.extract.model.LangFormat;
import com.mmt.core.extract.model.ModLangFile;
import com.mmt.core.log.MmtLogger;
import com.mmt.core.translate.dict.I18nDict;
import com.mmt.core.util.LangUtil;
import com.mmt.core.util.HashUtil;
import com.mmt.core.util.LangUtil;
import com.mmt.core.util.ModFilter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 提取器主类
 * 负责扫描模组语言文件、增量提取、生成 extracted.json
 */
public class Extractor {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final IResourceAdapter resourceAdapter;
    private final ConfigManager configManager;
    private final PathHelper pathHelper;
    private final SourceLangDetector langDetector;
    private final MmtLogger logger;
    private final AiFileGenerator aiFileGenerator;

    private Map<String, Object> lastStats = new HashMap<>();

    public Extractor(IResourceAdapter resourceAdapter, ConfigManager configManager,
                     PathHelper pathHelper, MmtLogger logger) {
        this.resourceAdapter = resourceAdapter;
        this.configManager = configManager;
        this.pathHelper = pathHelper;
        this.langDetector = new SourceLangDetector();
        this.logger = logger;
        this.aiFileGenerator = new AiFileGenerator(configManager, pathHelper, logger, I18nDict.getInstance(logger));
    }

    /**
     * 执行提取流程
     * @param targetLanguage 目标语言代码
     * @return 是否提取成功
     */
    public boolean extract(String targetLanguage) {
        String normalizedLang = LangUtil.normalize(targetLanguage);
        logger.info("Starting extraction for target language: " + normalizedLang);

        int processedModCount = 0;
        int totalKeys = 0;

        try {
            ExtractedData existingData = loadExtractedData();
            Map<String, List<ModLangFile>> allModLangFiles = resourceAdapter.getAllModLangFiles();

            Set<String> loadedModIds = allModLangFiles.keySet();
            Set<String> existingModIds = existingData.containsKey(normalizedLang)
                    ? existingData.getMods(normalizedLang).keySet()
                    : new HashSet<>();

            Set<String> candidates = filterCandidates(loadedModIds, existingModIds);
            logger.info("Found " + candidates.size() + " candidate mods to extract");

            for (String modId : candidates) {
                int keysBefore = existingData.containsKey(normalizedLang)
                        ? existingData.getMods(normalizedLang).getOrDefault(modId, new ExtractedModData()).getEntryCount()
                        : 0;
                processMod(modId, allModLangFiles.get(modId), normalizedLang, existingData);
                ExtractedModData after = existingData.getMod(normalizedLang, modId);
                if (after != null && after.getEntryCount() > keysBefore) {
                    processedModCount++;
                    totalKeys += after.getEntryCount();
                }
            }

            removeUnloadedMods(existingData, normalizedLang, loadedModIds);
            saveExtractedData(existingData);

            int aiFiles = aiFileGenerator.generate(normalizedLang);
            Map<String, Object> aiStats = aiFileGenerator.getLastStats();
            int dictHits = aiStats.containsKey("dict_hits") ? (int) aiStats.get("dict_hits") : 0;

            int totalModCount = existingData.containsKey(normalizedLang)
                    ? existingData.getMods(normalizedLang).size() : 0;
            int totalKeyCount = 0;
            if (existingData.containsKey(normalizedLang)) {
                for (ExtractedModData mod : existingData.getMods(normalizedLang).values()) {
                    totalKeyCount += mod.getEntryCount();
                }
            }

            lastStats.clear();
            lastStats.put("success", true);
            lastStats.put("processed_mods", processedModCount);
            lastStats.put("processed_keys", totalKeys);
            lastStats.put("total_mods", totalModCount);
            lastStats.put("total_keys", totalKeyCount);
            lastStats.put("ai_files", aiFiles);
            lastStats.put("dict_hits", dictHits);

            logger.info("Extraction completed successfully, generated " + aiFiles + " AItranslation file(s)");
            return true;
        } catch (Exception e) {
            logger.error("Extraction failed", e);
            lastStats.clear();
            lastStats.put("success", false);
            return false;
        }
    }

    public Map<String, Object> getLastStats() {
        return lastStats;
    }

    /**
     * 加载已有的 extracted.json
     */
    private ExtractedData loadExtractedData() {
        Path path = pathHelper.getExtractedPath();
        return JsonUtil.readFromFile(path, ExtractedData.class);
    }

    /**
     * 筛选候选模组
     */
    private Set<String> filterCandidates(Set<String> loadedModIds, Set<String> existingModIds) {
        String broadScope = configManager.getString("extract_broad_scope", "new");
        String listMode = configManager.getString("extract_list_mode", "blacklist");
        String modList = configManager.getString("extract_list", "");

        Set<String> filtered = new HashSet<>();

        for (String modId : loadedModIds) {
            boolean isNew = !existingModIds.contains(modId);

            if ("new".equalsIgnoreCase(broadScope) && !isNew) {
                continue;
            }

            if (!modList.isEmpty()) {
                Set<String> listSet = ModFilter.parseModList(modList);
                boolean inList = listSet.contains(modId);

                if ("blacklist".equalsIgnoreCase(listMode)) {
                    if (inList) {
                        continue;
                    }
                } else {
                    if (!inList) {
                        continue;
                    }
                }
            }

            filtered.add(modId);
        }

        return filtered;
    }

    /**
     * 处理单个模组
     */
    private void processMod(String modId, List<ModLangFile> langFiles, String targetLanguage,
                           ExtractedData existingData) {
        if (langFiles == null || langFiles.isEmpty()) {
            return;
        }

        // 跳过 Minecraft 本体，仅提取额外模组
        if ("minecraft".equals(modId)) {
            return;
        }

        String normalizedTarget = LangUtil.normalize(targetLanguage);
        String sourceLang = langDetector.detect(langFiles, normalizedTarget);
        String currentHash = calculateModFileHash(langFiles);

        // 目标语言在最大键数量列表中，跳过该模组
        if (sourceLang == null) {
            logger.debug("Mod " + modId + " target language in max key list, skipping");
            return;
        }

        if (sourceLang.equals(normalizedTarget)) {
            logger.debug("Mod " + modId + " source language equals target language, skipping");
            return;
        }

        ExtractedModData existingModData = existingData.getMod(targetLanguage, modId);
        if (existingModData != null && currentHash.equals(existingModData.getModFileHash())) {
            logger.debug("Mod " + modId + " hash unchanged, skipping");
            return;
        }

        logger.info("Processing mod: " + modId + " (source: " + sourceLang + ")");

        Map<String, String> entries = extractEntries(langFiles, sourceLang, targetLanguage);

        // 获取源语言文件的格式
        LangFormat langFormat = LangFormat.JSON;
        for (ModLangFile lf : langFiles) {
            if (LangUtil.normalize(lf.getLanguage()).equals(sourceLang)) {
                langFormat = lf.getFormat();
                break;
            }
        }

        ExtractedModData modData = new ExtractedModData();
        modData.setSourceLang(sourceLang);
        modData.setExtractDate(LocalDateTime.now().format(DATE_FORMATTER));
        modData.setModFileHash(currentHash);
        modData.setLangFormat(langFormat);
        modData.getEntries().putAll(entries);

        existingData.putMod(targetLanguage, modId, modData);

        logger.info("Extracted " + entries.size() + " entries for " + modId);
    }

    /**
     * 计算模组的语言文件哈希
     * 仅计算语言文件内容，不算整个 jar
     */
    private String calculateModFileHash(List<ModLangFile> langFiles) {
        List<String> allValues = new ArrayList<>();
        for (ModLangFile langFile : langFiles) {
            for (Map.Entry<String, String> entry : langFile.getEntries().entrySet()) {
                allValues.add(entry.getKey() + "=" + entry.getValue());
            }
        }
        return HashUtil.crc32(allValues);
    }

    /**
     * 提取键值对
     */
    private Map<String, String> extractEntries(List<ModLangFile> langFiles, String sourceLang,
                                                String targetLanguage) {
        Map<String, String> sourceEntries = new HashMap<>();
        Map<String, String> targetEntries = new HashMap<>();

        for (ModLangFile langFile : langFiles) {
            String lang = LangUtil.normalize(langFile.getLanguage());
            if (lang.equals(sourceLang)) {
                sourceEntries.putAll(langFile.getEntries());
            } else if (lang.equals(targetLanguage)) {
                targetEntries.putAll(langFile.getEntries());
            }
        }

        ExtractMode mode = ExtractMode.fromString(configManager.getString("extract_mode", "diff"));

        Map<String, String> result = new HashMap<>();

        for (Map.Entry<String, String> entry : sourceEntries.entrySet()) {
            String key = entry.getKey();
            String sourceValue = entry.getValue();
            String targetValue = targetEntries.get(key);

            boolean include = false;

            switch (mode) {
                case DIFF:
                    include = targetValue == null || targetValue.isEmpty();
                    break;
                case DIFF_SAME:
                    include = targetValue == null || targetValue.isEmpty() || targetValue.equals(sourceValue);
                    break;
                case FULL:
                    include = true;
                    break;
            }

            if (include) {
                result.put(key, sourceValue);
            }
        }

        return result;
    }

    /**
     * 移除已卸载的模组
     */
    private void removeUnloadedMods(ExtractedData existingData, String normalizedLang, Set<String> loadedModIds) {
        Map<String, ExtractedModData> mods = existingData.get(normalizedLang);
        if (mods == null) {
            return;
        }

        Set<String> removed = new HashSet<>();
        for (String modId : mods.keySet()) {
            if (!loadedModIds.contains(modId)) {
                removed.add(modId);
            }
        }

        for (String modId : removed) {
            existingData.removeMod(normalizedLang, modId);
            logger.info("Removed unloaded mod from extracted data: " + modId);
        }
    }

    /**
     * 保存 extracted.json（原子写入）
     */
    private void saveExtractedData(ExtractedData data) throws Exception {
        Path path = pathHelper.getExtractedPath();
        Path tempPath = path.resolveSibling(path.getFileName() + ".tmp");

        JsonUtil.writeToFile(data, tempPath);

        if (Files.exists(tempPath)) {
            Files.deleteIfExists(path);
            Files.move(tempPath, path);
        }
    }

    /**
     * 获取提取的键数量统计
     * @param targetLanguage 目标语言代码
     * @return 模组 ID → 键数量的映射
     */
    public Map<String, Integer> getExtractStats(String targetLanguage) {
        ExtractedData data = loadExtractedData();
        Map<String, Integer> stats = new HashMap<>();
        Map<String, ExtractedModData> mods = data.get(targetLanguage);
        if (mods != null) {
            for (Map.Entry<String, ExtractedModData> entry : mods.entrySet()) {
                stats.put(entry.getKey(), entry.getValue().getEntryCount());
            }
        }
        return stats;
    }

    /**
     * 获取指定模组的提取数据
     * @param targetLanguage 目标语言代码
     * @param modId 模组 ID
     * @return 提取数据，不存在返回 null
     */
    public ExtractedModData getModExtractData(String targetLanguage, String modId) {
        ExtractedData data = loadExtractedData();
        return data.getMod(targetLanguage, modId);
    }

    /**
     * 获取所有已提取的模组列表
     * @param targetLanguage 目标语言代码
     * @return 模组 ID 列表
     */
    public List<String> getExtractedModIds(String targetLanguage) {
        ExtractedData data = loadExtractedData();
        Map<String, ExtractedModData> mods = data.get(targetLanguage);
        if (mods == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(mods.keySet());
    }
}