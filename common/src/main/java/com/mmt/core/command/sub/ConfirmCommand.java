package com.mmt.core.command.sub;

import com.mmt.core.command.CommandContext;
import com.mmt.core.command.ConfirmManager;
import com.mmt.core.command.ICommand;

public class ConfirmCommand implements ICommand {
    private final ConfirmManager confirmManager;

    public ConfirmCommand(ConfirmManager confirmManager) {
        this.confirmManager = confirmManager;
    }

    @Override
    public String getName() {
        return "confirm";
    }

    @Override
    public String getDescription() {
        return "Confirm dangerous operations";
    }

    @Override
    public String getUsage() {
        return "/mmt confirm";
    }

    @Override
    public String execute(CommandContext context, String[] args, String playerName) {
        ConfirmManager.PendingOperation op = confirmManager.getPending(playerName);

        if (op == null) {
            return context.t("mmt.prefix") + " " + context.t("mmt.command.confirm.nothing");
        }

        if (op.isExpired()) {
            confirmManager.clearPending(playerName);
            return context.t("mmt.prefix") + " " + context.t("mmt.command.confirm.expired");
        }

        confirmManager.clearPending(playerName);

        String[] opArgs = new String[op.args.length + 1];
        opArgs[0] = op.type;
        System.arraycopy(op.args, 0, opArgs, 1, op.args.length);

        ClearCommand clearCommand = new ClearCommand();
        return clearCommand.execute(context, opArgs, playerName);
    }

    @Override
    public String[] getTabCompletions(String[] args) {
        return new String[0];
    }
}