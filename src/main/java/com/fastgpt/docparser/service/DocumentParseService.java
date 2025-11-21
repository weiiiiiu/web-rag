package com.fastgpt.docparser.service;

import com.fastgpt.docparser.config.FileProperties;
import com.fastgpt.docparser.dto.ParseResult;
import com.fastgpt.docparser.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 文档解析主服务
 */
@Service
public class DocumentParseService {

    private static final Logger log = LoggerFactory.getLogger(DocumentParseService.class);

    private final MinerUDocParserService minerUDocParserService;
    private final MarkdownProcessService markdownProcessService;
    private final HistoryService historyService;
    private final FileProperties fileProperties;

    public DocumentParseService(
            MinerUDocParserService minerUDocParserService,
            MarkdownProcessService markdownProcessService,
            HistoryService historyService,
            FileProperties fileProperties) {
        this.minerUDocParserService = minerUDocParserService;
        this.markdownProcessService = markdownProcessService;
        this.historyService = historyService;
        this.fileProperties = fileProperties;

        // 确保目录存在
        initDirectories();
    }

    /**
     * 解析文档
     *
     * @param file 上传的文件
     * @return 解析结果
     */
    public ParseResult parseDocument(MultipartFile file) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 验证文件
            validateFile(file);

            // 2. 保存到临时目录
            Path tmpFilePath = saveToTmpDir(file);
            log.info("文件保存到临时目录: {}", tmpFilePath);

            // 3. 调用 MinerU 解析文档
            log.info("开始调用 MinerU 解析文档...");
            MinerUDocParserService.ParseResult parseResult = minerUDocParserService.parseToMarkdown(tmpFilePath);

            // 4. 处理 Markdown：上传本地图片到 GitHub、替换链接
            log.info("开始处理 Markdown 图片...");
            MarkdownProcessService.ProcessedMarkdown processed =
                    markdownProcessService.processMarkdownWithLocalImages(
                            parseResult.markdownContent,
                            parseResult.extractDir);

            // 5. 保存最终结果到 results 目录
            Path resultFilePath = saveToResultDir(file.getOriginalFilename(), processed.content);
            log.info("结果保存到: {}", resultFilePath);

            // 6. 清理临时文件
            Files.deleteIfExists(tmpFilePath);
            // 清理 MinerU 解压目录
            cleanupDirectory(parseResult.extractDir);

            // 7. 构建返回结果
            long processingTime = System.currentTimeMillis() - startTime;

            ParseResult result = ParseResult.builder()
                    .originalFilename(file.getOriginalFilename())
                    .markdownContent(processed.content)
                    .imageCount(processed.imageUrls.size())
                    .imageUrls(processed.imageUrls)
                    .resultFilePath(resultFilePath.toString())
                    .processingTime(processingTime)
                    .build();

            // 8. 保存到历史记录
            historyService.addHistory(result);

            return result;

        } catch (Exception e) {
            log.error("文档解析失败", e);
            throw new BusinessException("文档解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new BusinessException("文件名不能为空");
        }

        // 检查文件类型
        String extension = getFileExtension(originalFilename);
        if (!fileProperties.isAllowedType(extension)) {
            throw new BusinessException("不支持的文件类型，仅支持: " + fileProperties.getAllowedTypesString());
        }

        // 检查文件大小
        long maxSizeBytes = (long) fileProperties.getMaxSize() * 1024 * 1024;
        if (file.getSize() > maxSizeBytes) {
            throw new BusinessException("文件大小超出限制，最大支持 " + fileProperties.getMaxSize() + "MB");
        }
    }

    /**
     * 保存文件到临时目录
     */
    private Path saveToTmpDir(MultipartFile file) throws IOException {
        // 获取绝对路径
        Path tmpDir = Paths.get(fileProperties.getTmpDir()).toAbsolutePath();

        // 确保目录存在
        if (!Files.exists(tmpDir)) {
            Files.createDirectories(tmpDir);
            log.info("创建临时目录: {}", tmpDir);
        }

        String fileName = generateFileName(file.getOriginalFilename());
        Path filePath = tmpDir.resolve(fileName);

        // 保存文件
        file.transferTo(filePath.toFile());

        log.debug("文件已保存到: {}", filePath);

        return filePath;
    }

    /**
     * 保存 Markdown 到结果目录
     */
    private Path saveToResultDir(String originalFilename, String markdownContent) throws IOException {
        // 获取绝对路径
        Path resultDir = Paths.get(fileProperties.getResultDir()).toAbsolutePath();

        // 确保目录存在
        if (!Files.exists(resultDir)) {
            Files.createDirectories(resultDir);
            log.info("创建结果目录: {}", resultDir);
        }

        String fileName = generateResultFileName(originalFilename);
        Path filePath = resultDir.resolve(fileName);

        // 保存 Markdown 文件
        Files.writeString(filePath, markdownContent);

        log.debug("结果已保存到: {}", filePath);

        return filePath;
    }

    /**
     * 生成唯一文件名
     */
    private String generateFileName(String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String extension = getFileExtension(originalFilename);
        String baseName = getBaseName(originalFilename);

        return String.format("%s_%s.%s", baseName, timestamp, extension);
    }

    /**
     * 生成结果文件名
     */
    private String generateResultFileName(String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String baseName = getBaseName(originalFilename);

        return String.format("%s_%s.md", baseName, timestamp);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * 获取文件基本名（不含扩展名）
     */
    private String getBaseName(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(0, dotIndex);
        }
        return filename;
    }

    /**
     * 初始化目录
     */
    private void initDirectories() {
        try {
            Path tmpDir = Paths.get(fileProperties.getTmpDir()).toAbsolutePath();
            Path resultDir = Paths.get(fileProperties.getResultDir()).toAbsolutePath();

            Files.createDirectories(tmpDir);
            Files.createDirectories(resultDir);

            log.info("目录初始化成功:");
            log.info("  临时目录: {}", tmpDir);
            log.info("  结果目录: {}", resultDir);
        } catch (IOException e) {
            log.error("目录初始化失败", e);
            throw new BusinessException("目录初始化失败", e);
        }
    }

    /**
     * 递归清理目录
     */
    private void cleanupDirectory(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }

        try {
            Files.walk(directory)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("删除文件失败: {}", path, e);
                        }
                    });
            log.debug("清理目录成功: {}", directory);
        } catch (IOException e) {
            log.warn("清理目录失败: {}", directory, e);
        }
    }
}
