package com.mmt.core.data.model.extracted;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * extracted.json 顶层数据模型
 * 顶层键为目标语言代码，值为 Map<modId, ExtractedModData>
 */
public class ExtractedData extends LinkedHashMap<String, Map<String, ExtractedModData>> {
    public Map<String, ExtractedModData> getMods(String language) {
        return computeIfAbsent(language, k -> new LinkedHashMap<>());
    }

    public ExtractedModData getMod(String language, String modId) {
        Map<String, ExtractedModData> mods = get(language);
        return mods != null ? mods.get(modId) : null;
    }

    public void putMod(String language, String modId, ExtractedModData data) {
        getMods(language).put(modId, data);
    }

    public boolean containsMod(String language, String modId) {
        Map<String, ExtractedModData> mods = get(language);
        return mods != null && mods.containsKey(modId);
    }

    public void removeMod(String language, String modId) {
        Map<String, ExtractedModData> mods = get(language);
        if (mods != null) {
            mods.remove(modId);
        }
    }

    public int getModCount(String language) {
        Map<String, ExtractedModData> mods = get(language);
        return mods != null ? mods.size() : 0;
    }
}
