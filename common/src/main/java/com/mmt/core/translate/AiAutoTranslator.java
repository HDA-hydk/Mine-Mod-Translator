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
import com.mmt.core.translate.ai.AiFileParser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AiAutoTranslator implements ITranslator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SYSTEM_PROMPT = "你是一个专业的 Minecraft 模组翻译助手。请将以下键值对中的英语值翻译成中文。\n" +
            "保持键名不变，只翻译等号后面的值。\n" +
            "翻译要准确、自然，符合 Minecraft 中文社区的用语习惯。\n" +
            "不要添加任何额外解释或说明，只输出翻译结果。\n" +
            "输出格式：键 = 翻译值";

    @Override
    public com.mmt.core.data.model.TranslateMethod getMethod() {
        return com.mmt.core.data.model.TranslateMethod.AI_AUTO;
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

        ApiConfig config = new ApiConfig("ai_api_url", "ai_api_key", "ai_api_model", configManager);
        if (!config.isValid()) {
            logger.warn("AI-auto API not configured, skipping");
            return 0;
        }

        logger.info("AI-auto translation starting with model: " + config.getApiModel());

        HttpUtil httpUtil = new HttpUtil(logger);
        RequestBuilder requestBuilder = new RequestBuilder();
        ResponseParser responseParser = new ResponseParser(logger);
        RateLimiter rateLimiter = new RateLimiter(logger, config.getRequestIntervalMs(), config.getRetryCount());

        List<RequestBuilder.ModTranslationChunk> chunks = requestBuilder.splitIntoChunks(
                mods, config.getRequestCharLimit());

        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < chunks.size(); i++) {
            RequestBuilder.ModTranslationChunk chunk = chunks.get(i);
            logger.info("Processing chunk " + (i + 1) + "/" + chunks.size() + " (" + chunk.getCharCount() + " chars)");

            try {
                String requestBody = requestBuilder.buildAiAutoRequest(SYSTEM_PROMPT, List.of(chunk), config.getApiModel());

                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + config.getApiKey());

                String response = rateLimiter.executeWithRetry(new RateLimiter.ApiCall() {
                    @Override
                    public String execute() throws HttpUtil.ApiException {
                        return httpUtil.postJson(config.getApiUrl(), requestBody, headers);
                    }

                    @Override
                    public String getKey() {
                        return "ai-auto";
                    }
                });

                String translationText = responseParser.parseAiAutoResponse(response);
                AiFileParser.ParsedResult parsedResult = responseParser.parseAiAutoTranslations(translationText);

                for (Map.Entry<String, AiFileParser.ModSection> modEntry : parsedResult.getModEntries().entrySet()) {
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
                        }
                    }
                }

            } catch (Exception e) {
                logger.error("AI-auto translation failed for chunk " + (i + 1), e);
                failCount++;
            }
        }

        logger.info("AI-auto completed: " + successCount + " successes, " + failCount + " failed chunks");
        return successCount;
    }

    @Override
    public boolean isSupported(ConfigManager configManager) {
        ApiConfig config = new ApiConfig("ai_api_url", "ai_api_key", "ai_api_model", configManager);
        return config.isValid();
    }
}