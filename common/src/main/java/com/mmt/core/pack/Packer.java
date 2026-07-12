package com.mmt.core.pack;

import com.mmt.core.config.ConfigManager;
import com.mmt.core.data.json.JsonUtil;
import com.mmt.core.data.model.extracted.ExtractedData;
import com.mmt.core.data.model.extracted.ExtractedModData;
import com.mmt.core.data.model.packed.PackedData;
import com.mmt.core.data.model.packed.PackedEntry;
import com.mmt.core.data.model.packed.PackedModData;
import com.mmt.core.data.model.translated.TranslatedData;
import com.mmt.core.data.model.translated.TranslatedEntry;
import com.mmt.core.data.model.translated.TranslatedModData;
import com.mmt.core.data.path.PathHelper;
import com.mmt.core.extract.model.LangFormat;
import com.mmt.core.log.MmtLogger;
import com.mmt.core.util.HashUtil;
import com.mmt.core.util.LangUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Packer {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ConfigManager configManager;
    private final PathHelper pathHelper;
    private final MmtLogger logger;

    private Map<String, Object> lastStats = new HashMap<>();

    public Packer(ConfigManager configManager, PathHelper pathHelper, MmtLogger logger) {
        this.configManager = configManager;
        this.pathHelper = pathHelper;
        this.logger = logger;
    }

    public boolean pack(String targetLanguage, String mcVersion) {
        String normalizedLang = LangUtil.normalize(targetLanguage);
        logger.info("Starting packing for target language: " + normalizedLang);

        try {
            TranslatedData translatedData = loadTranslatedData();
            PackedData packedData = loadPackedData();

            Map<String, Map<String, String>> existingLangFiles = new ResourcePackGenerator(pathHelper, logger)
                    .loadExistingLangFiles(normalizedLang);

            UserTranslationDetector detector = new UserTranslationDetector(logger);
            Map<String, TranslatedModData> translatedMods = translatedData.getMods(normalizedLang);
            int userTranslatedCount = detector.detect(translatedMods, packedData, normalizedLang, existingLangFiles);

            int overriddenCount = 0;
            Map<String, TranslatedModData> filteredTranslations = new HashMap<>();

            Map<String, TranslatedModData> mods = translatedData.get(normalizedLang);
            if (mods != null) {
                for (Map.Entry<String, TranslatedModData> modEntry : mods.entrySet()) {
                    String modId = modEntry.getKey();
                    TranslatedModData translatedMod = modEntry.getValue();

                    TranslatedModData filteredMod = new TranslatedModData();
                    filteredMod.setSourceLang(translatedMod.getSourceLang());
                    filteredMod.setTranslateDate(translatedMod.getTranslateDate());

                    PackedModData packedMod = packedData.getMod(normalizedLang, modId);

                    for (Map.Entry<String, TranslatedEntry> entry : translatedMod.getEntries().entrySet()) {
                        String key = entry.getKey();
                        TranslatedEntry translatedEntry = entry.getValue();

                        boolean shouldOverride = true;
                        if (packedMod != null && packedMod.getEntries().containsKey(key)) {
                            PackedEntry packedEntry = packedMod.getEntries().get(key);
                            String existingMethod = packedEntry.getMethod();
                            String newMethod = translatedEntry.getMethod();

                            boolean sameMethod = existingMethod != null && existingMethod.equals(newMethod);

                            if (!PriorityResolver.shouldOverride(existingMethod, newMethod)) {
                                shouldOverride = false;
                                if (!sameMethod) {
                                    overriddenCount++;
                                }

                                TranslatedEntry keptEntry = new TranslatedEntry();
                                keptEntry.setValue(packedEntry.getValue());
                                keptEntry.setMethod(packedEntry.getMethod());
                                keptEntry.setSourceValue(packedEntry.getSourceValue());
                                filteredMod.putEntry(key, keptEntry);
                            }
                        }

                        if (shouldOverride) {
                            filteredMod.putEntry(key, translatedEntry);
                        }
                    }

                    if (!filteredMod.getEntries().isEmpty()) {
                        filteredTranslations.put(modId, filteredMod);
                    }
                }
            }

            ResourcePackGenerator generator = new ResourcePackGenerator(pathHelper, logger);

            // 从 extractedData 中收集每个模组的语言文件格式
            Map<String, LangFormat> modFormats = new HashMap<>();
            try {
                ExtractedData extractedData = loadExtractedData();
                Map<String, ExtractedModData> extractedMods = extractedData.getMods(normalizedLang);
                if (extractedMods != null) {
                    for (Map.Entry<String, ExtractedModData> entry : extractedMods.entrySet()) {
                        modFormats.put(entry.getKey(), entry.getValue().getLangFormat());
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to load extracted data for format info, defaulting to JSON");
            }

            generator.generate(normalizedLang, filteredTranslations, packedData, mcVersion, modFormats);

            savePackedData(packedData);

            int modCount = filteredTranslations.size();
            int totalKeys = 0;
            for (TranslatedModData mod : filteredTranslations.values()) {
                totalKeys += mod.getEntryCount();
            }

            lastStats.clear();
            lastStats.put("success", true);
            lastStats.put("mod_count", modCount);
            lastStats.put("total_keys", totalKeys);
            lastStats.put("user_translated", userTranslatedCount);
            lastStats.put("overridden", overriddenCount);

            logger.info("Packing completed successfully");
            return true;
        } catch (Exception e) {
            logger.error("Packing failed", e);
            lastStats.clear();
            lastStats.put("success", false);
            return false;
        }
    }

    public Map<String, Object> getLastStats() {
        return lastStats;
    }

    private TranslatedData loadTranslatedData() {
        Path path = pathHelper.getTranslatedPath();
        return JsonUtil.readFromFile(path, TranslatedData.class);
    }

    private ExtractedData loadExtractedData() {
        Path path = pathHelper.getExtractedPath();
        return JsonUtil.readFromFile(path, ExtractedData.class);
    }

    private PackedData loadPackedData() {
        Path path = pathHelper.getPackedPath();
        return JsonUtil.readFromFile(path, PackedData.class);
    }

    private void savePackedData(PackedData data) throws Exception {
        Path path = pathHelper.getPackedPath();
        Path tempPath = path.resolveSibling(path.getFileName() + ".tmp");

        JsonUtil.writeToFile(data, tempPath);

        if (Files.exists(tempPath)) {
            Files.deleteIfExists(path);
            Files.move(tempPath, path);
        }
    }

    /**
     * 打包所有已翻译语言
     * 遍历 translated.json 中所有有数据的语言，逐一打包
     * @param mcVersion Minecraft 版本
     * @return 是否所有语言打包成功
     */
    public boolean packAll(String mcVersion) {
        logger.info("Starting packing for all translated languages");

        TranslatedData translatedData = loadTranslatedData();

        boolean allSuccess = true;
        for (String language : translatedData.keySet()) {
            logger.info("Packing language: " + language);
            boolean success = pack(language, mcVersion);
            if (!success) {
                allSuccess = false;
            }
        }

        logger.info("Batch packing completed for " + translatedData.keySet().size() + " languages");
        return allSuccess;
    }

    public Map<String, Object> getPackStats(String targetLanguage) {
        Map<String, Object> stats = new HashMap<>();

        PackedData packedData = loadPackedData();
        int totalEntries = 0;
        Map<String, PackedModData> mods = packedData.get(targetLanguage);
        int modCount = mods != null ? mods.size() : 0;
        Map<String, Integer> methodCounts = new HashMap<>();

        if (mods != null) {
            for (PackedModData modData : mods.values()) {
                for (PackedEntry entry : modData.getEntries().values()) {
                    totalEntries++;
                    String method = entry.getMethod();
                    methodCounts.merge(method, 1, Integer::sum);
                }
            }
        }

        stats.put("total_entries", totalEntries);
        stats.put("mod_count", modCount);
        stats.put("method_counts", methodCounts);

        return stats;
    }
}