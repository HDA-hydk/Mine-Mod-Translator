package com.mmt.core.data.model.extracted;

import com.mmt.core.extract.model.LangFormat;

import java.util.HashMap;
import java.util.Map;

/**
 * extracted.json 模组级数据模型
 */
public class ExtractedModData {
    private String sourceLang;
    private String extractDate;
    private String modFileHash;
    private LangFormat langFormat;
    private final Map<String, String> entries = new HashMap<>();

    public ExtractedModData() {
        this.langFormat = LangFormat.JSON;
    }

    public ExtractedModData(String sourceLang, String extractDate, String modFileHash) {
        this.sourceLang = sourceLang;
        this.extractDate = extractDate;
        this.modFileHash = modFileHash;
        this.langFormat = LangFormat.JSON;
    }

    public String getSourceLang() {
        return sourceLang;
    }

    public void setSourceLang(String sourceLang) {
        this.sourceLang = sourceLang;
    }

    public String getExtractDate() {
        return extractDate;
    }

    public void setExtractDate(String extractDate) {
        this.extractDate = extractDate;
    }

    public String getModFileHash() {
        return modFileHash;
    }

    public void setModFileHash(String modFileHash) {
        this.modFileHash = modFileHash;
    }

    public LangFormat getLangFormat() {
        return langFormat;
    }

    public void setLangFormat(LangFormat langFormat) {
        this.langFormat = langFormat;
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

    public void removeEntry(String key) {
        entries.remove(key);
    }

    public int getEntryCount() {
        return entries.size();
    }
}