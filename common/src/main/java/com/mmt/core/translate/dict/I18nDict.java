package com.mmt.core.translate.dict;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mmt.core.data.path.PathHelper;
import com.mmt.core.log.MmtLogger;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class I18nDict {
    private static I18nDict instance;

    private final Map<String, String> normalizedDict = new HashMap<>();
    private final Map<String, List<String>> rawDict = new HashMap<>();
    private boolean loaded = false;
    private boolean loadAttempted = false;
    private final MmtLogger logger;
    private PathHelper pathHelper;
    private int loadedFileCount = 0;

    private I18nDict(MmtLogger logger) {
        this.logger = logger;
    }

    public static synchronized I18nDict getInstance(MmtLogger logger) {
        if (instance == null) {
            instance = new I18nDict(logger);
        }
        return instance;
    }

    public void setPathHelper(PathHelper pathHelper) {
        this.pathHelper = pathHelper;
    }

    public void load() {
        if (loadAttempted) {
            return;
        }
        loadAttempted = true;

        if (pathHelper == null) {
            logger.warn("PathHelper not set, cannot load i18n-dict");
            return;
        }

        Path dictDir = pathHelper.getI18nDicDir();
        pathHelper.ensureDirExists(dictDir);

        if (!Files.exists(dictDir)) {
            logger.warn("i18n-dic directory does not exist: " + dictDir);
            return;
        }

        try (Stream<Path> paths = Files.list(dictDir)) {
            List<Path> jsonFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".json"))
                    .sorted()
                    .toList();

            if (jsonFiles.isEmpty()) {
                logger.info("No dictionary files found in " + dictDir);
                return;
            }

            Gson gson = new Gson();
            for (Path jsonFile : jsonFiles) {
                try (BufferedReader reader = Files.newBufferedReader(jsonFile, StandardCharsets.UTF_8)) {
                    Map<String, List<String>> fileDict = gson.fromJson(reader, new TypeToken<Map<String, List<String>>>() {}.getType());
                    if (fileDict != null) {
                        rawDict.putAll(fileDict);
                        loadedFileCount++;
                        logger.info("Loaded dictionary file: " + jsonFile.getFileName() + " (" + fileDict.size() + " entries)");
                    }
                } catch (Exception e) {
                    logger.error("Failed to load dictionary file: " + jsonFile.getFileName(), e);
                }
            }

            for (Map.Entry<String, List<String>> entry : rawDict.entrySet()) {
                String key = normalizeKey(entry.getKey());
                if (!key.isEmpty() && entry.getValue() != null && !entry.getValue().isEmpty()) {
                    normalizedDict.put(key, entry.getValue().get(0));
                }
            }

            loaded = true;
            logger.info("I18n-dict loaded: " + loadedFileCount + " files, " + normalizedDict.size() + " total entries");
        } catch (Exception e) {
            logger.error("Failed to load I18n-dict", e);
        }
    }

    public void reload() {
        clear();
        load();
    }

    public String lookup(String sourceValue) {
        if (!loaded) {
            return null;
        }
        if (sourceValue == null || sourceValue.isEmpty()) {
            return null;
        }
        String normalized = normalizeKey(sourceValue);
        return normalizedDict.get(normalized);
    }

    private String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        return key.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    public boolean isLoaded() {
        return loaded;
    }

    public int size() {
        return normalizedDict.size();
    }

    public int getLoadedFileCount() {
        return loadedFileCount;
    }

    public void clear() {
        normalizedDict.clear();
        rawDict.clear();
        loaded = false;
        loadAttempted = false;
        loadedFileCount = 0;
    }
}
