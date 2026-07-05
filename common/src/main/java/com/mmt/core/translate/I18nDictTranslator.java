package com.mmt.core.translate;

import com.mmt.core.config.ConfigManager;
import com.mmt.core.data.model.extracted.ExtractedData;
import com.mmt.core.data.model.extracted.ExtractedModData;
import com.mmt.core.data.model.translated.TranslatedData;
import com.mmt.core.data.model.translated.TranslatedEntry;
import com.mmt.core.data.model.translated.TranslatedModData;
import com.mmt.core.data.path.PathHelper;
import com.mmt.core.log.MmtLogger;
import com.mmt.core.translate.dict.I18nDict;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class I18nDictTranslator implements ITranslator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public com.mmt.core.data.model.TranslateMethod getMethod() {
        return com.mmt.core.data.model.TranslateMethod.I18N_DICT;
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

        if (!"zh_cn".equals(targetLanguage) && !"zh_tw".equals(targetLanguage)) {
            logger.debug("I18n-dict only supports Chinese target language, skipping");
            return 0;
        }

        I18nDict dict = I18nDict.getInstance(logger);
        dict.load();

        if (!dict.isLoaded()) {
            logger.debug("I18n-dict not loaded, skipping");
            return 0;
        }

        int hitCount = 0;

        for (Map.Entry<String, ExtractedModData> modEntry : mods.entrySet()) {
            String modId = modEntry.getKey();
            ExtractedModData extractedMod = modEntry.getValue();

            if (!"en_us".equals(extractedMod.getSourceLang())) {
                logger.debug("Mod " + modId + " source lang is not en_us, skipping I18n-dict");
                continue;
            }

            TranslatedModData translatedMod = translatedData.getMod(targetLanguage, modId);
            if (translatedMod == null) {
                translatedMod = new TranslatedModData();
                translatedMod.setSourceLang(extractedMod.getSourceLang());
                translatedMod.setTranslateDate(LocalDateTime.now().format(DATE_FORMATTER));
                translatedData.putMod(targetLanguage, modId, translatedMod);
            }

            List<String> matchedKeys = new ArrayList<>();

            for (Map.Entry<String, String> entry : extractedMod.getEntries().entrySet()) {
                String key = entry.getKey();
                String sourceValue = entry.getValue();

                String translation = dict.lookup(sourceValue);
                if (translation != null) {
                    TranslatedEntry translatedEntry = new TranslatedEntry();
                    translatedEntry.setValue(translation);
                    translatedEntry.setMethod(getMethod());
                    translatedEntry.setSourceValue(sourceValue);
                    translatedMod.putEntry(key, translatedEntry);

                    matchedKeys.add(key);
                    hitCount++;

                    logger.debug("I18n-dict match: '" + sourceValue + "' -> '" + translation + "'");
                }
            }

            for (String key : matchedKeys) {
                extractedMod.removeEntry(key);
            }
        }

        if (hitCount > 0) {
            logger.info("I18n-dict completed: " + hitCount + " hits");
        }

        return hitCount;
    }

    @Override
    public boolean isSupported(ConfigManager configManager) {
        return true;
    }
}