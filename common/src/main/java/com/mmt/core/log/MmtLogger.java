package com.mmt.core.log;

import com.mmt.core.Constants;
import com.mmt.core.api.IPlatformAdapter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

/**
 * MMT 日志工具类
 * 提供分级日志输出功能
 * 日志格式：[时间] [级别] 消息
 * 日志文件：<版本>/mmt/logs/mmt-YYYY-MM-DD.log
 */
public class MmtLogger {
    private static MmtLogger INSTANCE;

    private final IPlatformAdapter platformAdapter;
    private int debugMode = 0;
    private PrintWriter fileWriter;
    private String currentLogDate;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private MmtLogger(IPlatformAdapter adapter) {
        this.platformAdapter = adapter;
    }

    /**
     * 获取单例实例
     */
    public static MmtLogger getInstance(IPlatformAdapter adapter) {
        if (INSTANCE == null) {
            INSTANCE = new MmtLogger(adapter);
        }
        return INSTANCE;
    }

    /**
     * 获取单例实例（已初始化）
     */
    public static MmtLogger getInstance() {
        return INSTANCE;
    }

    /**
     * 初始化日志系统
     * @param debugMode 调试模式（0=INFO/WARN，1=DEBUG）
     */
    public void init(int debugMode) {
        this.debugMode = debugMode;
        openLogFile();
    }

    /**
     * 设置调试模式
     */
    public void setDebugMode(int debugMode) {
        this.debugMode = debugMode;
    }

    /**
     * 打开日志文件
     */
    private void openLogFile() {
        try {
            Path logDir = getLogDirectory();
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }

            String dateStr = LocalDate.now().format(DATE_FORMATTER);
            Path logFile = logDir.resolve("mmt-" + dateStr + ".log");

            this.fileWriter = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(logFile.toFile(), true), StandardCharsets.UTF_8)));
            this.currentLogDate = dateStr;

            info("========================================");
            info(Constants.MOD_NAME + " v" + Constants.MOD_VERSION + " started");
            info("Platform: " + platformAdapter.getPlatformName());
            info("Minecraft: " + platformAdapter.getMinecraftVersion());
            info("========================================");
        } catch (IOException e) {
            // 日志文件创建失败，只输出到控制台
            System.err.println("[MMT] ERROR: Failed to open log file");
            e.printStackTrace();
        }
    }

    /**
     * 获取日志目录路径
     */
    private Path getLogDirectory() {
        return platformAdapter.getGameVersionDir()
                .resolve("mmt")
                .resolve("logs");
    }

    /**
     * 检查日志文件是否需要按日期轮转
     */
    private void checkLogRotation() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        if (!today.equals(currentLogDate)) {
            closeLogFile();
            openLogFile();
        }
    }

    /**
     * 关闭日志文件
     */
    private void closeLogFile() {
        if (fileWriter != null) {
            try {
                fileWriter.close();
            } catch (Exception e) {
                // ignore
            }
            fileWriter = null;
        }
    }

    /**
     * 输出 INFO 级别日志
     */
    public void info(String message) {
        log(LogLevel.INFO, message);
    }

    /**
     * 输出 INFO 级别日志（带格式化）
     */
    public void info(String format, Object... args) {
        log(LogLevel.INFO, String.format(format, args));
    }

    /**
     * 输出 WARN 级别日志
     */
    public void warn(String message) {
        log(LogLevel.WARN, message);
    }

    /**
     * 输出 WARN 级别日志（带格式化）
     */
    public void warn(String format, Object... args) {
        log(LogLevel.WARN, String.format(format, args));
    }

    /**
     * 输出 DEBUG 级别日志
     */
    public void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    /**
     * 输出 DEBUG 级别日志（带格式化）
     */
    public void debug(String format, Object... args) {
        log(LogLevel.DEBUG, String.format(format, args));
    }

    /**
     * 输出异常日志
     */
    public void error(String message, Throwable e) {
        warn(message);
        warn("Exception: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getName()));
        warn("Stack trace:");
        for (StackTraceElement element : e.getStackTrace()) {
            warn("  at " + element.toString());
        }
    }

    /**
     * 输出异常日志（简化版）
     */
    public void error(Throwable e) {
        error("An error occurred", e);
    }

    /**
     * 通用日志输出方法
     */
    private void log(LogLevel level, String message) {
        if (!level.shouldLog(debugMode)) {
            return;
        }

        checkLogRotation();

        String time = LocalDateTime.now().format(TIME_FORMATTER);
        String logMessage = "[" + time + "] [" + level.name() + "] " + message;

        // 输出到控制台
        if (level == LogLevel.WARN) {
            System.out.println("[MMT] " + logMessage);
        } else {
            System.out.println("[MMT] " + logMessage);
        }

        // 输出到文件（DEBUG 仅在 debugMode >= 1 时输出）
        if (fileWriter != null) {
            fileWriter.println(logMessage);
            fileWriter.flush();
        }
    }

    /**
     * 关闭日志系统（游戏退出时调用）
     */
    public void shutdown() {
        info("========================================");
        info(Constants.MOD_NAME + " shutdown");
        info("========================================");
        closeLogFile();
    }
}