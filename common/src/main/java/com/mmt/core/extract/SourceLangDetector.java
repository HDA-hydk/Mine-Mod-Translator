package com.mmt.core.extract;

import com.mmt.core.extract.model.ModLangFile;
import com.mmt.core.log.MmtLogger;
import com.mmt.core.util.LangUtil;

import java.util.*;

/**
 * 源语言判定器
 * 根据模组的语言文件键数量判定源语言
 */
public class SourceLangDetector {
    private static final String EN_US = "en_us";

    /**
     * 判定模组的源语言
     * @param langFiles 模组的所有语言文件
     * @return 源语言代码
     */
    public String detect(List<ModLangFile> langFiles) {
        return detect(langFiles, null);
    }

    /**
     * 判定模组的源语言（排除指定语言）
     * @param langFiles 模组的所有语言文件
     * @param excludeLang 要排除的语言代码（通常是目标语言），可为 null
     * @return 源语言代码，如果目标语言在最大键数量列表中则返回 null（表示应跳过该模组）
     */
    public String detect(List<ModLangFile> langFiles, String excludeLang) {
        if (langFiles == null || langFiles.isEmpty()) {
            return EN_US;
        }

        Map<String, Integer> langKeyCounts = new HashMap<>();

        for (ModLangFile langFile : langFiles) {
            String lang = LangUtil.normalize(langFile.getLanguage());
            int count = langFile.getEntryCount();
            langKeyCounts.merge(lang, count, Integer::sum);
        }

        String normalizedExclude = null;
        if (excludeLang != null && !excludeLang.isEmpty()) {
            normalizedExclude = LangUtil.normalize(excludeLang);

            // 检查目标语言是否在最大键数量列表中，如果是则跳过该模组
            if (langKeyCounts.containsKey(normalizedExclude)) {
                int maxCount = Collections.max(langKeyCounts.values());
                if (langKeyCounts.get(normalizedExclude) == maxCount) {
                    MmtLogger logger = MmtLogger.getInstance();
                    if (logger != null) {
                        logger.info("Target language " + normalizedExclude +
                                " is in max key count list, skipping mod");
                    }
                    return null;
                }
            }

            langKeyCounts.remove(normalizedExclude);
        }

        return determineSourceLang(langKeyCounts, normalizedExclude);
    }

    /**
     * 根据键数量判定源语言
     * @param langKeyCounts 语言→键数量映射
     * @param targetLang 目标语言代码（已排除），用于选择变体优先级，可为 null
     */
    private String determineSourceLang(Map<String, Integer> langKeyCounts, String targetLang) {
        if (langKeyCounts.isEmpty()) {
            return EN_US;
        }

        int maxCount = Collections.max(langKeyCounts.values());

        List<String> candidates = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : langKeyCounts.entrySet()) {
            if (entry.getValue() == maxCount) {
                candidates.add(entry.getKey());
            }
        }

        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        // 优先选择目标语言的变体（如目标zh_tw，候选有zh_cn时优先zh_cn）
        if (targetLang != null) {
            String targetPrefix = getLangPrefix(targetLang);
            if (targetPrefix != null) {
                List<String> variantMatches = new ArrayList<>();
                for (String candidate : candidates) {
                    String candidatePrefix = getLangPrefix(candidate);
                    if (targetPrefix.equals(candidatePrefix)) {
                        variantMatches.add(candidate);
                    }
                }
                if (!variantMatches.isEmpty()) {
                    Collections.sort(variantMatches);
                    String selected = variantMatches.get(0);
                    MmtLogger logger = MmtLogger.getInstance();
                    if (logger != null) {
                        logger.info("Multiple languages have the same max key count: " + candidates +
                                ", selected target language variant: " + selected);
                    }
                    return selected;
                }
            }
        }

        if (candidates.contains(EN_US)) {
            return EN_US;
        }

        Collections.sort(candidates);
        String selected = candidates.get(0);

        MmtLogger logger = MmtLogger.getInstance();
        if (logger != null) {
            logger.warn("Multiple languages have the same max key count: " + candidates +
                    ", selected by alphabetical order: " + selected);
        }

        return selected;
    }

    /**
     * 获取语言代码的前缀（下划线前的部分）
     * 如 zh_cn → zh, en_us → en, ja_jp → ja
     */
    private String getLangPrefix(String lang) {
        if (lang == null || lang.isEmpty()) {
            return null;
        }
        int idx = lang.indexOf('_');
        if (idx > 0) {
            return lang.substring(0, idx);
        }
        return lang;
    }

    /**
     * 获取模组所有语言文件的键数量统计
     * @param langFiles 模组的所有语言文件
     * @return 语言代码 → 键数量的映射
     */
    public Map<String, Integer> getLangKeyCounts(List<ModLangFile> langFiles) {
        Map<String, Integer> langKeyCounts = new HashMap<>();

        for (ModLangFile langFile : langFiles) {
            String lang = LangUtil.normalize(langFile.getLanguage());
            int count = langFile.getEntryCount();
            langKeyCounts.merge(lang, count, Integer::sum);
        }

        return langKeyCounts;
    }

    /**
     * 判断指定语言是否是模组的源语言
     * @param langFiles 模组的所有语言文件
     * @param language 待判断的语言代码
     * @return 是否是源语言
     */
    public boolean isSourceLang(List<ModLangFile> langFiles, String language) {
        String sourceLang = detect(langFiles);
        return sourceLang.equalsIgnoreCase(language);
    }
}