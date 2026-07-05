package com.mmt.core.translate;

import com.mmt.core.config.ConfigManager;
import com.mmt.core.data.model.extracted.ExtractedData;
import com.mmt.core.data.model.extracted.ExtractedModData;
import com.mmt.core.data.model.translated.TranslatedData;
import com.mmt.core.data.model.translated.TranslatedEntry;
import com.mmt.core.data.model.translated.TranslatedModData;
import com.mmt.core.data.path.PathHelper;
import com.mmt.core.log.MmtLogger;
import com.mmt.core.translate.ai.AiFileManager;
import com.mmt.core.translate.ai.AiFileParser;
import com.mmt.core.translate.ai.AiTranslationStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AiManualTranslator implements ITranslator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public com.mmt.core.data.model.TranslateMethod getMethod() {
        return com.mmt.core.data.model.TranslateMethod.AI_MANUAL;
    }

    @Override
    public int translate(ExtractedData extractedData, TranslatedData translatedData,
                        String targetLanguage, ConfigManager configManager,
                        PathHelper pathHelper, MmtLogger logger) {
        if (extractedData == null) {
            return 0;
        }

        Map<String, ExtractedModData> mods = extractedData.get(targetLanguage);
        if (mods == null || mods.isEmpty()) {
            return 0;
        }

        AiFileManager fileManager = new AiFileManager(pathHelper, logger);
        AiFileParser parser = new AiFileParser(logger);

        List<Path> nnFiles;
        try {
            nnFiles = fileManager.findAiTranslationFiles(targetLanguage);
        } catch (Exception e) {
            logger.error("Failed to find AI translation files", e);
            return 0;
        }

        List<Path> translatedFiles = new ArrayList<>();
        boolean allUntranslated = true;
        boolean resultFileUsed = false;

        for (Path file : nnFiles) {
            AiTranslationStatus status = parser.parseStatus(file);
            if (status == AiTranslationStatus.TRANSLATED) {
                translatedFiles.add(file);
                allUntranslated = false;
            }
        }

        List<AiFileParser.ParsedResult> results = new ArrayList<>();

        for (Path file : translatedFiles) {
            AiFileParser.ParsedResult result = parser.parse(file);
            if (!result.isEmpty()) {
                results.add(result);
            }
        }

        if (allUntranslated) {
            Path resultFile = fileManager.getAiResultFile(targetLanguage);
            try {
                if (Files.exists(resultFile) && Files.size(resultFile) > 0) {
                    AiFileParser.ParsedResult result = parser.parse(resultFile);
                    if (!result.isEmpty()) {
                        results.add(result);
                        resultFileUsed = true;
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to read AItranslationResult_" + targetLanguage + ".txt", e);
            }
        }

        if (results.isEmpty()) {
            logger.debug("No AI translation files to parse, skipping AI-manual");
            return 0;
        }

        int successCount = 0;
        int failCount = 0;
        List<String> failedKeys = new ArrayList<>();

        for (AiFileParser.ParsedResult result : results) {
            for (Map.Entry<String, AiFileParser.ModSection> modEntry : result.getModEntries().entrySet()) {
                String modId = modEntry.getKey();
                AiFileParser.ModSection section = modEntry.getValue();

                ExtractedModData extractedMod = extractedData.getMod(targetLanguage, modId);
                if (extractedMod == null) {
                    continue;
                }

                TranslatedModData translatedMod = translatedData.getMod(targetLanguage, modId);
                if (translatedMod == null) {
                    translatedMod = new TranslatedModData();
                    translatedMod.setSourceLang(extractedMod.getSourceLang());
                    translatedMod.setTranslateDate(LocalDateTime.now().format(DATE_FORMATTER));
                    translatedData.putMod(targetLanguage, modId, translatedMod);
                }

                for (Map.Entry<String, String> entry : section.entries.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    if (extractedMod.getEntries().containsKey(key)) {
                        String sourceValue = extractedMod.getEntries().get(key);

                        TranslatedEntry translatedEntry = new TranslatedEntry();
                        translatedEntry.setValue(value);
                        translatedEntry.setMethod(getMethod());
                        translatedEntry.setSourceValue(sourceValue);
                        translatedMod.putEntry(key, translatedEntry);

                        extractedMod.removeEntry(key);
                        successCount++;
                    } else {
                        failCount++;
                        if (failedKeys.size() < 10) {
                            failedKeys.add(modId + ":" + key);
                        }
                    }
                }
            }
        }

        if (failCount > 0) {
            logger.warn("AI-manual: " + failCount + " keys failed to match. Examples: " + String.join(", ", failedKeys));
        }

        logger.info("AI-manual completed: " + successCount + " successes, " + failCount + " failures");

        try {
            fileManager.archiveFiles(translatedFiles);
            if (resultFileUsed) {
                fileManager.archiveResultFile(targetLanguage);
            }
        } catch (Exception e) {
            logger.error("Failed to archive AI translation files", e);
        }

        return successCount;
    }

    @Override
    public boolean isSupported(ConfigManager configManager) {
        return true;
    }
}