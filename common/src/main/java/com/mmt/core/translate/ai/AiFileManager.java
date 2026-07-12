package com.mmt.core.translate.ai;

import com.mmt.core.data.path.PathHelper;
import com.mmt.core.log.MmtLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 翻译文件管理器
 * 管理按语言分隔的 AItranslation_[目标语言]_NN.txt 文件
 */
public class AiFileManager {
    private static final Pattern AI_FILE_PATTERN = Pattern.compile("AItranslation_(\\w+)_(\\d{2})\\.txt");
    private static final DateTimeFormatter ARCHIVE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final PathHelper pathHelper;
    private final MmtLogger logger;

    public AiFileManager(PathHelper pathHelper, MmtLogger logger) {
        this.pathHelper = pathHelper;
        this.logger = logger;
    }

    /**
     * 查找指定目标语言的所有 AI 翻译文件
     * 文件名格式：AItranslation_[目标语言]_NN.txt
     */
    public List<Path> findAiTranslationFiles(String targetLanguage) throws IOException {
        List<Path> files = new ArrayList<>();
        Path mmtDir = pathHelper.getMmtDir();

        if (!Files.exists(mmtDir)) {
            return files;
        }

        String glob = "AItranslation_" + targetLanguage + "_*.txt";
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(mmtDir, glob)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    files.add(file);
                }
            }
        }

        files.sort((a, b) -> {
            Matcher ma = AI_FILE_PATTERN.matcher(a.getFileName().toString());
            Matcher mb = AI_FILE_PATTERN.matcher(b.getFileName().toString());
            if (ma.matches() && mb.matches()) {
                return Integer.compare(Integer.parseInt(ma.group(2)), Integer.parseInt(mb.group(2)));
            }
            return a.compareTo(b);
        });

        return files;
    }

    /**
     * 获取指定语言的 AI 翻译结果汇总文件路径
     */
    public Path getAiResultFile(String targetLanguage) {
        return pathHelper.getAiResultFile(targetLanguage);
    }

    /**
     * 归档已处理的 AI 翻译文件
     */
    public void archiveFiles(List<Path> files) throws IOException {
        Path archiveDir = pathHelper.getArchiveAiDir();
        Files.createDirectories(archiveDir);

        String timestamp = LocalDateTime.now().format(ARCHIVE_DATE_FORMAT);

        for (Path file : files) {
            if (Files.exists(file)) {
                String fileName = file.getFileName().toString();
                Path dest = archiveDir.resolve(timestamp + "_" + fileName);
                Files.move(file, dest, StandardCopyOption.REPLACE_EXISTING);
                logger.debug("Archived: " + fileName + " -> " + dest.getFileName());
            }
        }
    }

    /**
     * 归档并清空 AI 翻译结果汇总文件
     */
    public void archiveResultFile(String targetLanguage) throws IOException {
        Path resultFile = getAiResultFile(targetLanguage);
        if (Files.exists(resultFile) && Files.size(resultFile) > 0) {
            Path archiveDir = pathHelper.getArchiveAiDir();
            Files.createDirectories(archiveDir);

            String timestamp = LocalDateTime.now().format(ARCHIVE_DATE_FORMAT);
            String newName = timestamp + "_AItranslationResult_" + targetLanguage + ".txt";
            Path dest = archiveDir.resolve(newName);

            Files.copy(resultFile, dest, StandardCopyOption.REPLACE_EXISTING);
            Files.write(resultFile, new byte[0]);
            logger.debug("Archived and cleared AItranslationResult_" + targetLanguage + ".txt");
        }
    }

    /**
     * 保存解析失败的片段到 failed_pastes 目录
     */
    public void saveFailedPaste(String content, String targetLanguage) throws IOException {
        Path failedDir = pathHelper.getFailedPastesDir();
        Files.createDirectories(failedDir);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path failedFile = failedDir.resolve("failed_" + targetLanguage + "_" + timestamp + ".txt");
        Files.write(failedFile, content.getBytes(StandardCharsets.UTF_8));
        logger.warn("Saved failed paste to: " + failedFile.getFileName());
    }
}
