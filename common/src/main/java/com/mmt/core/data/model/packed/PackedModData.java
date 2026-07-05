package com.mmt.core.data.model.packed;

import com.mmt.core.extract.model.LangFormat;

import java.util.HashMap;
import java.util.Map;

/**
 * packed.json 模组级数据模型
 */
public class PackedModData {
    private String packDate;
    private LangFormat langFormat;
    private final Map<String, PackedEntry> entries = new HashMap<>();

    public PackedModData() {
        this.langFormat = LangFormat.JSON;
    }

    public PackedModData(String packDate) {
        this.packDate = packDate;
        this.langFormat = LangFormat.JSON;
    }

    public String getPackDate() {
        return packDate;
    }

    public void setPackDate(String packDate) {
        this.packDate = packDate;
    }

    public LangFormat getLangFormat() {
        return langFormat;
    }

    public void setLangFormat(LangFormat langFormat) {
        this.langFormat = langFormat;
    }

    public Map<String, PackedEntry> getEntries() {
        return entries;
    }

    public PackedEntry getEntry(String key) {
        return entries.get(key);
    }

    public void putEntry(String key, PackedEntry entry) {
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