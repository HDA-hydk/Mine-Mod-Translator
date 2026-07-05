package com.mmt.core.translate.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mmt.core.log.MmtLogger;
import com.mmt.core.translate.ai.AiFileParser;

import java.util.ArrayList;
import java.util.List;

public class ResponseParser {
    private static final Gson gson = new Gson();
    private final MmtLogger logger;

    public ResponseParser(MmtLogger logger) {
        this.logger = logger;
    }

    public String parseAiAutoResponse(String response) throws ParseException {
        try {
            JsonObject json = gson.fromJson(response, JsonObject.class);

            if (json.has("choices")) {
                JsonArray choices = json.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    JsonObject choice = choices.get(0).getAsJsonObject();
                    if (choice.has("message")) {
                        JsonObject message = choice.getAsJsonObject("message");
                        if (message.has("content")) {
                            return message.get("content").getAsString();
                        }
                    }
                }
            }

            if (json.has("content")) {
                return json.get("content").getAsString();
            }

            throw new ParseException("Could not find content in AI response");
        } catch (Exception e) {
            throw new ParseException("Failed to parse AI response", e);
        }
    }

    public AiFileParser.ParsedResult parseAiAutoTranslations(String responseText) {
        AiFileParser parser = new AiFileParser(logger);
        return parser.parseContent(responseText);
    }

    public List<String> parseMtResponse(String response) throws ParseException {
        try {
            JsonObject json = gson.fromJson(response, JsonObject.class);

            if (json.has("translations")) {
                JsonArray translations = json.getAsJsonArray("translations");
                List<String> results = new ArrayList<>();
                for (JsonElement element : translations) {
                    if (element.isJsonObject()) {
                        JsonObject translation = element.getAsJsonObject();
                        if (translation.has("text")) {
                            results.add(translation.get("text").getAsString());
                        } else if (translation.has("translation")) {
                            results.add(translation.get("translation").getAsString());
                        }
                    } else if (element.isJsonPrimitive()) {
                        results.add(element.getAsString());
                    }
                }
                return results;
            }

            if (json.has("result")) {
                JsonElement result = json.get("result");
                if (result.isJsonArray()) {
                    List<String> results = new ArrayList<>();
                    for (JsonElement element : result.getAsJsonArray()) {
                        results.add(element.getAsString());
                    }
                    return results;
                } else if (result.isJsonPrimitive()) {
                    List<String> results = new ArrayList<>();
                    results.add(result.getAsString());
                    return results;
                }
            }

            throw new ParseException("Could not find translations in MT response");
        } catch (Exception e) {
            throw new ParseException("Failed to parse MT response", e);
        }
    }

    public static class ParseException extends Exception {
        public ParseException(String message) {
            super(message);
        }

        public ParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}