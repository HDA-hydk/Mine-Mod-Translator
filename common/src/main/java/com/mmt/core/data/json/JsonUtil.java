package com.mmt.core.data.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mmt.core.log.MmtLogger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JSON 序列化/反序列化工具类
 * 使用 Gson 库（Forge/Fabric 都内置）
 */
public class JsonUtil {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /**
     * 从文件读取 JSON 并反序列化为对象
     * @param path 文件路径
     * @param clazz 对象类型
     * @param <T> 对象泛型
     * @return 反序列化后的对象，文件不存在或格式错误时返回空对象
     */
    public static <T> T readFromFile(Path path, Class<T> clazz) {
        if (!Files.exists(path)) {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                MmtLogger logger = MmtLogger.getInstance();
                if (logger != null) {
                    logger.warn("Failed to create empty instance for " + clazz.getName());
                }
                return null;
            }
        }

        try (Reader reader = new InputStreamReader(
                new FileInputStream(path.toFile()), StandardCharsets.UTF_8)) {
            T result = GSON.fromJson(reader, clazz);
            if (result == null) {
                return clazz.getDeclaredConstructor().newInstance();
            }
            return result;
        } catch (FileNotFoundException e) {
            MmtLogger logger = MmtLogger.getInstance();
            if (logger != null) {
                logger.warn("JSON file not found: " + path);
            }
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                return null;
            }
        } catch (Exception e) {
            MmtLogger logger = MmtLogger.getInstance();
            if (logger != null) {
                logger.warn("Failed to parse JSON file: " + path + ", error: " + e.getMessage());
            }
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    /**
     * 将对象序列化为 JSON 并写入文件
     * @param obj 对象
     * @param path 文件路径
     * @param <T> 对象泛型
     */
    public static <T> void writeToFile(T obj, Path path) {
        try {
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            try (Writer writer = new OutputStreamWriter(
                    new FileOutputStream(path.toFile()), StandardCharsets.UTF_8)) {
                GSON.toJson(obj, writer);
            }
        } catch (Exception e) {
            MmtLogger logger = MmtLogger.getInstance();
            if (logger != null) {
                logger.warn("Failed to write JSON file: " + path + ", error: " + e.getMessage());
            }
        }
    }

    /**
     * 将对象序列化为 JSON 字符串
     * @param obj 对象
     * @param <T> 对象泛型
     * @return JSON 字符串
     */
    public static <T> String toJson(T obj) {
        return GSON.toJson(obj);
    }

    /**
     * 从 JSON 字符串反序列化对象
     * @param json JSON 字符串
     * @param clazz 对象类型
     * @param <T> 对象泛型
     * @return 反序列化后的对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    /**
     * 获取 Gson 实例（供高级用法）
     * @return Gson 实例
     */
    public static Gson getGson() {
        return GSON;
    }
}