package com.mmt.core.data.model.translated;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * translated.json 顶层数据模型
 * 顶层键为目标语言代码，值为 Map<modId, TranslatedModData>
 */
public class TranslatedData extends LinkedHashMap<String, Map<String, TranslatedModData>> {
    public Map<String, TranslatedModData> getMods(String language) {
        return computeIfAbsent(language, k -> new LinkedHashMap<>());
    }

    public TranslatedModData getMod(String language, String modId) {
        Map<String, TranslatedModData> mods = get(language);
        return mods != null ? mods.get(modId) : null;
    }

    public void putMod(String language, String modId, TranslatedModData data) {
        getMods(language).put(modId, data);
    }

    public boolean containsMod(String language, String modId) {
        Map<String, TranslatedModData> mods = get(language);
        return mods != null && mods.containsKey(modId);
    }

    public void removeMod(String language, String modId) {
        Map<String, TranslatedModData> mods = get(language);
        if (mods != null) {
            mods.remove(modId);
        }
    }

    public int getModCount(String language) {
        Map<String, TranslatedModData> mods = get(language);
        return mods != null ? mods.size() : 0;
    }
}
