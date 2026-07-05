package com.mmt.core.pipeline;

import com.mmt.core.api.IPlatformAdapter;
import com.mmt.core.config.ConfigManager;
import com.mmt.core.data.path.PathHelper;
import com.mmt.core.extract.Extractor;
import com.mmt.core.i18n.I18nManager;
import com.mmt.core.log.MmtLogger;
import com.mmt.core.pack.Packer;
import com.mmt.core.translate.Translator;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoPipeline {

    private final ConfigManager configManager;
    private final Extractor extractor;
    private final Translator translator;
    private final Packer packer;
    private final MmtLogger logger;
    private final String mcVersion;
    private final IPlatformAdapter platformAdapter;
    private final I18nManager i18nManager;
    private final PathHelper pathHelper;

    private final AtomicBoolean hasRun = new AtomicBoolean(false);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean summaryShown = new AtomicBoolean(false);

    public AutoPipeline(ConfigManager configManager, Extractor extractor,
                        Translator translator, Packer packer,
                        MmtLogger logger, String mcVersion,
                        IPlatformAdapter platformAdapter,
                        I18nManager i18nManager,
                        PathHelper pathHelper) {
        this.configManager = configManager;
        this.extractor = extractor;
        this.translator = translator;
        this.packer = packer;
        this.logger = logger;
        this.mcVersion = mcVersion;
        this.platformAdapter = platformAdapter;
        this.i18nManager = i18nManager;
        this.pathHelper = pathHelper;
    }

    public boolean hasRun() {
        return hasRun.get();
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * 游戏启动时调用：静默运行流水线，不发送聊天消息
     */
    public void startAsync() {
        if (hasRun.get()) {
            logger.debug("Auto pipeline already ran, skipping");
            return;
        }

        if (isRunning.compareAndSet(false, true)) {
            Thread thread = new Thread(() -> {
                try {
                    runPipelineInternal(false, false);
                } finally {
                    isRunning.set(false);
                    hasRun.set(true);
                }
            }, "MMT-AutoPipeline");
            thread.setDaemon(true);
            thread.start();
        } else {
            logger.debug("Auto pipeline already running");
        }
    }

    /**
     * 玩家进入世界时调用：再次运行流水线并输出进度摘要
     * 如果启动时的流水线仍在运行，等待其完成后再运行
     */
    public void onPlayerJoinedWorld() {
        if (summaryShown.get()) {
            logger.debug("Pipeline summary already shown, skipping");
            return;
        }

        boolean enabled = configManager.getBoolean("show_join_summary", true);
        if (!enabled) {
            logger.debug("Join summary disabled in config, skipping");
            summaryShown.set(true);
            return;
        }

        if (!summaryShown.compareAndSet(false, true)) {
            return;
        }

        Thread thread = new Thread(() -> {
            while (isRunning.get()) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            if (isRunning.compareAndSet(false, true)) {
                try {
                    runPipelineInternal(true, true);
                } finally {
                    isRunning.set(false);
                }
            }
        }, "MMT-WorldJoinPipeline");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * 手动触发流水线（/mmt auto 命令）
     */
    public void runManually(String targetLang) {
        if (isRunning.compareAndSet(false, true)) {
            Thread thread = new Thread(() -> runPipelineManual(targetLang), "MMT-ManualPipeline");
            thread.setDaemon(true);
            thread.start();
        } else {
            logger.debug("Pipeline already running");
        }
    }

    private void runPipelineInternal(boolean showSummary, boolean showDisableHint) {
        try {
            logger.info("========== MMT Auto Pipeline Start ==========");

            boolean autoExtract = configManager.getBoolean("auto_extract", true);
            boolean autoTranslate = configManager.getBoolean("auto_translate", true);
            boolean autoPack = configManager.getBoolean("auto_pack", true);

            logger.info("auto_extract=" + autoExtract
                    + ", auto_translate=" + autoTranslate
                    + ", auto_pack=" + autoPack);

            String targetLang = configManager.getTargetLanguage(null);
            logger.info("Target language: " + targetLang);

            boolean extractOk = true;
            boolean translateOk = true;
            boolean packOk = true;
            String lastStep = "none";

            if (autoExtract) {
                extractOk = doExtract(targetLang, showSummary);
                if (!extractOk) {
                    logger.warn("Auto pipeline stopped at extract step");
                    if (showSummary && showDisableHint) {
                        sendDisableHint();
                    }
                    return;
                }
                lastStep = "extract";
            } else {
                logger.info("Auto extract disabled, skipping");
            }

            if (autoTranslate) {
                translateOk = doTranslate(targetLang, showSummary);
                if (!translateOk) {
                    logger.warn("Auto pipeline stopped at translate step");
                    if (showSummary && showDisableHint) {
                        sendDisableHint();
                    }
                    return;
                }
                lastStep = "translate";
            } else {
                logger.info("Auto translate disabled, skipping");
            }

            if (autoPack) {
                packOk = doPack(targetLang, showSummary);
                if (!packOk) {
                    logger.warn("Auto pipeline stopped at pack step");
                    if (showSummary && showDisableHint) {
                        sendDisableHint();
                    }
                    return;
                }
                lastStep = "pack";
            } else {
                logger.info("Auto pack disabled, skipping");
            }

            logger.info("========== MMT Auto Pipeline Complete ==========");

            if (showSummary) {
                sendNextStepHint(targetLang, lastStep);
                if (showDisableHint) {
                    sendDisableHint();
                }
            }

        } catch (Exception e) {
            logger.error("Auto pipeline failed with exception", e);
        } finally {
            hasRun.set(true);
            isRunning.set(false);
            if (showSummary) {
                summaryShown.set(true);
            }
        }
    }

    private void runPipelineManual(String targetLang) {
        try {
            logger.info("========== MMT Manual Pipeline Start ==========");
            logger.info("Target language: " + targetLang);

            boolean extractOk = doExtract(targetLang, true);
            if (!extractOk) {
                logger.warn("Manual pipeline stopped at extract step");
                return;
            }

            boolean translateOk = doTranslate(targetLang, true);
            if (!translateOk) {
                logger.warn("Manual pipeline stopped at translate step");
                sendNextStepHint(targetLang, "extract");
                return;
            }

            boolean packOk = doPack(targetLang, true);
            if (!packOk) {
                logger.warn("Manual pipeline stopped at pack step");
                sendNextStepHint(targetLang, "translate");
                return;
            }

            logger.info("========== MMT Manual Pipeline Complete ==========");
            sendNextStepHint(targetLang, "pack");

        } catch (Exception e) {
            logger.error("Manual pipeline failed with exception", e);
        } finally {
            isRunning.set(false);
        }
    }

    /**
     * 发送下一步提示
     */
    private void sendNextStepHint(String targetLang, String lastStep) {
        try {
            int extracted = countExtractedKeys(targetLang);
            int translated = countTranslatedKeys(targetLang);
            int packed = countPackedKeys(targetLang);
            int pendingTranslate = Math.max(0, extracted - translated);
            int pendingPack = Math.max(0, translated - packed);

            String prefix = i18nManager.t("mmt.prefix");
            String mmtDir = pathHelper.getMmtDir().toString();

            if ("extract".equals(lastStep)) {
                if (pendingTranslate > 0) {
                    String aiResultName = "AItranslationResult_" + targetLang + ".txt";

                    String head = prefix + " "
                            + i18nManager.t("mmt.pipeline.next_step.translate.head") + " ";
                    String tail = " "
                            + i18nManager.t("mmt.pipeline.next_step.translate.tail",
                                    aiResultName, targetLang);
                    platformAdapter.sendChatMessageWithFilePath(head, mmtDir, tail);
                } else if (pendingPack > 0) {
                    platformAdapter.sendChatMessage(prefix + " "
                            + i18nManager.t("mmt.pipeline.next_step.pack"));
                } else {
                    sendDoneMessage(targetLang);
                }
            } else if ("translate".equals(lastStep)) {
                if (pendingPack > 0) {
                    platformAdapter.sendChatMessage(prefix + " "
                            + i18nManager.t("mmt.pipeline.next_step.pack"));
                } else {
                    sendDoneMessage(targetLang);
                }
            } else if ("pack".equals(lastStep)) {
                sendDoneMessage(targetLang);
            }

        } catch (Exception e) {
            logger.warn("Failed to send next step hint: " + e.getMessage());
        }
    }

    private void sendDoneMessage(String targetLang) {
        String prefix = i18nManager.t("mmt.prefix");
        String rpDir = pathHelper.getResourcePacksDir().toString();
        String rpName = "MMT_" + targetLang;

        String head = prefix + " "
                + i18nManager.t("mmt.pipeline.next_step.done.head", rpName) + " ";
        String tail = " "
                + i18nManager.t("mmt.pipeline.next_step.done.tail");
        platformAdapter.sendChatMessageWithFilePath(head, rpDir, tail);
    }

    private void sendDisableHint() {
        String prefix = i18nManager.t("mmt.prefix");
        platformAdapter.sendChatMessage(prefix + " "
                + i18nManager.t("mmt.pipeline.disable_hint"));
    }

    private boolean doExtract(String targetLang, boolean sendMessage) {
        logger.info("[Extract] Starting...");
        long start = System.currentTimeMillis();

        int beforeCount = countExtractedKeys(targetLang);

        boolean success = extractor.extract(targetLang);

        if (success) {
            int afterCount = countExtractedKeys(targetLang);
            int newKeys = afterCount - beforeCount;
            int modCount = countExtractedMods(targetLang);
            long elapsed = System.currentTimeMillis() - start;

            logger.info("[Extract] Done: " + modCount + " mods, "
                    + (newKeys >= 0 ? "+" : "") + newKeys + " new keys ("
                    + afterCount + " total), " + elapsed + "ms");

            if (sendMessage) {
                sendExtractMessage(targetLang);
            }
        } else {
            logger.warn("[Extract] Failed");
            if (sendMessage) {
                String prefix = i18nManager.t("mmt.prefix");
                platformAdapter.sendChatMessage(prefix + " " + i18nManager.t("mmt.command.extract.failed"));
            }
        }

        return success;
    }

    private void sendExtractMessage(String targetLang) {
        Map<String, Object> stats = extractor.getLastStats();
        int processedMods = (int) stats.getOrDefault("processed_mods", 0);
        int processedKeys = (int) stats.getOrDefault("processed_keys", 0);
        int totalMods = (int) stats.getOrDefault("total_mods", 0);
        int totalKeys = (int) stats.getOrDefault("total_keys", 0);
        int dictHits = (int) stats.getOrDefault("dict_hits", 0);

        String prefix = i18nManager.t("mmt.prefix");
        String msg = prefix + " "
                + i18nManager.t("mmt.command.extract.success") + " "
                + i18nManager.t("mmt.command.extract.stats",
                        processedMods, processedKeys, totalMods, totalKeys, dictHits);
        platformAdapter.sendChatMessage(msg);
    }

    private boolean doTranslate(String targetLang, boolean sendMessage) {
        logger.info("[Translate] Starting...");
        long start = System.currentTimeMillis();

        int beforeCount = countTranslatedKeys(targetLang);

        boolean success = translator.translate(targetLang);

        if (success) {
            int afterCount = countTranslatedKeys(targetLang);
            int newKeys = afterCount - beforeCount;
            long elapsed = System.currentTimeMillis() - start;

            Map<String, Integer> methodCounts = getTranslateMethodCounts(targetLang);

            StringBuilder sb = new StringBuilder();
            sb.append("[Translate] Done: ").append(newKeys >= 0 ? "+" : "").append(newKeys)
                    .append(" new keys (").append(afterCount).append(" total), ")
                    .append(elapsed).append("ms");

            if (!methodCounts.isEmpty()) {
                sb.append(" (");
                boolean first = true;
                for (Map.Entry<String, Integer> entry : methodCounts.entrySet()) {
                    if (!first) sb.append(", ");
                    sb.append(entry.getKey()).append(": ").append(entry.getValue());
                    first = false;
                }
                sb.append(")");
            }

            logger.info(sb.toString());

            if (sendMessage) {
                sendTranslateMessage(targetLang);
            }
        } else {
            logger.warn("[Translate] Failed");
            if (sendMessage) {
                String prefix = i18nManager.t("mmt.prefix");
                platformAdapter.sendChatMessage(prefix + " " + i18nManager.t("mmt.command.translate.failed"));
            }
        }

        return success;
    }

    @SuppressWarnings("unchecked")
    private void sendTranslateMessage(String targetLang) {
        Map<String, Object> stats = translator.getLastStats();
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

        String prefix = i18nManager.t("mmt.prefix");
        String msg = prefix + " "
                + i18nManager.t("mmt.command.translate.success") + " "
                + i18nManager.t("mmt.command.translate.stats",
                        modCount, totalKeys, translatedKeys, failedKeys,
                        methodStr.length() > 0 ? methodStr.toString() : "none");
        platformAdapter.sendChatMessage(msg);
    }

    private boolean doPack(String targetLang, boolean sendMessage) {
        logger.info("[Pack] Starting...");
        long start = System.currentTimeMillis();

        int beforeCount = countPackedKeys(targetLang);

        boolean success = packer.pack(targetLang, mcVersion);

        if (success) {
            int afterCount = countPackedKeys(targetLang);
            int newKeys = afterCount - beforeCount;
            int modCount = countPackedMods(targetLang);
            long elapsed = System.currentTimeMillis() - start;

            logger.info("[Pack] Done: " + modCount + " mods, "
                    + (newKeys >= 0 ? "+" : "") + newKeys + " new keys ("
                    + afterCount + " total), " + elapsed + "ms");

            if (sendMessage) {
                sendPackMessage(targetLang);
            }
        } else {
            logger.warn("[Pack] Failed");
            if (sendMessage) {
                String prefix = i18nManager.t("mmt.prefix");
                platformAdapter.sendChatMessage(prefix + " " + i18nManager.t("mmt.command.pack.failed"));
            }
        }

        return success;
    }

    private void sendPackMessage(String targetLang) {
        Map<String, Object> stats = packer.getLastStats();
        int modCount = (int) stats.getOrDefault("mod_count", 0);
        int totalKeys = (int) stats.getOrDefault("total_keys", 0);
        int userTranslated = (int) stats.getOrDefault("user_translated", 0);
        int overridden = (int) stats.getOrDefault("overridden", 0);

        String prefix = i18nManager.t("mmt.prefix");
        String msg = prefix + " "
                + i18nManager.t("mmt.command.pack.success") + " "
                + i18nManager.t("mmt.command.pack.stats", modCount, totalKeys, userTranslated, overridden);
        platformAdapter.sendChatMessage(msg);
    }

    private int countExtractedKeys(String targetLang) {
        Map<String, Integer> stats = extractor.getExtractStats(targetLang);
        return stats.values().stream().mapToInt(Integer::intValue).sum();
    }

    private int countExtractedMods(String targetLang) {
        return extractor.getExtractStats(targetLang).size();
    }

    private int countTranslatedKeys(String targetLang) {
        Map<String, Object> stats = translator.getTranslationStats(targetLang);
        Object total = stats.get("total_entries");
        return total instanceof Number ? ((Number) total).intValue() : 0;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> getTranslateMethodCounts(String targetLang) {
        Map<String, Object> stats = translator.getTranslationStats(targetLang);
        Object counts = stats.get("method_counts");
        if (counts instanceof Map) {
            return (Map<String, Integer>) counts;
        }
        return new java.util.HashMap<>();
    }

    private int countPackedKeys(String targetLang) {
        Map<String, Object> stats = packer.getPackStats(targetLang);
        Object total = stats.get("total_entries");
        return total instanceof Number ? ((Number) total).intValue() : 0;
    }

    private int countPackedMods(String targetLang) {
        Map<String, Object> stats = packer.getPackStats(targetLang);
        Object count = stats.get("mod_count");
        return count instanceof Number ? ((Number) count).intValue() : 0;
    }
}
