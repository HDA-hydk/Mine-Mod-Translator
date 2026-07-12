package com.mmt.core.command.sub;

import com.mmt.core.command.CommandContext;
import com.mmt.core.command.ICommand;
import com.mmt.core.data.path.PathHelper;
import com.mmt.core.log.MmtLogger;
import com.mmt.core.translate.dict.I18nDict;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DictCommand implements ICommand {
    private static final String MINI_DICT_URL = "https://raw.githubusercontent.com/CFPAOrg/i18n-dict/main/Dict-Mini.json";
    private static final String FULL_DICT_URL = "https://raw.githubusercontent.com/CFPAOrg/i18n-dict/main/Dict-Full.json";
    private static final String MINI_FILENAME = "Dict-Mini.json";
    private static final String FULL_FILENAME = "Dict-Full.json";
    private static volatile boolean downloading = false;

    @Override
    public String getName() {
        return "dict";
    }

    @Override
    public String getDescription() {
        return "Dictionary operations";
    }

    @Override
    public String getUsage() {
        return "/mmt dict <status|install|reload|dir>";
    }

    @Override
    public String execute(CommandContext context, String[] args, String playerName) {
        if (args.length == 0 || "status".equalsIgnoreCase(args[0])) {
            return handleStatus(context);
        }

        switch (args[0].toLowerCase()) {
            case "install":
                return handleInstall(context, args);
            case "reload":
                return handleReload(context);
            case "dir":
                return handleDir(context);
            default:
                return context.t("mmt.prefix") + " " + context.t("mmt.command.dict.unknown_sub", args[0]);
        }
    }

    private String handleStatus(CommandContext context) {
        I18nDict dict = I18nDict.getInstance(context.getLogger());
        PathHelper pathHelper = context.getPathHelper();
        Path dictDir = pathHelper.getI18nDicDir();

        int fileCount = 0;
        long totalSize = 0;
        if (Files.exists(dictDir)) {
            try (Stream<Path> stream = Files.list(dictDir)) {
                List<Path> files = stream.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".json"))
                        .collect(Collectors.toList());
                fileCount = files.size();
                for (Path f : files) {
                    try {
                        totalSize += Files.size(f);
                    } catch (IOException ignored) {}
                }
            } catch (IOException ignored) {}
        }

        if (!dict.isLoaded()) {
            dict.load();
        }

        String sizeStr = formatSize(totalSize);
        if (dict.isLoaded()) {
            return context.t("mmt.prefix") + " " + context.t("mmt.command.dict.status",
                    dict.getLoadedFileCount(), dict.size(), fileCount, sizeStr);
        } else {
            return context.t("mmt.prefix") + " " + context.t("mmt.command.dict.not_loaded", fileCount, sizeStr);
        }
    }

    private String handleInstall(CommandContext context, String[] args) {
        if (args.length < 2) {
            return context.t("mmt.prefix") + " " + context.t("mmt.command.dict.install.usage");
        }

        String version = args[1].toLowerCase();
        String urlStr;
        String fileName;

        switch (version) {
            case "mini":
                urlStr = MINI_DICT_URL;
                fileName = MINI_FILENAME;
                break;
            case "full":
                urlStr = FULL_DICT_URL;
                fileName = FULL_FILENAME;
                break;
            default:
                return context.t("mmt.prefix") + " " + context.t("mmt.command.dict.install.unknown_version", version);
        }

        if (downloading) {
            return context.t("mmt.prefix") + " " + context.t("mmt.command.dict.install.already_downloading");
        }

        PathHelper pathHelper = context.getPathHelper();
        Path dictDir = pathHelper.getI18nDicDir();
        pathHelper.ensureDirExists(dictDir);
        Path targetPath = dictDir.resolve(fileName);

        MmtLogger logger = context.getLogger();

        downloading = true;
        new Thread(() -> {
            try {
                logger.info("Starting download of " + fileName + " from " + urlStr);
                downloadFile(urlStr, targetPath, logger);
                logger.info("Download completed: " + fileName);

                I18nDict dict = I18nDict.getInstance(logger);
                dict.reload();
            } catch (Exception e) {
                logger.error("Failed to download dictionary: " + fileName, e);
            } finally {
                downloading = false;
            }
        }, "MMT-Dict-Downloader").start();

        return context.t("mmt.prefix") + " " + context.t("mmt.command.dict.install.started", fileName);
    }

    private void downloadFile(String urlStr, Path targetPath, MmtLogger logger) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("User-Agent", "MMT-Mod");

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP " + responseCode + ": " + conn.getResponseMessage());
        }

        long contentLength = conn.getContentLengthLong();

        try (InputStream in = new BufferedInputStream(conn.getInputStream())) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        long downloaded = Files.size(targetPath);
        logger.info("Downloaded " + fileName(targetPath) + ": " + formatSize(downloaded));
    }

    private static String fileName(Path path) {
        return path.getFileName().toString();
    }

    private String handleReload(CommandContext context) {
        I18nDict dict = I18nDict.getInstance(context.getLogger());
        dict.reload();

        if (dict.isLoaded()) {
            return context.t("mmt.prefix") + " " + context.t("mmt.command.dict.reload.success",
                    dict.getLoadedFileCount(), dict.size());
        } else {
            return context.t("mmt.prefix") + " " + context.t("mmt.command.dict.reload.failed");
        }
    }

    private String handleDir(CommandContext context) {
        PathHelper pathHelper = context.getPathHelper();
        Path dictDir = pathHelper.getI18nDicDir();
        pathHelper.ensureDirExists(dictDir);
        return context.t("mmt.prefix") + " " + context.t("mmt.command.dict.dir", dictDir.toAbsolutePath());
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }

    @Override
    public String[] getTabCompletions(String[] args) {
        if (args.length == 1) {
            return new String[]{"status", "install", "reload", "dir"};
        }
        if (args.length == 2 && "install".equalsIgnoreCase(args[0])) {
            return new String[]{"mini", "full"};
        }
        return new String[0];
    }
}
