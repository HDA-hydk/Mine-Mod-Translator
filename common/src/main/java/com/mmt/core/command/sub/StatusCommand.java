package com.mmt.core.command.sub;

import com.mmt.core.command.CommandContext;
import com.mmt.core.command.ICommand;
import com.mmt.core.data.model.extracted.ExtractedData;
import com.mmt.core.data.model.extracted.ExtractedModData;
import com.mmt.core.data.model.packed.PackedData;
import com.mmt.core.data.model.packed.PackedEntry;
import com.mmt.core.data.model.packed.PackedModData;
import com.mmt.core.data.model.translated.TranslatedData;
import com.mmt.core.data.model.translated.TranslatedEntry;
import com.mmt.core.data.model.translated.TranslatedModData;
import com.mmt.core.data.json.JsonUtil;
import com.mmt.core.data.path.PathHelper;
import com.mmt.core.util.LangUtil;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class StatusCommand implements ICommand {
    @Override
    public String getName() {
        return "status";
    }

    @Override
    public String getDescription() {
        return "Show status summary";
    }

    @Override
    public String getUsage() {
        return "/mmt status [targetLanguage]";
    }

    @Override
    public String execute(CommandContext context, String[] args, String playerName) {
        String targetLang = context.getConfigManager().getTargetLanguage(null);

        if (args.length > 0) {
            targetLang = LangUtil.normalize(args[0]);
        }

        PathHelper pathHelper = context.getPathHelper();

        ExtractedData extractedData = JsonUtil.readFromFile(pathHelper.getExtractedPath(), ExtractedData.class);
        TranslatedData translatedData = JsonUtil.readFromFile(pathHelper.getTranslatedPath(), TranslatedData.class);
        PackedData packedData = JsonUtil.readFromFile(pathHelper.getPackedPath(), PackedData.class);

        int extractedCount = 0;
        Map<String, ExtractedModData> extractedMods = extractedData.get(targetLang);
        if (extractedMods != null) {
            for (ExtractedModData mod : extractedMods.values()) {
                extractedCount += mod.getEntries().size();
            }
        }

        int translatedCount = 0;
        Map<String, Integer> methodCounts = new HashMap<>();
        Map<String, TranslatedModData> translatedMods = translatedData.get(targetLang);
        if (translatedMods != null) {
            for (TranslatedModData mod : translatedMods.values()) {
                for (TranslatedEntry entry : mod.getEntries().values()) {
                    translatedCount++;
                    String method = entry.getMethod();
                    methodCounts.merge(method, 1, Integer::sum);
                }
            }
        }

        int packedCount = 0;
        int userTranslatedCount = 0;
        String lastPackDate = "";
        Map<String, PackedModData> packedMods = packedData.get(targetLang);
        if (packedMods != null) {
            for (PackedModData mod : packedMods.values()) {
                if (!mod.getPackDate().isEmpty()) {
                    lastPackDate = mod.getPackDate();
                }
                for (PackedEntry entry : mod.getEntries().values()) {
                    packedCount++;
                    if ("user-translated".equals(entry.getMethod())) {
                        userTranslatedCount++;
                    }
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(context.t("mmt.prefix")).append(" ").append(context.t("mmt.command.status.header", targetLang)).append("\n");
        sb.append("  ").append(context.t("mmt.command.status.extracted", extractedCount)).append("\n");
        sb.append("  ").append(context.t("mmt.command.status.translated", translatedCount)).append("\n");

        for (Map.Entry<String, Integer> entry : methodCounts.entrySet()) {
            sb.append(String.format("    - %s: %d\n", entry.getKey(), entry.getValue()));
        }

        sb.append("  ").append(context.t("mmt.command.status.packed", packedCount)).append("\n");
        sb.append("  ").append(context.t("mmt.command.status.user_translated", userTranslatedCount)).append("\n");

        if (!lastPackDate.isEmpty()) {
            sb.append("  ").append(context.t("mmt.command.status.last_pack", lastPackDate)).append("\n");
        }

        return sb.toString();
    }

    @Override
    public String[] getTabCompletions(String[] args) {
        if (args.length == 1) {
            return new String[]{"zh_cn", "en_us", "ja_jp", "ru_ru", "ko_kr", "fr_fr", "de_de", "es_es", "pt_br", "it_it"};
        }
        return new String[0];
    }
}