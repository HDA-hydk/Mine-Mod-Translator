package com.mmt.core.translate.ai;

import com.mmt.core.log.MmtLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AiFileParser {
    private static final Pattern MOD_SECTION_PATTERN = Pattern.compile("###\\s*\\[?([^|\\]]+)\\]?\\s*\\|\\s*\\[?([^|\\]#]+)\\]?\\s*###");
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("###\\s*FILE_NAME\\s*###");
    private static final Pattern AI_PROMPT_PATTERN = Pattern.compile("###\\s*AI_PROMPT\\s*###");
    private static final Pattern STATUS_PATTERN = Pattern.compile("###\\s*TRANSLATION_STATUS\\s*###");
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("\\s*([=:→])\\s*");
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("^\\s*```");
    private static final Pattern COMMENT_OR_EMPTY_PATTERN = Pattern.compile("^\\s*(#.*)?$");

    private final MmtLogger logger;

    public AiFileParser(MmtLogger logger) {
        this.logger = logger;
    }

    public ParsedResult parse(Path file) {
        try {
            String content = Files.readString(file);
            return parseContent(content);
        } catch (Exception e) {
            logger.error("Failed to read AI translation file: " + file, e);
            return new ParsedResult();
        }
    }

    public ParsedResult parseContent(String content) {
        ParsedResult result = new ParsedResult();

        if (content == null || content.isEmpty()) {
            return result;
        }

        String[] lines = content.split("\\r?\\n");
        String currentModId = null;
        String currentSourceLang = null;
        boolean inPrompt = false;
        boolean inCodeBlock = false;
        int lineNum = 0;

        for (String line : lines) {
            lineNum++;
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                continue;
            }

            if (CODE_BLOCK_PATTERN.matcher(trimmed).matches()) {
                inCodeBlock = !inCodeBlock;
                continue;
            }

            if (COMMENT_OR_EMPTY_PATTERN.matcher(trimmed).matches() && !trimmed.startsWith("###")) {
                continue;
            }

            if (inPrompt) {
                if (trimmed.startsWith("###")) {
                    inPrompt = false;
                } else {
                    continue;
                }
            }

            if (FILE_NAME_PATTERN.matcher(trimmed).matches()) {
                continue;
            }

            if (AI_PROMPT_PATTERN.matcher(trimmed).matches()) {
                inPrompt = true;
                continue;
            }

            if (STATUS_PATTERN.matcher(trimmed).matches()) {
                continue;
            }

            Matcher modMatcher = MOD_SECTION_PATTERN.matcher(trimmed);
            if (modMatcher.matches()) {
                currentModId = modMatcher.group(1).trim();
                currentSourceLang = modMatcher.group(2).trim();
                result.addModSection(currentModId, currentSourceLang);
                continue;
            }

            if (currentModId == null) {
                continue;
            }

            Matcher sepMatcher = SEPARATOR_PATTERN.matcher(trimmed);
            if (sepMatcher.find()) {
                int sepIndex = sepMatcher.start(1);
                String key = trimmed.substring(0, sepIndex).trim();
                String value = trimmed.substring(sepIndex + 1).trim();

                if (!key.isEmpty()) {
                    result.addEntry(currentModId, key, value);
                }
            }
        }

        if (result.getModEntries().isEmpty()) {
            logger.warn("No valid entries found in AI translation content");
        }

        return result;
    }

    public AiTranslationStatus parseStatus(Path file) {
        try {
            String content = Files.readString(file);
            String[] lines = content.split("\\r?\\n");

            boolean foundStatus = false;
            for (String line : lines) {
                if (foundStatus) {
                    return AiTranslationStatus.fromString(line);
                }
                if (STATUS_PATTERN.matcher(line.trim()).matches()) {
                    foundStatus = true;
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to parse status from file: " + file);
        }

        return AiTranslationStatus.UNTRANSLATED;
    }

    public static class ParsedResult {
        private final Map<String, ModSection> modEntries = new HashMap<>();

        public void addModSection(String modId, String sourceLang) {
            modEntries.computeIfAbsent(modId, k -> new ModSection()).sourceLang = sourceLang;
        }

        public void addEntry(String modId, String key, String value) {
            modEntries.computeIfAbsent(modId, k -> new ModSection()).entries.put(key, value);
        }

        public Map<String, ModSection> getModEntries() {
            return modEntries;
        }

        public boolean isEmpty() {
            return modEntries.isEmpty();
        }
    }

    public static class ModSection {
        public String sourceLang;
        public final Map<String, String> entries = new HashMap<>();
    }
}