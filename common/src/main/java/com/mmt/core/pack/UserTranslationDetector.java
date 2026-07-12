package com.mmt.core.pack;

import com.mmt.core.data.model.packed.PackedData;
import com.mmt.core.data.model.packed.PackedEntry;
import com.mmt.core.data.model.packed.PackedModData;
import com.mmt.core.data.model.translated.TranslatedEntry;
import com.mmt.core.data.model.translated.TranslatedModData;
import com.mmt.core.log.MmtLogger;
import com.mmt.core.util.HashUtil;

import java.util.Collections;
import java.util.Map;

public class UserTranslationDetector {
    private final MmtLogger logger;

    public UserTranslationDetector(MmtLogger logger) {
        this.logger = logger;
    }

    public int detect(Map<String, TranslatedModData> translatedMods, PackedData packedData,
                       String language, Map<String, Map<String, String>> existingLangFiles) {
        if (packedData == null) {
            return 0;
        }

        Map<String, PackedModData> packedMods = packedData.get(language);
        if (packedMods == null) {
            return 0;
        }

        int detectedCount = 0;

        for (Map.Entry<String, PackedModData> packedEntry : packedMods.entrySet()) {
            String modId = packedEntry.getKey();
            PackedModData packedMod = packedEntry.getValue();

            TranslatedModData translatedMod = translatedMods.get(modId);
            if (translatedMod == null) {
                continue;
            }

            Map<String, String> existingLang = existingLangFiles.getOrDefault(modId, Collections.emptyMap());

            for (Map.Entry<String, PackedEntry> packedKeyEntry : packedMod.getEntries().entrySet()) {
                String key = packedKeyEntry.getKey();
                PackedEntry packedEntryData = packedKeyEntry.getValue();

                String storedHash = packedEntryData.getValueHash();
                String existingValue = existingLang.get(key);

                if (existingValue != null && !"00000000".equals(storedHash)) {
                    String currentHash = HashUtil.crc32(existingValue);

                    if (!currentHash.equals(storedHash)) {
                        logger.info("User translation detected for " + modId + ":" + key);

                        TranslatedEntry translatedEntry = new TranslatedEntry();
                        translatedEntry.setValue(existingValue);
                        translatedEntry.setMethod("user-translated");
                        translatedEntry.setSourceValue(packedEntryData.getSourceValue());
                        translatedMod.putEntry(key, translatedEntry);

                        packedEntryData.setValueHash("00000000");
                        packedEntryData.setValue(existingValue);
                        packedEntryData.setMethod("user-translated");
                        detectedCount++;
                    }
                }
            }
        }
        return detectedCount;
    }

    public void markUserTranslated(TranslatedModData translatedMod, String key, String value, String sourceValue) {
        TranslatedEntry entry = new TranslatedEntry();
        entry.setValue(value);
        entry.setMethod("user-translated");
        entry.setSourceValue(sourceValue);
        translatedMod.putEntry(key, entry);
    }
}