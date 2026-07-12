package com.mmt.core.translate;

import com.mmt.core.config.ConfigManager;
import com.mmt.core.data.json.JsonUtil;
import com.mmt.core.data.model.extracted.ExtractedData;
import com.mmt.core.data.model.extracted.ExtractedModData;
import com.mmt.core.data.model.translated.TranslatedData;
import com.mmt.core.data.model.translated.TranslatedEntry;
import com.mmt.core.data.model.translated.TranslatedModData;
import com.mmt.core.data.path.PathHelper;
import com.mmt.core.log.MmtLogger;
import com.mmt.core.util.LangUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 翻译器主类
 * 调度各翻译方式，按顺序执行翻译
 */
public class Translator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ConfigManager configManager;
    private final PathHelper pathHelper;
    private final MmtLogger logger;

    private Map<String, Object> lastStats = new HashMap<>();

    public Translator(ConfigManager configManager, PathHelper pathHelper, MmtLogger logger) {
        this.configManager = configManager;
        this.pathHelper = pathHelper;
        this.logger = logger;
    }

    /**
     * 执行翻译流程（单个语言）
     * @param targetLanguage 目标语言代码
     * @return 是否翻译成功
     */
    public boolean translate(String targetLanguage) {
        String normalizedLang = LangUtil.normalize(targetLanguage);
        logger.info("Starting translation for target language: " + normalizedLang);

        try {
            ExtractedData extractedData = loadExtractedData();
            TranslatedData translatedData = loadTranslatedData();

            // 从 extractedData 中移除 translatedData 中已翻译的键
            removeAlreadyTranslated(extractedData, translatedData, normalizedLang);

            List<ITranslator> translators = getTranslators();

            int totalKeys = 0;
            Map<String, ExtractedModData> mods = extractedData.get(normalizedLang);
            if (mods != null) {
                for (ExtractedModData mod : mods.values()) {
                    totalKeys += mod.getEntryCount();
                }
            }

            int totalHits = 0;
            Map<String, Integer> methodHits = new LinkedHashMap<>();

            for (ITranslator translator : translators) {
                if (!translator.isSupported(configManager)) {
                    continue;
                }

                logger.info("Running translator: " + translator.getMethod().getDisplayName());

                int hits = translator.translate(extractedData, translatedData,
                        normalizedLang, configManager, pathHelper, logger);

                totalHits += hits;
                if (hits > 0) {
                    methodHits.put(translator.getMethod().getDisplayName(), hits);
                }

                cleanupEmptyMods(extractedData, normalizedLang);
                cleanupEmptyMods(translatedData, normalizedLang);
            }

            saveTranslatedData(translatedData);

            int failedKeys = Math.max(0, totalKeys - totalHits);

            int modCount = 0;
            Map<String, TranslatedModData> translatedMods = translatedData.get(normalizedLang);
            if (translatedMods != null) {
                modCount = translatedMods.size();
            }

            lastStats.clear();
            lastStats.put("success", true);
            lastStats.put("mod_count", modCount);
            lastStats.put("total_keys", totalKeys);
            lastStats.put("translated_keys", totalHits);
            lastStats.put("failed_keys", failedKeys);
            lastStats.put("method_hits", methodHits);

            logger.info("Translation completed: " + totalHits + " total hits");
            return true;
        } catch (Exception e) {
            logger.error("Translation failed", e);
            lastStats.clear();
            lastStats.put("success", false);
            return false;
        }
    }

    public Map<String, Object> getLastStats() {
        return lastStats;
    }

    /**
     * 执行翻译流程（所有已提取语言）
     * 遍历 extracted.json 中所有有数据的语言，逐一执行翻译
     * @return 是否所有语言翻译成功
     */
    public boolean translateAll() {
        logger.info("Starting translation for all extracted languages");

        ExtractedData extractedData = loadExtractedData();

        boolean allSuccess = true;
        int totalHits = 0;

        for (String language : extractedData.keySet()) {
            logger.info("Translating language: " + language);
            boolean success = translate(language);
            if (!success) {
                allSuccess = false;
            }
        }

        logger.info("Batch translation completed for " + extractedData.keySet().size() + " languages");
        return allSuccess;
    }

    /**
     * 获取配置的翻译方式列表
     */
    private List<ITranslator> getTranslators() {
        String methodsStr = configManager.getString("translate_methods", "I18n-dict,AI-manual");
        String[] methodNames = methodsStr.split(",");

        List<ITranslator> translators = new ArrayList<>();

        for (String methodName : methodNames) {
            methodName = methodName.trim();
            ITranslator translator = createTranslator(methodName);
            if (translator != null) {
                translators.add(translator);
            }
        }

        return translators;
    }

    /**
     * 根据方法名称创建翻译器实例
     */
    private ITranslator createTranslator(String methodName) {
        if (methodName == null || methodName.isEmpty()) {
            return null;
        }

        switch (methodName.toLowerCase()) {
            case "i18n-dict":
            case "i18n_dict":
                return new I18nDictTranslator();
            case "ai-manual":
            case "ai_manual":
                return new AiManualTranslator();
            case "ai-auto":
            case "ai_auto":
                return new AiAutoTranslator();
            case "mt":
                return new MtTranslator();
            default:
                logger.warn("Unknown translation method: " + methodName);
                return null;
        }
    }

    /**
     * 加载 extracted.json
     */
    private ExtractedData loadExtractedData() {
        Path path = pathHelper.getExtractedPath();
        return JsonUtil.readFromFile(path, ExtractedData.class);
    }

    /**
     * 加载 translated.json
     */
    private TranslatedData loadTranslatedData() {
        Path path = pathHelper.getTranslatedPath();
        return JsonUtil.readFromFile(path, TranslatedData.class);
    }

    /**
     * 保存 translated.json（原子写入）
     */
    private void saveTranslatedData(TranslatedData data) throws Exception {
        Path path = pathHelper.getTranslatedPath();
        Path tempPath = path.resolveSibling(path.getFileName() + ".tmp");

        JsonUtil.writeToFile(data, tempPath);

        if (Files.exists(tempPath)) {
            Files.deleteIfExists(path);
            Files.move(tempPath, path);
        }
    }

    /**
     * 从 extractedData 中移除 translatedData 中已翻译的键
     * 只翻译 extracted 中有但 translated 中没有的键
     */
    private void removeAlreadyTranslated(ExtractedData extractedData, TranslatedData translatedData, String language) {
        Map<String, ExtractedModData> extractedMods = extractedData.get(language);
        if (extractedMods == null) {
            return;
        }

        for (Map.Entry<String, ExtractedModData> entry : extractedMods.entrySet()) {
            String modId = entry.getKey();
            ExtractedModData extractedMod = entry.getValue();

            TranslatedModData translatedMod = translatedData.getMod(language, modId);
            if (translatedMod == null) {
                continue;
            }

            Set<String> keysToRemove = new HashSet<>();
            for (Map.Entry<String, String> extEntry : extractedMod.getEntries().entrySet()) {
                String key = extEntry.getKey();
                String sourceValue = extEntry.getValue();

                TranslatedEntry translatedEntry = translatedMod.getEntries().get(key);
                if (translatedEntry != null
                        && translatedEntry.getSourceValue() != null
                        && translatedEntry.getSourceValue().equals(sourceValue)) {
                    keysToRemove.add(key);
                }
            }

            for (String key : keysToRemove) {
                extractedMod.removeEntry(key);
            }
        }
    }

    /**
     * 清理空模组数据
     */
    private void cleanupEmptyMods(ExtractedData data, String language) {
        Map<String, ExtractedModData> mods = data.get(language);
        if (mods == null) {
            return;
        }

        Set<String> emptyMods = new HashSet<>();
        for (Map.Entry<String, ExtractedModData> entry : mods.entrySet()) {
            if (entry.getValue().getEntries().isEmpty()) {
                emptyMods.add(entry.getKey());
            }
        }
        for (String modId : emptyMods) {
            data.removeMod(language, modId);
        }
    }

    /**
     * 清理空模组数据（重载）
     */
    private void cleanupEmptyMods(TranslatedData data, String language) {
        Map<String, TranslatedModData> mods = data.get(language);
        if (mods == null) {
            return;
        }

        Set<String> emptyMods = new HashSet<>();
        for (Map.Entry<String, TranslatedModData> entry : mods.entrySet()) {
            if (entry.getValue().getEntries().isEmpty()) {
                emptyMods.add(entry.getKey());
            }
        }
        for (String modId : emptyMods) {
            data.removeMod(language, modId);
        }
    }

    /**
     * 获取翻译统计信息
     * @param targetLanguage 目标语言代码
     * @return 统计信息映射
     */
    public Map<String, Object> getTranslationStats(String targetLanguage) {
        Map<String, Object> stats = new HashMap<>();

        TranslatedData translatedData = loadTranslatedData();
        int totalEntries = 0;
        Map<String, Integer> methodCounts = new HashMap<>();

        Map<String, TranslatedModData> mods = translatedData.get(targetLanguage);
        if (mods != null) {
            for (TranslatedModData modData : mods.values()) {
                for (TranslatedEntry entry : modData.getEntries().values()) {
                    totalEntries++;
                    String method = entry.getMethod();
                    methodCounts.merge(method, 1, Integer::sum);
                }
            }
        }

        stats.put("total_entries", totalEntries);
        stats.put("method_counts", methodCounts);

        return stats;
    }

    /**
     * 获取指定模组的翻译数据
     * @param targetLanguage 目标语言代码
     * @param modId 模组 ID
     * @return 翻译数据，不存在返回 null
     */
    public TranslatedModData getModTranslatedData(String targetLanguage, String modId) {
        TranslatedData data = loadTranslatedData();
        return data.getMod(targetLanguage, modId);
    }

    /**
     * 获取所有已翻译的模组列表
     * @param targetLanguage 目标语言代码
     * @return 模组 ID 列表
     */
    public List<String> getTranslatedModIds(String targetLanguage) {
        TranslatedData data = loadTranslatedData();
        Map<String, TranslatedModData> mods = data.get(targetLanguage);
        if (mods == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(mods.keySet());
    }
}