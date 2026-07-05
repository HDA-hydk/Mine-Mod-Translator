package com.mmt.core.extract;

import com.mmt.core.config.ConfigManager;
import com.mmt.core.data.json.JsonUtil;
import com.mmt.core.data.model.extracted.ExtractedData;
import com.mmt.core.data.model.extracted.ExtractedModData;
import com.mmt.core.data.model.translated.TranslatedData;
import com.mmt.core.data.model.translated.TranslatedEntry;
import com.mmt.core.data.model.translated.TranslatedModData;
import com.mmt.core.data.path.PathHelper;
import com.mmt.core.log.MmtLogger;
import com.mmt.core.translate.ai.AiFileParser;
import com.mmt.core.translate.ai.AiTranslationStatus;
import com.mmt.core.translate.dict.I18nDict;
import com.mmt.core.util.LangUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AItranslation_[目标语言]_NN.txt 生成器
 * 生成待翻译的文本文件，供 AI 翻译使用
 *
 * 切分算法（D15，2026-07-05）：
 * 1. 按模组分组，保证单个模组完整性
 * 2. 超限模组（> maxFileSize）：单独成文件，允许超过大小限制
 * 3. 可合并模组（<= maxFileSize）：first-fit 装箱
 * 4. 处理顺序：先可合并后超限
 * 5. 配置项 ai_split_oversized_mod=true 时，对超限模组内部按条目强制切分
 */
public class AiFileGenerator {
    private static final DateTimeFormatter ARCHIVE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Pattern AI_FILE_PATTERN = Pattern.compile("AItranslation_(\\w+)_(\\d{2})\\.txt");

    private final ConfigManager configManager;
    private final PathHelper pathHelper;
    private final MmtLogger logger;
    private final I18nDict i18nDict;

    private Map<String, Object> lastStats = new HashMap<>();

    public AiFileGenerator(ConfigManager configManager, PathHelper pathHelper, MmtLogger logger, I18nDict i18nDict) {
        this.configManager = configManager;
        this.pathHelper = pathHelper;
        this.logger = logger;
        this.i18nDict = i18nDict;
    }

    /**
     * 生成 AItranslation_[目标语言]_NN.txt 文件
     * @param targetLanguage 目标语言代码
     * @return 生成的文件数量
     */
    public int generate(String targetLanguage) {
        String normalizedLang = LangUtil.normalize(targetLanguage);
        logger.info("Generating AItranslation files for target language: " + normalizedLang);

        lastStats.clear();

        try {
            ExtractedData extractedData = loadExtractedData();
            TranslatedData translatedData = loadTranslatedData();

            int[] dictHitsHolder = new int[1];
            List<ModGroup> modGroups = collectAiEntriesByMod(extractedData, translatedData, normalizedLang, dictHitsHolder);
            int dictHits = dictHitsHolder[0];

            int aiEntryCount = 0;
            for (ModGroup mod : modGroups) {
                aiEntryCount += mod.entries.size();
            }

            lastStats.put("dict_hits", dictHits);
            lastStats.put("ai_entries", aiEntryCount);
            lastStats.put("ai_mods", modGroups.size());

            if (modGroups.isEmpty()) {
                logger.info("No entries to translate for " + normalizedLang);
                archiveOldFiles(normalizedLang);
                createEmptyResultFileIfNotExists(normalizedLang);
                lastStats.put("ai_files", 0);
                return 0;
            }

            archiveOldFiles(normalizedLang);

            int startFileNumber = findNextFileNumber(normalizedLang);

            int fileMaxSize = configManager.getInt("ai_file_max_size", -1);
            int fileCount;
            if (fileMaxSize <= 0) {
                writeSingleFile(flatten(modGroups), normalizedLang, startFileNumber);
                fileCount = 1;
            } else {
                fileCount = writeSplitFiles(modGroups, normalizedLang, fileMaxSize, startFileNumber);
            }

            createEmptyResultFileIfNotExists(normalizedLang);
            lastStats.put("ai_files", fileCount);
            return fileCount;
        } catch (Exception e) {
            logger.error("Failed to generate AItranslation files", e);
            lastStats.put("dict_hits", 0);
            lastStats.put("ai_entries", 0);
            lastStats.put("ai_files", 0);
            return 0;
        }
    }

