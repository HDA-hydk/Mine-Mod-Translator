package com.mmt.core.command;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConfirmManager {
    private static final long CONFIRM_TIMEOUT = 15000;

    public static class PendingOperation {
        public final String type;
        public final String[] args;
        public final long timestamp;

        public PendingOperation(String type, String[] args) {
            this.type = type;
            this.args = args;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CONFIRM_TIMEOUT;
        }
    }

    private final Map<String, PendingOperation> pendingOperations = new ConcurrentHashMap<>();

    public void setPending(String playerName, String type, String[] args) {
        pendingOperations.put(playerName, new PendingOperation(type, args));
    }

    public PendingOperation getPending(String playerName) {
        PendingOperation op = pendingOperations.get(playerName);
        if (op != null && op.isExpired()) {
            pendingOperations.remove(playerName);
            return null;
        }
        return op;
    }

    public void clearPending(String playerName) {
        pendingOperations.remove(playerName);
    }

    public boolean hasPending(String playerName) {
        return getPending(playerName) != null;
    }
}