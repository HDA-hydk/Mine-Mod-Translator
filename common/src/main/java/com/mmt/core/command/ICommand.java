package com.mmt.core.command;

public interface ICommand {
    String getName();

    String getDescription();

    String getUsage();

    String execute(CommandContext context, String[] args, String playerName);

    String[] getTabCompletions(String[] args);
}