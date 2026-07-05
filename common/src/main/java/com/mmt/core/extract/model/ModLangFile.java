package com.mmt.core.extract.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 模组语言文件数据结构
 * 表示一个模组的一个语言文件内容
 */
public class ModLangFile {
    private String modId;
    private String namespace;
    private String language;
    private String filePath;
    private LangFormat format;
    private final Map<String, String> entries = new HashMap<>();
    private String contentHash;

    public ModLangFile() {
        this.format = LangFormat.JSON;
    }

    public ModLangFile(String modId, String namespace, String language, String filePath) {
        this.modId = modId;
        this.namespace = namespace;
        this.language = language;
        this.filePath = filePath;
        this.format = LangFormat.fromPath(filePath);
    }

    public ModLangFile(String modId, String namespace, String language, String filePath, LangFormat format) {
        this.modId = modId;
        this.namespace = namespace;
        this.language = language;
        this.filePath = filePath;
        this.format = format;
    }

    public String getModId() {
        return modId;
    }

    public void setModId(String modId) {
        this.modId = modId;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public LangFormat getFormat() {
        return format;
    }

    public void setFormat(LangFormat format) {
        this.format = format;
    }

    public Map<String, String> getEntries() {
        return entries;
    }

    public String getEntry(String key) {
        return entries.get(key);
    }

    public void putEntry(String key, String value) {
        entries.put(key, value);
    }

    public boolean containsEntry(String key) {
        return entries.containsKey(key);
    }

    public int getEntryCount() {
        return entries.size();
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }
}