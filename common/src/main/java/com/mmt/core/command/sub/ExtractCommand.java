package com.mmt.core.command.sub;

import com.mmt.core.command.CommandContext;
import com.mmt.core.command.ICommand;
import com.mmt.core.util.LangUtil;

import java.util.Map;

public class ExtractCommand implements ICommand {
    @Override
    public String getName() {
        return "extract";
    }

    @Override
    public String getDescription() {
        return "Extract mod language files to extracted.json";
    }

    @Override
    public String getUsage() {
        return "/mmt extract [targetLanguage] [broadScope] [listMode] [modList] [mode]";
    }

    @Override
    public String execute(CommandContext context, String[] args, String playerName) {
        String targetLang = context.getConfigManager().getTargetLanguage(null);

        int argIndex = 0;
        if (args.length > argIndex) {
            targetLang = LangUtil.normalize(args[argIndex++]);
        }
        if (args.length > argIndex) {
            context.getConfigManager().setString("extract_broad_scope", args[argIndex++]);
        }
        if (args.length > argIndex) {
            context.getConfigManager().setString("extract_list_mode", args[argIndex++]);
        }
        if (args.length > argIndex) {
            context.getConfigManager().setString("extract_list", args[argIndex++]);
        }
        if (args.length > argIndex) {
            context.getConfigManager().setString("extract_mode", args[argIndex]);
        }

        boolean success = context.getExtractor().extract(targetLang);

        if (success) {
            Map<String, Object> stats = context.getExtractor().getLastStats();
            int processedMods = (int) stats.getOrDefault("processed_mods", 0);
            int processedKeys = (int) stats.getOrDefault("processed_keys", 0);
            int totalMods = (int) stats.getOrDefault("total_mods", 0);
            int totalKeys = (int) stats.getOrDefault("total_keys", 0);
            int dictHits = (int) stats.getOrDefault("dict_hits", 0);

            return context.t("mmt.prefix") + " "
                    + context.t("mmt.command.extract.success") + " "
                    + context.t("mmt.command.extract.stats", processedMods, processedKeys, totalMods, totalKeys, dictHits);
        } else {
            return context.t("mmt.prefix") + " " + context.t("mmt.command.extract.failed");
        }
    }

    @Override
    public String[] getTabCompletions(String[] args) {
        if (args.length == 1) {
            return new String[]{"zh_cn", "en_us", "ja_jp", "ru_ru", "ko_kr", "fr_fr", "de_de", "es_es", "pt_br", "it_it"};
        }
        if (args.length == 2) {
            return new String[]{"new", "all"};
        }
        if (args.length == 3) {
            return new String[]{"blacklist", "whitelist"};
        }
        if (args.length == 5) {
            return new String[]{"diff", "diff_same", "full"};
        }
        return new String[0];
    }
}