    public Map<String, Object> getLastStats() {
        return lastStats;
    }

    /**
     * 收集待翻译的条目（按模组分组）
     * 只提取 extracted.json 中存在、但 translated.json 中尚无翻译、或源语言值已发生变化的键
     */
    private List<ModGroup> collectAiEntriesByMod(ExtractedData extractedData, TranslatedData translatedData,
                                                  String targetLanguage, int[] dictHitsHolder) {
        Map<String, ModGroup> modMap = new LinkedHashMap<>();

        Map<String, ExtractedModData> mods = extractedData.get(targetLanguage);
        if (mods == null) {
            return new ArrayList<>();
        }

        if (i18nDict != null && !i18nDict.isLoaded()) {
            i18nDict.load();
        }

        for (Map.Entry<String, ExtractedModData> modEntry : mods.entrySet()) {
            String modId = modEntry.getKey();
            ExtractedModData extractedMod = modEntry.getValue();
            String sourceLang = extractedMod.getSourceLang();

            TranslatedModData translatedMod = translatedData != null ? translatedData.getMod(targetLanguage, modId) : null;
            Map<String, TranslatedEntry> translatedEntries = translatedMod != null ? translatedMod.getEntries() : null;

            List<AiFileEntry> modEntries = new ArrayList<>();
            for (Map.Entry<String, String> entry : extractedMod.getEntries().entrySet()) {
                String key = entry.getKey();
                String sourceValue = entry.getValue();

                boolean needsTranslation = false;
                if (translatedEntries == null || !translatedEntries.containsKey(key)) {
                    needsTranslation = true;
                } else {
                    TranslatedEntry translatedEntry = translatedEntries.get(key);
                    if (translatedEntry.getSourceValue() == null ||
                        !translatedEntry.getSourceValue().equals(sourceValue)) {
                        needsTranslation = true;
                    }
                }

                if (needsTranslation && i18nDict != null && i18nDict.isLoaded()) {
                    String dictResult = i18nDict.lookup(sourceValue);
                    if (dictResult != null && !dictResult.isEmpty()) {
                        needsTranslation = false;
                        if (dictHitsHolder != null) {
                            dictHitsHolder[0]++;
                        }
                    }
                }

                if (needsTranslation) {
                    modEntries.add(new AiFileEntry(modId, sourceLang, key, sourceValue));
                }
            }

            if (!modEntries.isEmpty()) {
                int modSize = estimateModSize(modId, sourceLang, modEntries);
                modMap.put(modId, new ModGroup(modId, sourceLang, modEntries, modSize));
            }
        }

        return new ArrayList<>(modMap.values());
    }

    /**
     * 估算模组大小（含分节标记）
     */
    private int estimateModSize(String modId, String sourceLang, List<AiFileEntry> entries) {
        int size = modId.length() + sourceLang.length() + 20; // ### [modid]|[source_lang] ###\n\n
        for (AiFileEntry entry : entries) {
            size += estimateEntrySize(entry);
        }
        return size;
    }

