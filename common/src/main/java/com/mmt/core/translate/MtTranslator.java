package com.mmt.core.translate;

import com.mmt.core.config.ConfigManager;
import com.mmt.core.data.model.extracted.ExtractedData;
import com.mmt.core.data.model.extracted.ExtractedModData;
import com.mmt.core.data.model.translated.TranslatedData;
import com.mmt.core.data.model.translated.TranslatedEntry;
import com.mmt.core.data.model.translated.TranslatedModData;
import com.mmt.core.data.path.PathHelper;
import com.mmt.core.log.MmtLogger;
import com.mmt.core.translate.api.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MtTranslator implements ITranslator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public com.mmt.core.data.model.TranslateMethod getMethod() {
        return com.mmt.core.data.model.TranslateMethod.MT;
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

        ApiConfig config = new ApiConfig("mt_api_url", "mt_api_key", "mt_api_model", configManager);
        if (!config.isValid()) {
            logger.warn("MT API not configured, skipping");
            return 0;
        }

        logger.info("MT translation starting");

        HttpUtil httpUtil = new HttpUtil(logger);
        ResponseParser responseParser = new ResponseParser(logger);
        RateLimiter rateLimiter = new RateLimiter(logger, config.getRequestIntervalMs(), config.getRetryCount());

        List<String> texts = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        List<String> modIds = new ArrayList<>();
        List<String> sourceValues = new ArrayList<>();

        for (Map.Entry<String, ExtractedModData> modEntry : mods.entrySet()) {
            String modId = modEntry.getKey();
            ExtractedModData modData = modEntry.getValue();

            for (Map.Entry<String, String> entry : modData.getEntries().entrySet()) {
                String key = entry.getKey();
                String sourceValue = entry.getValue();

                keys.add(key);
                modIds.add(modId);
                sourceValues.add(sourceValue);
                texts.add(sourceValue);
            }
        }

        if (texts.isEmpty()) {
            return 0;
        }

        int batchSize = config.getRequestCharLimit() > 0 ? 50 : texts.size();
        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batchTexts = texts.subList(i, end);

            logger.info("Processing MT batch " + (i / batchSize + 1) + "/" + ((texts.size() + batchSize - 1) / batchSize));

            try {
                String requestBody = new RequestBuilder().buildMtRequest(batchTexts, "en", targetLanguage);

                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + config.getApiKey());

                String response = rateLimiter.executeWithRetry(new RateLimiter.ApiCall() {
                    @Override
                    public String execute() throws HttpUtil.ApiException {
                        return httpUtil.postJson(config.getApiUrl(), requestBody, headers);
                    }

                    @Override
                    public String getKey() {
                        return "mt";
                    }
                });

                List<String> translations = responseParser.parseMtResponse(response);

                for (int j = 0; j < translations.size(); j++) {
                    int idx = i + j;
                    if (idx >= modIds.size()) break;

                    String modId = modIds.get(idx);
                    String key = keys.get(idx);
                    String sourceValue = sourceValues.get(idx);
                    String translation = translations.get(j);

                    ExtractedModData extractedMod = extractedData.getMod(targetLanguage, modId);
                    if (extractedMod == null || !extractedMod.getEntries().containsKey(key)) {
                        continue;
                    }

                    TranslatedModData translatedMod = translatedData.getMod(targetLanguage, modId);
                    if (translatedMod == null) {
                        translatedMod = new TranslatedModData();
                        translatedMod.setSourceLang(extractedMod.getSourceLang());
                        translatedMod.setTranslateDate(LocalDateTime.now().format(DATE_FORMATTER));
                        translatedData.putMod(targetLanguage, modId, translatedMod);
                    }

                    TranslatedEntry translatedEntry = new TranslatedEntry();
                    translatedEntry.setValue(translation);
                    translatedEntry.setMethod(getMethod());
                    translatedEntry.setSourceValue(sourceValue);
                    translatedMod.putEntry(key, translatedEntry);

                    extractedMod.removeEntry(key);
                    successCount++;
                }

            } catch (Exception e) {
                logger.error("MT translation failed for batch " + (i / batchSize + 1), e);
                failCount++;
            }
        }

        logger.info("MT completed: " + successCount + " successes, " + failCount + " failed batches");
        return successCount;
    }

    @Override
    public boolean isSupported(ConfigManager configManager) {
        ApiConfig config = new ApiConfig("mt_api_url", "mt_api_key", "mt_api_model", configManager);
        return config.isValid();
    }
}