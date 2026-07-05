package com.mmt.core.data.model.packed;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * packed.json 顶层数据模型
 * 顶层键为目标语言代码，值为 Map<modId, PackedModData>
 */
public class PackedData extends LinkedHashMap<String, Map<String, PackedModData>> {
    public Map<String, PackedModData> getMods(String language) {
        return computeIfAbsent(language, k -> new LinkedHashMap<>());
    }

    public PackedModData getMod(String language, String modId) {
        Map<String, PackedModData> mods = get(language);
        return mods != null ? mods.get(modId) : null;
    }

    public void putMod(String language, String modId, PackedModData data) {
        getMods(language).put(modId, data);
    }

    public boolean containsMod(String language, String modId) {
        Map<String, PackedModData> mods = get(language);
        return mods != null && mods.containsKey(modId);
    }

    public void removeMod(String language, String modId) {
        Map<String, PackedModData> mods = get(language);
        if (mods != null) {
            mods.remove(modId);
        }
    }

    public int getModCount(String language) {
        Map<String, PackedModData> mods = get(language);
        return mods != null ? mods.size() : 0;
    }
}