    /**
     * 按新切分算法写入多个文件
     * 1. 分组：超限模组 + 可合并模组
     * 2. 对可合并模组执行 first-fit 装箱
     * 3. 超限模组：单独成文件（或按配置强制切分）
     */
    private int writeSplitFiles(List<ModGroup> modGroups, String targetLanguage, int maxSizeKb, int startFileCount) throws IOException {
        int maxSizeBytes = maxSizeKb * 1024;

        List<ModGroup> oversizedMods = new ArrayList<>();
        List<ModGroup> mergeableMods = new ArrayList<>();
        for (ModGroup mod : modGroups) {
            if (mod.size > maxSizeBytes) {
                oversizedMods.add(mod);
            } else {
                mergeableMods.add(mod);
            }
        }

        logger.info("Mod groups: " + modGroups.size() + " (oversized: " + oversizedMods.size()
                + ", mergeable: " + mergeableMods.size() + ")");

        int fileCount = startFileCount;

        // 第一步：对可合并模组执行 first-fit 装箱
        while (!mergeableMods.isEmpty()) {
            List<ModGroup> currentChunk = new ArrayList<>();
            int currentSize = 0;

            // 循环遍历队列，找能装下的模组加入当前文件
            boolean found = true;
            while (found) {
                found = false;
                Iterator<ModGroup> it = mergeableMods.iterator();
                while (it.hasNext()) {
                    ModGroup mod = it.next();
                    if (currentSize + mod.size <= maxSizeBytes) {
                        currentChunk.add(mod);
                        currentSize += mod.size;
                        it.remove();
                        found = true;
                    }
                }
            }

            // 安全保护：如果当前文件为空且队列还有模组，强制加入第一个
            if (currentChunk.isEmpty() && !mergeableMods.isEmpty()) {
                ModGroup mod = mergeableMods.remove(0);
                currentChunk.add(mod);
                currentSize += mod.size;
            }

            if (!currentChunk.isEmpty()) {
                String fileNumber = String.format("%02d", fileCount);
                Path filePath = pathHelper.getAiTranslationFile(targetLanguage, fileNumber);
                List<AiFileEntry> entries = flatten(currentChunk);
                writeAiFile(entries, filePath, targetLanguage, fileNumber);
                logger.info("Generated AItranslation_" + targetLanguage + "_" + fileNumber + ".txt ("
                        + entries.size() + " entries, " + currentChunk.size() + " mods, "
                        + (currentSize / 1024) + "KB)");
                fileCount++;
            }
        }

        // 第二步：处理超限模组
        boolean splitOversized = configManager.getBoolean("ai_split_oversized_mod", false);
        for (ModGroup mod : oversizedMods) {
            if (splitOversized) {
                fileCount = splitOversizedMod(mod, targetLanguage, maxSizeBytes, fileCount);
            } else {
                String fileNumber = String.format("%02d", fileCount);
                Path filePath = pathHelper.getAiTranslationFile(targetLanguage, fileNumber);
                writeAiFile(mod.entries, filePath, targetLanguage, fileNumber);
                logger.info("Generated AItranslation_" + targetLanguage + "_" + fileNumber + ".txt (oversized mod: "
                        + mod.modId + ", " + mod.entries.size() + " entries, " + (mod.size / 1024) + "KB)");
                fileCount++;
            }
        }

        return fileCount - startFileCount;
    }

    /**
     * 对超限模组内部按条目切分
     * 每个文件都包含该模组的分节标记
     */
    private int splitOversizedMod(ModGroup mod, String targetLanguage, int maxSizeBytes, int startFileCount) throws IOException {
        List<AiFileEntry> currentEntries = new ArrayList<>();
        int sectionHeaderSize = mod.modId.length() + mod.sourceLang.length() + 20;
        int currentSize = sectionHeaderSize;
        int fileCount = startFileCount;

        for (AiFileEntry entry : mod.entries) {
            int entrySize = estimateEntrySize(entry);
            if (currentSize + entrySize > maxSizeBytes && !currentEntries.isEmpty()) {
                String fileNumber = String.format("%02d", fileCount);
                Path filePath = pathHelper.getAiTranslationFile(targetLanguage, fileNumber);
                writeAiFile(currentEntries, filePath, targetLanguage, fileNumber);
                logger.info("Generated AItranslation_" + targetLanguage + "_" + fileNumber + ".txt (split oversized mod: "
                        + mod.modId + ", " + currentEntries.size() + " entries)");
                fileCount++;

                currentEntries = new ArrayList<>();
                currentSize = sectionHeaderSize;
            }
            currentEntries.add(entry);
            currentSize += entrySize;
        }

        if (!currentEntries.isEmpty()) {
            String fileNumber = String.format("%02d", fileCount);
            Path filePath = pathHelper.getAiTranslationFile(targetLanguage, fileNumber);
            writeAiFile(currentEntries, filePath, targetLanguage, fileNumber);
            logger.info("Generated AItranslation_" + targetLanguage + "_" + fileNumber + ".txt (split oversized mod: "
                    + mod.modId + ", " + currentEntries.size() + " entries)");
            fileCount++;
        }

        return fileCount;
    }

