package com.mmt.core.command.sub;

import com.mmt.core.command.CommandContext;
import com.mmt.core.command.ICommand;

public class ConfigCommand implements ICommand {
    @Override
    public String getName() {
        return "config";
    }

    @Override
    public String getDescription() {
        return "Manage configuration";
    }

    @Override
    public String getUsage() {
        return "/mmt config get <key> | set <key> <value> | reload";
    }

    @Override
    public String execute(CommandContext context, String[] args, String playerName) {
        if (args.length == 0) {
            return context.t("mmt.prefix") + " " + context.t("mmt.command.config.usage");
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "get":
                if (args.length < 2) {
                    return context.t("mmt.prefix") + " " + context.t("mmt.command.config.specify_key");
                }
                String key = args[1];
                String value = context.getConfigManager().getString(key, "");
                return context.t("mmt.prefix") + " " + context.t("mmt.command.config.get", key, value.isEmpty() ? context.t("mmt.command.config.not_set") : value);

            case "set":
                if (args.length < 3) {
                    return context.t("mmt.prefix") + " " + context.t("mmt.command.config.specify_key_value");
                }
                key = args[1];
                value = args[2];
                context.getConfigManager().setString(key, value);
                return context.t("mmt.prefix") + " " + context.t("mmt.command.config.set", key, value);

            case "reload":
                context.getConfigManager().reload();
                return context.t("mmt.prefix") + " " + context.t("mmt.command.config.reload");

            default:
                return context.t("mmt.prefix") + " " + context.t("mmt.command.config.unknown_sub", subCommand);
        }
    }

    @Override
    public String[] getTabCompletions(String[] args) {
        if (args.length == 1) {
            return new String[]{"get", "set", "reload"};
        }
        if (args.length == 2 && "get".equals(args[0].toLowerCase())) {
            return new String[]{
                    "target_language", "target_languages",
                    "extract_broad_scope", "extract_list_mode", "extract_mod_list", "extract_mode",
                    "translate_methods",
                    "ai_api_url", "ai_api_key", "ai_api_model",
                    "mt_api_url", "mt_api_key", "mt_api_model",
                    "request_char_limit", "request_interval_ms", "retry_count"
            };
        }
        if (args.length == 2 && "set".equals(args[0].toLowerCase())) {
            return new String[]{
                    "target_language", "target_languages",
                    "extract_broad_scope", "extract_list_mode", "extract_mod_list", "extract_mode",
                    "translate_methods",
                    "ai_api_url", "ai_api_key", "ai_api_model",
                    "mt_api_url", "mt_api_key", "mt_api_model",
                    "request_char_limit", "request_interval_ms", "retry_count"
            };
        }
        return new String[0];
    }
}