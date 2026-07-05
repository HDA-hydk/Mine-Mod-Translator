package com.mmt.core.command.sub;

import com.mmt.core.command.CommandContext;
import com.mmt.core.command.ICommand;
import com.mmt.core.util.LangUtil;

public class AutoCommand implements ICommand {
    @Override
    public String getName() {
        return "auto";
    }

    @Override
    public String getDescription() {
        return "Run the full pipeline (extract -> translate -> pack)";
    }

    @Override
    public String getUsage() {
        return "/mmt auto [targetLanguage]";
    }

    @Override
    public String execute(CommandContext context, String[] args, String playerName) {
        if (context.getAutoPipeline().isRunning()) {
            return context.t("mmt.prefix") + " " + context.t("mmt.command.auto.already_running");
        }

        String targetLang = context.getConfigManager().getTargetLanguage(null);
        if (args.length > 0) {
            targetLang = LangUtil.normalize(args[0]);
        }

        context.getAutoPipeline().runManually(targetLang);

        return context.t("mmt.prefix") + " " + context.t("mmt.command.auto.started", targetLang);
    }

    @Override
    public String[] getTabCompletions(String[] args) {
        if (args.length == 1) {
            return new String[]{"zh_cn", "en_us", "ja_jp", "ru_ru", "ko_kr", "fr_fr", "de_de", "es_es", "pt_br", "it_it"};
        }
        return new String[0];
    }
}