    /**
     * 把多个模组合并为一个条目列表（保持模组内顺序）
     */
    private List<AiFileEntry> flatten(List<ModGroup> modGroups) {
        List<AiFileEntry> entries = new ArrayList<>();
        for (ModGroup mod : modGroups) {
            entries.addAll(mod.entries);
        }
        return entries;
    }

    /**
     * 写入单个文件
     */
    private void writeSingleFile(List<AiFileEntry> entries, String targetLanguage, int fileNumber) throws IOException {
        String fileNumberStr = String.format("%02d", fileNumber);
        Path filePath = pathHelper.getAiTranslationFile(targetLanguage, fileNumberStr);
        writeAiFile(entries, filePath, targetLanguage, fileNumberStr);
        logger.info("Generated AItranslation_" + targetLanguage + "_" + fileNumberStr + ".txt");
    }

    /**
     * 写入 AI 文件（每个文件都包含完整的 AI 提示词）
     */
    private void writeAiFile(List<AiFileEntry> entries, Path filePath, String targetLanguage, String fileNumber) throws IOException {
        Path parentDir = filePath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        String fileName = filePath.getFileName().toString();

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath.toFile()), StandardCharsets.UTF_8))) {

            writer.write("### FILE_NAME ###");
            writer.newLine();
            writer.write(fileName);
            writer.newLine();
            writer.newLine();

            writer.write("### AI_PROMPT ###");
            writer.newLine();
            writer.write("You are a Minecraft mod translation assistant. Translate the values on the right side of \"=\" in each mod section below into the target language (" + targetLanguage + ").");
            writer.newLine();
            writer.write("Rules:");
            writer.newLine();
            writer.write("1. Keep the key names (left side of \"=\") completely unchanged. Only modify the values on the right.");
            writer.newLine();
            writer.write("2. After translation, change TRANSLATION_STATUS below from \"UNTRANSLATED\" to \"TRANSLATED\".");
            writer.newLine();
            writer.write("3. Do NOT modify any other markers (### XXX ###) or metadata in this file.");
            writer.newLine();
            writer.write("4. Output must remain as plain text, preserving the original structure.");
            writer.newLine();
            writer.write("5. Each mod section is marked as ### [modid]|[source_lang] ### — translate entries in that section accordingly.");
            writer.newLine();
            writer.write("6. Preserve formatting codes: %s, %d, §a, §6, \\n");
            writer.newLine();
            writer.write("7. Wrap your entire output inside a single code block (```), so the player can copy it easily.");
            writer.newLine();
            writer.newLine();

            writer.write("### TRANSLATION_STATUS ###");
            writer.newLine();
            writer.write("UNTRANSLATED");
            writer.newLine();
            writer.newLine();

            String currentModId = null;

            for (AiFileEntry entry : entries) {
                if (!entry.modId.equals(currentModId)) {
                    currentModId = entry.modId;
                    writer.write("### [" + entry.modId + "]|" + entry.sourceLang + " ###");
                    writer.newLine();
                    writer.newLine();
                }

                writer.write(entry.key + " = " + escapeValue(entry.sourceValue));
                writer.newLine();
            }
        }
    }

    /**
     * 估算条目大小
     */
    private int estimateEntrySize(AiFileEntry entry) {
        return entry.modId.length() + entry.sourceLang.length() +
               entry.key.length() + entry.sourceValue.length() + 30;
    }

    /**
     * 转义值中的特殊字符
     */
    private String escapeValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\"", "\\\"");
    }

    /**
     * 创建空白的 AItranslationResult 文件（若不存在则创建，已存在则保留）
     */
    private void createEmptyResultFileIfNotExists(String targetLanguage) throws IOException {
        Path resultFile = pathHelper.getAiResultFile(targetLanguage);
        if (!Files.exists(resultFile)) {
            Path parentDir = resultFile.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            Files.write(resultFile, new byte[0]);
            logger.info("Created empty AItranslationResult_" + targetLanguage + ".txt");
        }
    }

    /**
     * 归档当前语言的旧 AI 翻译文件
     * 注意：TRANSLATED 状态的文件不归档（用户已粘贴翻译结果，保留给 AI-manual 处理）
     */
    private void archiveOldFiles(String targetLanguage) throws IOException {
        Path mmtDir = pathHelper.getMmtDir();
        Path archiveDir = pathHelper.getArchiveAiDir();

        if (!Files.exists(mmtDir)) {
            return;
        }

        Files.createDirectories(archiveDir);

        AiFileParser parser = new AiFileParser(logger);
        String timestamp = LocalDateTime.now().format(ARCHIVE_FORMAT);
        int count = 0;
        int kept = 0;

        try (var stream = Files.newDirectoryStream(mmtDir, "AItranslation_" + targetLanguage + "_*.txt")) {
            for (Path file : stream) {
                AiTranslationStatus status = parser.parseStatus(file);
                if (status == AiTranslationStatus.TRANSLATED) {
                    kept++;
                    continue;
                }

                String fileName = file.getFileName().toString();
                Path dest = archiveDir.resolve(timestamp + "_" + fileName);
                Files.move(file, dest, StandardCopyOption.REPLACE_EXISTING);
                count++;
            }
        }

        if (count > 0) {
            logger.info("Archived " + count + " old AItranslation files for " + targetLanguage);
        }
        if (kept > 0) {
            logger.info("Kept " + kept + " translated AItranslation file(s) for " + targetLanguage);
        }
    }

    /**
     * 查找下一个可用的文件编号（跳过已存在的文件）
     */
    private int findNextFileNumber(String targetLanguage) throws IOException {
        int max = -1;
        Path mmtDir = pathHelper.getMmtDir();
        if (!Files.exists(mmtDir)) {
            return 0;
        }
        try (var stream = Files.newDirectoryStream(mmtDir, "AItranslation_" + targetLanguage + "_*.txt")) {
            for (Path file : stream) {
                Matcher m = AI_FILE_PATTERN.matcher(file.getFileName().toString());
                if (m.matches()) {
                    int num = Integer.parseInt(m.group(2));
                    if (num > max) {
                        max = num;
                    }
                }
            }
        }
        return max + 1;
    }

    private ExtractedData loadExtractedData() {
        Path path = pathHelper.getExtractedPath();
        return JsonUtil.readFromFile(path, ExtractedData.class);
    }

    private TranslatedData loadTranslatedData() {
        Path path = pathHelper.getTranslatedPath();
        return JsonUtil.readFromFile(path, TranslatedData.class);
    }

    /**
     * AI 文件条目数据结构
     */
    private static class AiFileEntry {
        final String modId;
        final String sourceLang;
        final String key;
        final String sourceValue;

        AiFileEntry(String modId, String sourceLang, String key, String sourceValue) {
            this.modId = modId;
            this.sourceLang = sourceLang;
            this.key = key;
            this.sourceValue = sourceValue;
        }
    }

    /**
     * 模组分组数据结构
     */
    private static class ModGroup {
        final String modId;
        final String sourceLang;
        final List<AiFileEntry> entries;
        final int size;

        ModGroup(String modId, String sourceLang, List<AiFileEntry> entries, int size) {
            this.modId = modId;
            this.sourceLang = sourceLang;
            this.entries = entries;
            this.size = size;
        }
    }
}
