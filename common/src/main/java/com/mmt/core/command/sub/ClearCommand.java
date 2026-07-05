package com.mmt.core.command.sub;

import com.mmt.core.command.CommandContext;
import com.mmt.core.command.ICommand;
import com.mmt.core.data.json.JsonUtil;
import com.mmt.core.data.model.extracted.ExtractedData;
import com.mmt.core.data.model.packed.PackedData;
import com.mmt.core.data.model.translated.TranslatedData;
import com.mmt.core.util.LangUtil;

import java.nio.file.Files;
import java.nio.file.Path;

public class ClearCommand implements ICommand {
    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public String getDescription() {
        return "Clear data";
    }

    @Override
    public String getUsage() {
        return "/mmt clear extracted|translated|packed|ai|all [targetLanguage]";
    }

    @Override
    public String execute(CommandContext context, String[] args, String playerName) {
        if (args.length == 0) {
            return context.t("mmt.prefix") + " " + context.t("mmt.command.clear.usage");
        }

        String subCommand = args[0].toLowerCase();
        String targetLang = context.getConfigManager().getTargetLanguage(null);

        if (args.length > 1) {
            targetLang = LangUtil.normalize(args[1]);
        }

        try {
            switch (subCommand) {
                case "extracted":
                    clearExtracted(context, targetLang);
                    return context.t("mmt.prefix") + " " + context.t("mmt.command.clear.cleared_extracted");

                case "translated":
                    clearTranslated(context, targetLang);
                    return context.t("mmt.prefix") + " " + context.t("mmt.command.clear.cleared_translated");

                case "packed":
                    clearPacked(context, targetLang);
                    return context.t("mmt.prefix") + " " + context.t("mmt.command.clear.cleared_packed");

                case "ai":
                    clearAiFiles(context, targetLang);
                    return context.t("mmt.prefix") + " " + context.t("mmt.command.clear.cleared_ai");

                case "all":
                    clearExtracted(context, targetLang);
                    clearTranslated(context, targetLang);
                    clearPacked(context, targetLang);
                    clearAiFiles(context, targetLang);
                    return context.t("mmt.prefix") + " " + context.t("mmt.command.clear.cleared_all");

                default:
                    return context.t("mmt.prefix") + " " + context.t("mmt.command.clear.unknown_sub", subCommand);
            }
        } catch (Exception e) {
            context.getLogger().error("Clear failed", e);
            return context.t("mmt.prefix") + " " + context.t("mmt.command.clear.failed", e.getMessage());
        }
    }

    private void clearExtracted(CommandContext context, String targetLang) throws Exception {
        Path path = context.getPathHelper().getExtractedPath();
        ExtractedData data = JsonUtil.readFromFile(path, ExtractedData.class);
        if (data == null) {
            data = new ExtractedData();
        }
        data.remove(targetLang);
        JsonUtil.writeToFile(data, path);
    }

    private void clearTranslated(CommandContext context, String targetLang) throws Exception {
        Path path = context.getPathHelper().getTranslatedPath();
        TranslatedData data = JsonUtil.readFromFile(path, TranslatedData.class);
        if (data == null) {
            data = new TranslatedData();
        }
        data.remove(targetLang);
        JsonUtil.writeToFile(data, path);
    }

    private void clearPacked(CommandContext context, String targetLang) throws Exception {
        Path path = context.getPathHelper().getPackedPath();
        PackedData data = JsonUtil.readFromFile(path, PackedData.class);
        if (data == null) {
            data = new PackedData();
        }
        data.remove(targetLang);
        JsonUtil.writeToFile(data, path);

        Path resourcePackDir = context.getPathHelper().getResourcePackDir(targetLang);
        if (Files.exists(resourcePackDir)) {
            deleteDirectory(resourcePackDir);
        }
    }

    private void clearAiFiles(CommandContext context, String targetLang) throws Exception {
        Path mmtDir = context.getPathHelper().getMmtDir();
        try (var stream = Files.newDirectoryStream(mmtDir, "AItranslation_" + targetLang + "_*.txt")) {
            for (Path file : stream) {
                try {
                    Files.delete(file);
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        Path resultFile = context.getPathHelper().getAiResultFile(targetLang);
        if (Files.exists(resultFile)) {
            Files.write(resultFile, new byte[0]);
        }
    }

    private void deleteDirectory(Path dir) throws Exception {
        if (Files.isDirectory(dir)) {
            try (var stream = Files.list(dir)) {
                stream.forEach(p -> {
                    try {
                        deleteDirectory(p);
                    } catch (Exception e) {
                        // ignore
                    }
                });
            }
        }
        Files.deleteIfExists(dir);
    }

    @Override
    public String[] getTabCompletions(String[] args) {
        if (args.length == 1) {
            return new String[]{"extracted", "translated", "packed", "ai", "all"};
        }
        if (args.length == 2) {
            return new String[]{"zh_cn", "en_us", "ja_jp", "ru_ru", "ko_kr", "fr_fr", "de_de", "es_es", "pt_br", "it_it"};
        }
        return new String[0];
    }
}