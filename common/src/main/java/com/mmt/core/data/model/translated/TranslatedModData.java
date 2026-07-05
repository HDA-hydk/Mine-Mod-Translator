package com.mmt.core.data.model.translated;

import java.util.HashMap;
import java.util.Map;

/**
 * translated.json 模组级数据模型
 */
public class TranslatedModData {
    private String sourceLang;
    private String translateDate;
    private final Map<String, TranslatedEntry> entries = new HashMap<>();

    public TranslatedModData() {
    }

    public TranslatedModData(String sourceLang, String translateDate) {
        this.sourceLang = sourceLang;
        this.translateDate = translateDate;
    }

    public String getSourceLang() {
        return sourceLang;
    }

    public void setSourceLang(String sourceLang) {
        this.sourceLang = sourceLang;
    }

    public String getTranslateDate() {
        return translateDate;
    }

    public void setTranslateDate(String translateDate) {
        this.translateDate = translateDate;
    }

    public Map<String, TranslatedEntry> getEntries() {
        return entries;
    }

    public TranslatedEntry getEntry(String key) {
        return entries.get(key);
    }

    public void putEntry(String key, TranslatedEntry entry) {
        entries.put(key, entry);
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