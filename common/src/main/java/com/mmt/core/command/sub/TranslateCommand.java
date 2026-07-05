package com.mmt.core.command.sub;

import com.mmt.core.command.CommandContext;
import com.mmt.core.command.ICommand;
import com.mmt.core.util.LangUtil;

import java.util.Map;

public class TranslateCommand implements ICommand {
    @Override
    public String getName() {
        return "translate";
    }

    @Override
    public String getDescription() {
        return "Execute translation workflow";
    }

    @Override
    public String getUsage() {
        return "/mmt translate [targetLanguage] [method]";
    }

    @SuppressWarnings("unchecked")
    @Override
    public String execute(CommandContext context, String[] args, String playerName) {
        boolean success;

        if (args.length > 0) {
            String targetLang = LangUtil.normalize(args[0]);
            success = context.getTranslator().translate(targetLang);
        } else {
            success = context.getTranslator().translateAll();
        }

        if (success) {
            Map<String, Object> stats = context.getTranslator().getLastStats();
            int modCount = (int) stats.getOrDefault("mod_count", 0);
            int totalKeys = (int) stats.getOrDefault("total_keys", 0);
            int translatedKeys = (int) stats.getOrDefault("translated_keys", 0);
            int failedKeys = (int) stats.getOrDefault("failed_keys", 0);

            Map<String, Integer> methodHits = (Map<String, Integer>) stats.get("method_hits");
            StringBuilder methodStr = new StringBuilder();
            if (methodHits != null && !methodHits.isEmpty()) {
                boolean first = true;
                for (Map.Entry<String, Integer> entry : methodHits.entrySet()) {
                    if (!first) methodStr.append(", ");
                    methodStr.append(entry.getKey()).append(": ").append(entry.getValue());
                    first = false;
                }
            }

            return context.t("mmt.prefix") + " "
                    + context.t("mmt.command.translate.success") + " "
                    + context.t("mmt.command.translate.stats",
                            modCount, totalKeys, translatedKeys, failedKeys,
                            methodStr.length() > 0 ? methodStr.toString() : "none");
        } else {
            return context.t("mmt.prefix") + " " + context.t("mmt.command.translate.failed");
        }
    }

    @Override
    public String[] getTabCompletions(String[] args) {
        if (args.length == 1) {
            return new String[]{"zh_cn", "en_us", "ja_jp", "ru_ru", "ko_kr", "fr_fr", "de_de", "es_es", "pt_br", "it_it"};
        }
        if (args.length == 2) {
            return new String[]{"I18n-dict", "AI-manual", "AI-auto", "MT"};
        }
        return new String[0];
    }
}