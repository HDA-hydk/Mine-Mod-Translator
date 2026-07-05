package com.mmt.core.translate.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mmt.core.data.model.extracted.ExtractedModData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestBuilder {
    private static final Gson gson = new Gson();

    public String buildAiAutoRequest(String systemPrompt, List<ModTranslationChunk> chunks, String model) {
        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        StringBuilder userContent = new StringBuilder();
        for (ModTranslationChunk chunk : chunks) {
            userContent.append(chunk.toText());
        }

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userContent.toString());
        messages.add(userMessage);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.add("messages", gson.toJsonTree(messages));
        requestBody.addProperty("temperature", 0.7);

        return gson.toJson(requestBody);
    }

    public String buildMtRequest(List<String> texts, String sourceLang, String targetLang) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("source_lang", sourceLang);
        requestBody.addProperty("target_lang", targetLang);
        requestBody.add("texts", gson.toJsonTree(texts));

        return gson.toJson(requestBody);
    }

    public List<ModTranslationChunk> splitIntoChunks(Map<String, ExtractedModData> mods, int charLimit) {
        List<ModTranslationChunk> chunks = new ArrayList<>();
        ModTranslationChunk currentChunk = new ModTranslationChunk();

        for (Map.Entry<String, ExtractedModData> modEntry : mods.entrySet()) {
            String modId = modEntry.getKey();
            ExtractedModData modData = modEntry.getValue();
            String sourceLang = modData.getSourceLang();

            StringBuilder modSection = new StringBuilder();
            modSection.append("### ").append(modId).append("|").append(sourceLang).append(" ###\n");

            for (Map.Entry<String, String> entry : modData.getEntries().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                String line = key + " = " + value + "\n";

                if (currentChunk.getCharCount() + modSection.length() + line.length() > charLimit &&
                    !currentChunk.isEmpty()) {
                    chunks.add(currentChunk);
                    currentChunk = new ModTranslationChunk();
                    modSection = new StringBuilder();
                    modSection.append("### ").append(modId).append("|").append(sourceLang).append(" ###\n");
                }

                modSection.append(line);
            }

            if (modSection.length() > 0) {
                if (currentChunk.getCharCount() + modSection.length() > charLimit && !currentChunk.isEmpty()) {
                    chunks.add(currentChunk);
                    currentChunk = new ModTranslationChunk();
                }
                currentChunk.addModSection(modId, sourceLang, modSection.toString());
            }
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk);
        }

        return chunks;
    }

    public static class ModTranslationChunk {
        private final List<String> modSections = new ArrayList<>();
        private int charCount = 0;

        public void addModSection(String modId, String sourceLang, String content) {
            modSections.add(content);
            charCount += content.length();
        }

        public String toText() {
            StringBuilder sb = new StringBuilder();
            for (String section : modSections) {
                sb.append(section);
            }
            return sb.toString();
        }

        public int getCharCount() {
            return charCount;
        }

        public boolean isEmpty() {
            return modSections.isEmpty();
        }
    }
}