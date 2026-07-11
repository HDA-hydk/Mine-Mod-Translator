package com.mmt.neoforge;

import com.mmt.core.Constants;
import com.mmt.core.MmtMod;
import com.mmt.core.command.CommandContext;
import com.mmt.core.command.sub.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID)
public class MmtCommands {
    private static final ExtractCommand EXTRACT_COMMAND = new ExtractCommand();
    private static final TranslateCommand TRANSLATE_COMMAND = new TranslateCommand();
    private static final PackCommand PACK_COMMAND = new PackCommand();
    private static final ConfigCommand CONFIG_COMMAND = new ConfigCommand();
    private static final DictCommand DICT_COMMAND = new DictCommand();
    private static final ClearCommand CLEAR_COMMAND = new ClearCommand();
    private static final StatusCommand STATUS_COMMAND = new StatusCommand();
    private static final AutoCommand AUTO_COMMAND = new AutoCommand();
    private static final ToggleJoinMsgCommand TOGGLE_JOIN_MSG_COMMAND = new ToggleJoinMsgCommand();

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        LiteralArgumentBuilder<CommandSourceStack> mmtCommand = Commands.literal("mmt");

        LiteralArgumentBuilder<CommandSourceStack> extractCmd = Commands.literal("extract");
        extractCmd.executes(MmtCommands::executeExtract);
        extractCmd.then(Commands.argument("targetLanguage", StringArgumentType.word())
                .suggests(MmtCommands::suggestLanguage)
                .executes(MmtCommands::executeExtract)
                .then(Commands.argument("broadScope", StringArgumentType.word())
                        .suggests(MmtCommands::suggestBroadScope)
                        .executes(MmtCommands::executeExtract)
                        .then(Commands.argument("listMode", StringArgumentType.word())
                                .suggests(MmtCommands::suggestListMode)
                                .executes(MmtCommands::executeExtract)
                                .then(Commands.argument("modList", StringArgumentType.greedyString())
                                        .executes(MmtCommands::executeExtract)
                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                .suggests(MmtCommands::suggestExtractMode)
                                                .executes(MmtCommands::executeExtract))))));
        mmtCommand.then(extractCmd);

        mmtCommand.then(Commands.literal("translate")
                .executes(MmtCommands::executeTranslate)
                .then(Commands.argument("targetLanguage", StringArgumentType.word())
                        .suggests(MmtCommands::suggestLanguage)
                        .executes(MmtCommands::executeTranslate)
                        .then(Commands.argument("method", StringArgumentType.word())
                                .suggests(MmtCommands::suggestTranslateMethod)
                                .executes(MmtCommands::executeTranslate))));

        mmtCommand.then(Commands.literal("pack")
                .executes(MmtCommands::executePack)
                .then(Commands.argument("targetLanguage", StringArgumentType.word())
                        .suggests(MmtCommands::suggestLanguage)
                        .executes(MmtCommands::executePack)));

        mmtCommand.then(Commands.literal("config")
                .then(Commands.literal("get")
                        .then(Commands.argument("key", StringArgumentType.word())
                                .suggests(MmtCommands::suggestConfigKey)
                                .executes(MmtCommands::executeConfig)))
                .then(Commands.literal("set")
                        .then(Commands.argument("key", StringArgumentType.word())
                                .suggests(MmtCommands::suggestConfigKey)
                                .then(Commands.argument("value", StringArgumentType.greedyString())
                                        .executes(MmtCommands::executeConfig))))
                .then(Commands.literal("reload")
                        .executes(MmtCommands::executeConfig)));

        mmtCommand.then(Commands.literal("dict")
                .executes(MmtCommands::executeDict)
                .then(Commands.argument("subcommand", StringArgumentType.word())
                        .suggests(MmtCommands::suggestDictSub)
                        .executes(MmtCommands::executeDict)
                        .then(Commands.argument("version", StringArgumentType.word())
                                .suggests(MmtCommands::suggestDictVersion)
                                .executes(MmtCommands::executeDict))));

        mmtCommand.then(Commands.literal("clear")
                .then(Commands.literal("extracted")
                        .executes(MmtCommands::executeClear)
                        .then(Commands.argument("lang", StringArgumentType.word())
                                .suggests(MmtCommands::suggestLanguage)
                                .executes(MmtCommands::executeClear)))
                .then(Commands.literal("translated")
                        .executes(MmtCommands::executeClear)
                        .then(Commands.argument("lang", StringArgumentType.word())
                                .suggests(MmtCommands::suggestLanguage)
                                .executes(MmtCommands::executeClear)))
                .then(Commands.literal("packed")
                        .executes(MmtCommands::executeClear)
                        .then(Commands.argument("lang", StringArgumentType.word())
                                .suggests(MmtCommands::suggestLanguage)
                                .executes(MmtCommands::executeClear)))
                .then(Commands.literal("ai")
                        .executes(MmtCommands::executeClear))
                .then(Commands.literal("all")
                        .executes(MmtCommands::executeClear)
                        .then(Commands.argument("lang", StringArgumentType.word())
                                .suggests(MmtCommands::suggestLanguage)
                                .executes(MmtCommands::executeClear))));

        mmtCommand.then(Commands.literal("status")
                .executes(MmtCommands::executeStatus)
                .then(Commands.argument("targetLanguage", StringArgumentType.word())
                        .suggests(MmtCommands::suggestLanguage)
                        .executes(MmtCommands::executeStatus)));

        mmtCommand.then(Commands.literal("auto")
                .executes(MmtCommands::executeAuto)
                .then(Commands.argument("targetLanguage", StringArgumentType.word())
                        .suggests(MmtCommands::suggestLanguage)
                        .executes(MmtCommands::executeAuto)));

        mmtCommand.then(Commands.literal("togglejoinmsg")
                .executes(MmtCommands::executeToggleJoinMsg)
                .then(Commands.argument("state", StringArgumentType.word())
                        .suggests(MmtCommands::suggestToggleJoinMsg)
                        .executes(MmtCommands::executeToggleJoinMsg)));

        mmtCommand.then(Commands.literal("confirm")
                .executes(MmtCommands::executeConfirm));

        dispatcher.register(mmtCommand);
    }

    private static int executeExtract(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        String[] args = getArgs(ctx, "targetLanguage", "broadScope", "listMode", "modList", "mode");
        sendMessage(ctx, EXTRACT_COMMAND.execute(getContext(), args, getPlayerName(ctx)));
        return 1;
    }

    private static int executeTranslate(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        String[] args = getArgs(ctx, "targetLanguage", "method");
        sendMessage(ctx, TRANSLATE_COMMAND.execute(getContext(), args, getPlayerName(ctx)));
        return 1;
    }

    private static int executePack(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        String[] args = getArgs(ctx, "targetLanguage");
        sendMessage(ctx, PACK_COMMAND.execute(getContext(), args, getPlayerName(ctx)));
        return 1;
    }

    private static int executeConfig(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        String subCmd = getArg(ctx, "get") != null ? "get" : (getArg(ctx, "set") != null ? "set" : "reload");
        String key = getArg(ctx, "key");
        String value = getArg(ctx, "value");

        String[] args;
        if ("reload".equals(subCmd)) {
            args = new String[]{subCmd};
        } else if ("set".equals(subCmd)) {
            args = new String[]{subCmd, key, value};
        } else {
            args = new String[]{subCmd, key};
        }

        sendMessage(ctx, CONFIG_COMMAND.execute(getContext(), args, getPlayerName(ctx)));
        return 1;
    }

    private static int executeDict(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        String subCmd = getArg(ctx, "subcommand");
        if (subCmd == null) subCmd = "status";
        String version = getArg(ctx, "version");

        String[] args;
        if ("install".equals(subCmd) && version != null) {
            args = new String[]{subCmd, version};
        } else {
            args = new String[]{subCmd};
        }
        sendMessage(ctx, DICT_COMMAND.execute(getContext(), args, getPlayerName(ctx)));
        return 1;
    }

    private static int executeClear(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        String subCmd = getArg(ctx, "extracted") != null ? "extracted"
                : getArg(ctx, "translated") != null ? "translated"
                : getArg(ctx, "packed") != null ? "packed"
                : getArg(ctx, "ai") != null ? "ai" : "all";
        String lang = getArg(ctx, "lang");

        String[] args = lang != null ? new String[]{subCmd, lang} : new String[]{subCmd};
        sendMessage(ctx, CLEAR_COMMAND.execute(getContext(), args, getPlayerName(ctx)));
        return 1;
    }

    private static int executeStatus(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        String[] args = getArgs(ctx, "targetLanguage");
        sendMessage(ctx, STATUS_COMMAND.execute(getContext(), args, getPlayerName(ctx)));
        return 1;
    }

    private static int executeAuto(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        String[] args = getArgs(ctx, "targetLanguage");
        sendMessage(ctx, AUTO_COMMAND.execute(getContext(), args, getPlayerName(ctx)));
        return 1;
    }

    private static int executeToggleJoinMsg(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        String[] args = getArgs(ctx, "state");
        sendMessage(ctx, TOGGLE_JOIN_MSG_COMMAND.execute(getContext(), args, getPlayerName(ctx)));
        return 1;
    }

    private static int executeConfirm(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        sendMessage(ctx, "[MMT] 确认功能已实现");
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestLanguage(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return suggestAll(builder, new String[]{"zh_cn", "en_us", "ja_jp", "ru_ru", "ko_kr", "fr_fr", "de_de", "es_es", "pt_br", "it_it"});
    }

    private static CompletableFuture<Suggestions> suggestBroadScope(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return suggestAll(builder, new String[]{"new", "all"});
    }

    private static CompletableFuture<Suggestions> suggestListMode(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return suggestAll(builder, new String[]{"blacklist", "whitelist"});
    }

    private static CompletableFuture<Suggestions> suggestExtractMode(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return suggestAll(builder, new String[]{"diff", "diff_same", "full"});
    }

    private static CompletableFuture<Suggestions> suggestTranslateMethod(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return suggestAll(builder, new String[]{"I18n-dict", "AI-manual", "AI-auto", "MT"});
    }

    private static CompletableFuture<Suggestions> suggestConfigKey(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return suggestAll(builder, new String[]{
                "target_language", "target_languages",
                "extract_broad_scope", "extract_list_mode", "extract_mod_list", "extract_mode",
                "translate_methods",
                "ai_api_url", "ai_api_key", "ai_api_model",
                "mt_api_url", "mt_api_key", "mt_api_model",
                "request_char_limit", "request_interval_ms", "retry_count"
        });
    }

    private static CompletableFuture<Suggestions> suggestToggleJoinMsg(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return suggestAll(builder, new String[]{"on", "off"});
    }

    private static CompletableFuture<Suggestions> suggestDictSub(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return suggestAll(builder, new String[]{"status", "install", "reload", "dir"});
    }

    private static CompletableFuture<Suggestions> suggestDictVersion(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return suggestAll(builder, new String[]{"mini", "full"});
    }

    private static CompletableFuture<Suggestions> suggestAll(SuggestionsBuilder builder, String[] options) {
        Arrays.stream(options).filter(opt -> opt.startsWith(builder.getRemainingLowerCase()))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    private static CommandContext getContext() {
        return MmtMod.getInstance().getCommandContext();
    }

    private static void sendMessage(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String message) {
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(message), false);
    }

    private static String getPlayerName(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        try {
            return ctx.getSource().getPlayerOrException().getName().getString();
        } catch (Exception e) {
            return "server";
        }
    }

    private static String[] getArgs(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String... names) {
        String[] args = new String[names.length];
        for (int i = 0; i < names.length; i++) {
            args[i] = getArg(ctx, names[i]);
        }
        return Arrays.stream(args).filter(a -> a != null).toArray(String[]::new);
    }

    private static String getArg(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return StringArgumentType.getString(ctx, name);
        } catch (Exception e) {
            return null;
        }
    }
}
