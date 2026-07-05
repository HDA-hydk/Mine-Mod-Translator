package com.mmt.core.command.sub;

import com.mmt.core.command.CommandContext;
import com.mmt.core.command.ICommand;

public class ToggleJoinMsgCommand implements ICommand {
    @Override
    public String getName() {
        return "togglejoinmsg";
    }

    @Override
    public String getDescription() {
        return "Toggle the join-world pipeline summary message";
    }

    @Override
    public String getUsage() {
        return "/mmt togglejoinmsg [on|off]";
    }

    @Override
    public String execute(CommandContext context, String[] args, String playerName) {
        boolean current = context.getConfigManager().getBoolean("show_join_summary", true);
        boolean newValue;

        if (args.length > 0) {
            String arg = args[0].toLowerCase();
            if (arg.equals("on") || arg.equals("true") || arg.equals("1")) {
                newValue = true;
            } else if (arg.equals("off") || arg.equals("false") || arg.equals("0")) {
                newValue = false;
            } else {
                return context.t("mmt.prefix") + " "
                        + context.t("mmt.command.togglejoinmsg.usage");
            }
        } else {
            newValue = !current;
        }

        context.getConfigManager().setString("show_join_summary", String.valueOf(newValue));
        try {
            context.getConfigManager().save();
        } catch (Exception e) {
            return context.t("mmt.prefix") + " "
                    + context.t("mmt.command.togglejoinmsg.save_failed", e.getMessage());
        }

        String statusKey = newValue ? "mmt.command.togglejoinmsg.enabled"
                : "mmt.command.togglejoinmsg.disabled";
        return context.t("mmt.prefix") + " " + context.t(statusKey);
    }

    @Override
    public String[] getTabCompletions(String[] args) {
        if (args.length == 1) {
            return new String[]{"on", "off"};
        }
        return new String[0];
    }
}
