package com.mmt.core.pack;

import com.mmt.core.data.json.JsonUtil;
import com.mmt.core.data.model.packed.PackedData;
import com.mmt.core.data.model.packed.PackedEntry;
import com.mmt.core.data.model.packed.PackedModData;
import com.mmt.core.data.model.translated.TranslatedEntry;
import com.mmt.core.data.model.translated.TranslatedModData;
import com.mmt.core.data.path.PathHelper;
import com.mmt.core.extract.model.LangFormat;
import com.mmt.core.log.MmtLogger;
import com.mmt.core.util.HashUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Stream;

public class ResourcePackGenerator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PathHelper pathHelper;
    private final MmtLogger logger;

    public ResourcePackGenerator(PathHelper pathHelper, MmtLogger logger) {
        this.pathHelper = pathHelper;
        this.logger = logger;
    }

    public void generate(String targetLanguage, Map<String, TranslatedModData> translatedMods,
                         PackedData packedData, String mcVersion, Map<String, LangFormat> modFormats) throws IOException {
        Path resourcePackDir = pathHelper.getResourcePackDir(targetLanguage);
        Files.createDirectories(resourcePackDir);

        generatePackMcmeta(resourcePackDir, targetLanguage, translatedMods, mcVersion);
        copyPackIcon(resourcePackDir);

        int totalKeys = 0;

        for (Map.Entry<String, TranslatedModData> modEntry : translatedMods.entrySet()) {
            String modId = modEntry.getKey();
            TranslatedModData translatedMod = modEntry.getValue();

            // 确定语言文件格式：优先使用传入的格式，否则回退JSON
            LangFormat format = modFormats != null ? modFormats.getOrDefault(modId, LangFormat.JSON) : LangFormat.JSON;

            Path langFile = resourcePackDir.resolve("assets").resolve(modId).resolve("lang")
                    .resolve(format.getFileName(targetLanguage));
            Files.createDirectories(langFile.getParent());

            Map<String, String> langContent = new HashMap<>();
            PackedModData packedMod = packedData.getMod(targetLanguage, modId);
            if (packedMod == null) {
                packedMod = new PackedModData();
                packedMod.setPackDate(LocalDateTime.now().format(DATE_FORMATTER));
                packedMod.setLangFormat(format);
                packedData.putMod(targetLanguage, modId, packedMod);
            } else {
                packedMod.setPackDate(LocalDateTime.now().format(DATE_FORMATTER));
                packedMod.setLangFormat(format);
            }

            for (Map.Entry<String, TranslatedEntry> entry : translatedMod.getEntries().entrySet()) {
                String key = entry.getKey();
                TranslatedEntry translatedEntry = entry.getValue();

                langContent.put(key, translatedEntry.getValue());

                PackedEntry packedEntry = new PackedEntry();
                packedEntry.setValue(translatedEntry.getValue());
                packedEntry.setMethod(translatedEntry.getMethod());
                packedEntry.setSourceValue(translatedEntry.getSourceValue());

                if ("user-translated".equals(translatedEntry.getMethod())) {
                    packedEntry.setValueHash("00000000");
                } else {
                    packedEntry.setValueHash(HashUtil.crc32(translatedEntry.getValue()));
                }

                packedMod.putEntry(key, packedEntry);
                totalKeys++;
            }

            writeLangFile(langFile, langContent, format);
        }

        logger.info("Resource pack generated: " + totalKeys + " keys across " + translatedMods.size() + " mods");
    }

    /**
     * 从 jar 内资源复制 pack.png 到资源包目录
     * 与模组 jar 封面共用同一张 logo 图
     */
    private void copyPackIcon(Path resourcePackDir) throws IOException {
        Path target = resourcePackDir.resolve("pack.png");
        try (InputStream is = getClass().getResourceAsStream("/pack.png")) {
            if (is == null) {
                logger.warn("pack.png not found in jar resources, skipping pack icon");
                return;
            }
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
            logger.debug("Copied pack.png to resource pack");
        }
    }

    private void generatePackMcmeta(Path resourcePackDir, String targetLanguage,
                                    Map<String, TranslatedModData> translatedMods, String mcVersion) throws IOException {
        int packFormat = PackFormat.getPackFormat(mcVersion, logger);
        int totalKeys = translatedMods.values().stream().mapToInt(m -> m.getEntries().size()).sum();
        String date = LocalDateTime.now().format(DATE_FORMATTER);

        Map<String, Object> pack = new HashMap<>();
        Map<String, Object> inner = new HashMap<>();
        inner.put("pack_format", packFormat);
        inner.put("description", String.format("Mine Mod Translator auto-translation pack. Target: %s. Updated: %s. Total keys: %d.",
                targetLanguage, date, totalKeys));
        pack.put("pack", inner);

        Path mcmetaFile = resourcePackDir.resolve("pack.mcmeta");
        JsonUtil.writeToFile(pack, mcmetaFile);

        logger.debug("Generated pack.mcmeta with format " + packFormat);
    }

    private void writeLangFile(Path langFile, Map<String, String> content, LangFormat format) throws IOException {
        Path tempFile = langFile.resolveSibling(langFile.getFileName() + ".tmp");

        if (format == LangFormat.PROPERTIES) {
            // 按key排序输出properties格式（.lang文件实际就是properties格式）
            TreeMap<String, String> sorted = new TreeMap<>(content);
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    Files.newOutputStream(tempFile), StandardCharsets.UTF_8)) {
                for (Map.Entry<String, String> entry : sorted.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue() != null ? entry.getValue() : "";
                    // 转义properties特殊字符
                    key = key.replace("\\", "\\\\").replace(":", "\\:").replace("=", "\\=");
                    value = value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
                    writer.write(key + "=" + value + "\n");
                }
            }
        } else {
            JsonUtil.writeToFile(content, tempFile);
        }

        if (Files.exists(tempFile)) {
            Files.deleteIfExists(langFile);
            Files.move(tempFile, langFile);
        }
    }

    public Map<String, Map<String, String>> loadExistingLangFiles(String targetLanguage) {
        Map<String, Map<String, String>> existingLangFiles = new HashMap<>();

        try {
            Path resourcePackDir = pathHelper.getResourcePackDir(targetLanguage);
            if (!Files.exists(resourcePackDir)) {
                return existingLangFiles;
            }

            Path assetsDir = resourcePackDir.resolve("assets");
            if (!Files.exists(assetsDir)) {
                return existingLangFiles;
            }

            try (Stream<Path> walk = Files.walk(assetsDir)) {
                String jsonName = targetLanguage + ".json";
                String langName = targetLanguage + ".lang";
                walk.filter(p -> p.getFileName().toString().equals(jsonName)
                                || p.getFileName().toString().equals(langName))
                        .forEach(p -> {
                            try {
                                String modId = p.getParent().getParent().getFileName().toString();
                                Map<String, String> langContent;
                                if (p.toString().endsWith(".lang")) {
                                    langContent = new HashMap<>();
                                    Properties props = new Properties();
                                    try (InputStreamReader reader = new InputStreamReader(
                                            Files.newInputStream(p), StandardCharsets.UTF_8)) {
                                        props.load(reader);
                                    }
                                    for (String key : props.stringPropertyNames()) {
                                        langContent.put(key, props.getProperty(key));
                                    }
                                } else {
                                    langContent = JsonUtil.readFromFile(p, Map.class);
                                }
                                if (langContent != null && !langContent.isEmpty()) {
                                    existingLangFiles.put(modId, langContent);
                                }
                            } catch (Exception e) {
                                logger.warn("Failed to load existing lang file: " + p);
                            }
                        });
            }
        } catch (Exception e) {
            logger.warn("Failed to load existing lang files", e);
        }

        return existingLangFiles;
    }
}