package com.mmt.core.command.sub;

import com.mmt.core.command.CommandContext;
import com.mmt.core.command.ICommand;
import com.mmt.core.util.LangUtil;

import java.util.Map;

public class PackCommand implements ICommand {
    @Override
    public String getName() {
        return "pack";
    }

    @Override
    public String getDescription() {
        return "Pack translation results into resource pack";
    }

    @Override
    public String getUsage() {
        return "/mmt pack [targetLanguage]";
    }

    @Override
    public String execute(CommandContext context, String[] args, String playerName) {
        boolean success;

        if (args.length > 0) {
            String targetLang = LangUtil.normalize(args[0]);
            success = context.getPacker().pack(targetLang, context.getMcVersion());
        } else {
            success = context.getPacker().packAll(context.getMcVersion());
        }

        if (success) {
            Map<String, Object> stats = context.getPacker().getLastStats();
            int modCount = (int) stats.getOrDefault("mod_count", 0);
            int totalKeys = (int) stats.getOrDefault("total_keys", 0);
            int userTranslated = (int) stats.getOrDefault("user_translated", 0);
            int overridden = (int) stats.getOrDefault("overridden", 0);

            return context.t("mmt.prefix") + " "
                    + context.t("mmt.command.pack.success") + " "
                    + context.t("mmt.command.pack.stats", modCount, totalKeys, userTranslated, overridden);
        } else {
            return context.t("mmt.prefix") + " " + context.t("mmt.command.pack.failed");
        }
    }

    @Override
    public String[] getTabCompletions(String[] args) {
        if (args.length == 1) {
            return new String[]{"zh_cn", "en_us", "ja_jp", "ru_ru", "ko_kr", "fr_fr", "de_de", "es_es", "pt_br", "it_it"};
        }
        return new String[0];
    }
}