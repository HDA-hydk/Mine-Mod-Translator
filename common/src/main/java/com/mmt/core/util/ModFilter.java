package com.mmt.core.util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 候选模组筛选工具类
 * 根据配置参数筛选需要处理的模组
 */
public final class ModFilter {
    private ModFilter() {
        throw new AssertionError("ModFilter 类不应被实例化");
    }

    /**
     * 解析模组列表字符串
     * @param listStr 逗号分隔的模组列表字符串
     * @return 模组 ID/文件名集合
     */
    public static Set<String> parseModList(String listStr) {
        if (listStr == null || listStr.isEmpty()) {
            return Collections.emptySet();
        }

        return Arrays.stream(listStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * 筛选候选模组
     * @param loadedMods 已加载模组列表（包含 modId 和模组文件名）
     * @param broadScope 大范围模式："new"（仅新模组）或 "all"（所有模组）
     * @param listMode 名单模式："blacklist"（排除）或 "whitelist"（仅包含）
     * @param modList 名单列表（逗号分隔）
     * @param existingMods 已存在于 extracted.json 中的模组列表（用于 broadScope=new）
     * @return 筛选后的候选模组列表
     */
    public static List<String> filter(List<String> loadedMods, String broadScope,
                                       String listMode, String modList, Set<String> existingMods) {
        Set<String> modListSet = parseModList(modList);

        List<String> candidates = new ArrayList<>();

        for (String mod : loadedMods) {
            if (mod == null || mod.isEmpty()) {
                continue;
            }

            boolean inExisting = existingMods != null && existingMods.contains(mod);

            if ("new".equalsIgnoreCase(broadScope) && inExisting) {
                continue;
            }

            if (!modListSet.isEmpty()) {
                boolean inList = modListSet.contains(mod) ||
                               modListSet.contains(getModIdFromFilename(mod));

                if ("blacklist".equalsIgnoreCase(listMode)) {
                    if (inList) {
                        continue;
                    }
                } else {
                    if (!inList) {
                        continue;
                    }
                }
            }

            candidates.add(mod);
        }

        return candidates;
    }

    /**
     * 从模组文件名中提取 modId
     * @param filename 模组文件名（如 "mymod-1.0.jar"）
     * @return modId（如 "mymod"）
     */
    public static String getModIdFromFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }

        int dotIndex = filename.indexOf('.');
        if (dotIndex > 0) {
            filename = filename.substring(0, dotIndex);
        }

        int dashIndex = filename.indexOf('-');
        if (dashIndex > 0) {
            return filename.substring(0, dashIndex);
        }

        return filename;
    }

    /**
     * 双模匹配
     * 检查模组是否匹配列表中的任一模式（modId 或文件名）
     * @param mod 模组名（modId 或文件名）
     * @param modList 模组列表
     * @return 是否匹配
     */
    public static boolean matches(String mod, Set<String> modList) {
        if (mod == null || mod.isEmpty() || modList == null || modList.isEmpty()) {
            return false;
        }

        if (modList.contains(mod)) {
            return true;
        }

        String modId = getModIdFromFilename(mod);
        return modList.contains(modId);
    }

    /**
     * 应用黑白名单筛选
     * @param candidates 候选模组列表
     * @param listMode 名单模式："blacklist" 或 "whitelist"
     * @param modList 名单列表
     * @return 筛选后的列表
     */
    public static List<String> applyListFilter(List<String> candidates, String listMode, Set<String> modList) {
        if (modList == null || modList.isEmpty()) {
            return new ArrayList<>(candidates);
        }

        List<String> result = new ArrayList<>();

        for (String candidate : candidates) {
            boolean inList = matches(candidate, modList);

            if ("blacklist".equalsIgnoreCase(listMode)) {
                if (!inList) {
                    result.add(candidate);
                }
            } else {
                if (inList) {
                    result.add(candidate);
                }
            }
        }

        return result;
    }
}