package com.fastgpt.docparser.service;

import com.fastgpt.docparser.config.DocumentProperties;
import com.fastgpt.docparser.config.FileProperties;
import com.fastgpt.docparser.dto.ParseResult;
import com.fastgpt.docparser.dto.ParserResult;
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

    private final DocumentProperties documentProperties;
    private final MinerUDocParserService minerUDocParserService;
    private final AliyunDocParserService aliyunDocParserService;
    private final MarkdownProcessService markdownProcessService;
    private final FileProperties fileProperties;
    private final DocumentHistoryService documentHistoryService;

    /**
     * 获取知识库 ID
     * 
     * TODO: 后续可以从以下来源获取:
     * - 请求参数 (MultipartFile metadata 或单独参数)
     * - 请求头 (X-Knowledge-Base-Id)
     * - 配置文件 (application.yml)
     * - 用户认证信息 (从 JWT token 或 session)
     * 
     * @return 知识库 ID
     */
    private String getKnowledgeBaseId() {
        // TODO: 实现动态获取逻辑
        // 示例: return request.getHeader("X-Knowledge-Base-Id");
        // 示例: return documentProperties.getDefaultKnowledgeBaseId();
        return "测试知识库";
    }

    /**
     * 构造函数
     *
     * @param documentProperties 文档解析配置
     * @param minerUDocParserService MinerU 解析服务
     * @param aliyunDocParserService 阿里云解析服务
     * @param markdownProcessService Markdown 处理服务
     * @param fileProperties 文件配置
     * @param documentHistoryService 文档历史服务
     */
    public DocumentParseService(
            DocumentProperties documentProperties,
            MinerUDocParserService minerUDocParserService,
            AliyunDocParserService aliyunDocParserService,
            MarkdownProcessService markdownProcessService,
            FileProperties fileProperties,
            DocumentHistoryService documentHistoryService) {
        this.documentProperties = documentProperties;
        this.minerUDocParserService = minerUDocParserService;
        this.aliyunDocParserService = aliyunDocParserService;
        this.markdownProcessService = markdownProcessService;
        this.fileProperties = fileProperties;
        this.documentHistoryService = documentHistoryService;

        // 确保目录存在
        initDirectories();
    }

    /**
     * 解析文档为 Markdown
     * 
     * 工作流程:
     * 1. 验证文件类型和大小
     * 2. 保存到临时目录
     * 3. 根据配置选择解析器(阿里云/MinerU)
     * 4. 清理临时文件
     * 5. 返回解析结果
     *
     * @param file 上传的文档文件
     * @return 解析结果,包含 Markdown 内容、图片数量、处理时间等
     * @throws BusinessException 文件验证失败或解析失败时抛出
     */
    public ParseResult parseDocument(MultipartFile file) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 验证文件
            validateFile(file);

            // 2. 保存到临时目录
            Path tmpFilePath = saveToTmpDir(file);
            log.info("文件保存到临时目录: {}", tmpFilePath);

            String markdownContent;
            Path extractDir = null;
            int imageCount = 0;

            // 3. 根据配置选择解析器
            if (documentProperties.isAliyun()) {
                // 使用阿里云解析器
                log.info("使用阿里云文档解析服务...");
                String knowledgeBaseId = getKnowledgeBaseId();
                String documentId = sanitizeDocumentId(file.getOriginalFilename());

                ParserResult aliyunResult =
                        aliyunDocParserService.parseToMarkdown(tmpFilePath, knowledgeBaseId, documentId);

                markdownContent = aliyunResult.getMarkdownContent();
                extractDir = aliyunResult.getExtractDir();

                // 统计图片数量
                imageCount = countMarkdownImages(markdownContent);

            } else {
                // 使用 MinerU 解析器
                log.info("使用 MinerU 文档解析服务...");
                String knowledgeBaseId = getKnowledgeBaseId();
                String documentId = sanitizeDocumentId(file.getOriginalFilename());
                
                ParserResult mineruResult =
                        minerUDocParserService.parseToMarkdown(tmpFilePath);

                // 处理 Markdown：上传图片、替换链接
                log.info("开始处理 Markdown 图片...");
                MarkdownProcessService.ProcessedMarkdown processed =
                        markdownProcessService.processMarkdownImages(
                                mineruResult.getMarkdownContent(),
                                mineruResult.getExtractDir(),
                                knowledgeBaseId,
                                documentId);

                markdownContent = processed.content;
                extractDir = mineruResult.getExtractDir();
                imageCount = processed.imageUrls.size();
            }

            // 4. 清理临时文件
            Files.deleteIfExists(tmpFilePath);
            if (extractDir != null) {
                cleanupDirectory(extractDir);
            }

            // 5. 构建返回结果
            long processingTime = System.currentTimeMillis() - startTime;
            String knowledgeBaseId = getKnowledgeBaseId();
            String documentId = sanitizeDocumentId(file.getOriginalFilename());

            ParseResult result = ParseResult.builder()
                    .originalFilename(file.getOriginalFilename())
                    .markdownContent(markdownContent)
                    .imageCount(imageCount)
                    .imageUrls(java.util.Collections.emptyList())
                    .resultFilePath(null)  // 不再保存文件
                    .processingTime(processingTime)
                    .build();

            // 6. 保存历史记录到数据库
            try {
                documentHistoryService.saveHistory(
                        file.getOriginalFilename(),
                        documentId,
                        knowledgeBaseId,
                        markdownContent,
                        imageCount,
                        processingTime,
                        documentProperties.getImageStorage()
                );
                log.info("解析历史已保存到数据库");
            } catch (Exception e) {
                log.error("保存解析历史失败，但不影响主流程", e);
            }

            return result;

        } catch (Exception e) {
            log.error("文档解析失败", e);
            throw new BusinessException("文档解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 统计 Markdown 中的图片数量
     * 
     * @param markdown Markdown 内容
     * @return 图片数量
     */
    private int countMarkdownImages(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = markdown.indexOf("![", index)) != -1) {
            count++;
            index += 2;
        }
        return count;
    }

    /**
     * 验证上传的文件
     * 
     * 检查项:
     * - 文件是否为空
     * - 文件名是否有效
     * - 文件类型是否在允许列表中
     * - 文件大小是否超出限制
     *
     * @param file 上传的文件
     * @throws BusinessException 验证失败时抛出
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
     * 保存上传文件到临时目录
     * 
     * 生成带时间戳的唯一文件名,避免冲突
     *
     * @param file 上传的文件
     * @return 保存后的文件路径
     * @throws IOException 文件保存失败时抛出
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
     * 生成唯一文件名
     * 
     * 格式: {原文件名}_{时间戳}.{扩展名}
     * 例如: report_20241123_143025.pdf
     *
     * @param originalFilename 原始文件名
     * @return 生成的唯一文件名
     */
    private String generateFileName(String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String extension = getFileExtension(originalFilename);
        String baseName = getBaseName(originalFilename);

        return String.format("%s_%s.%s", baseName, timestamp, extension);
    }


    /**
     * 获取文件扩展名(小写)
     *
     * @param filename 文件名
     * @return 文件扩展名,不包含点号,如果没有扩展名则返回空字符串
     */
    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * 获取文件基本名(不含扩展名)
     *
     * @param filename 文件名
     * @return 文件基本名
     */
    private String getBaseName(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(0, dotIndex);
        }
        return filename;
    }

    /**
     * 清理文件名,生成安全的 documentId
     * 
     * 处理规则:
     * - 去掉文件扩展名
     * - 保留字母、数字、中文、下划线、连字符
     * - 替换空格和其他特殊字符为下划线
     * - 限制长度为 50 字符避免 URL 过长
     * 
     * 示例: "2024年报告 (最终版).pdf" → "2024年报告_最终版"
     *
     * @param originalFilename 原始文件名
     * @return 清理后的安全 ID,用于 OSS 存储路径
     */
    private String sanitizeDocumentId(String originalFilename) {
        // 获取文件基本名（不含扩展名）
        String baseName = getBaseName(originalFilename);

        if (baseName == null || baseName.isEmpty()) {
            return "doc-" + System.currentTimeMillis();
        }

        // 替换空格和特殊字符为下划线，保留字母、数字、中文、下划线、连字符
        String sanitized = baseName
            .replaceAll("[\\s]+", "_")  // 空格替换为下划线
            .replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]", "_")  // 其他特殊字符替换为下划线
            .replaceAll("_+", "_")  // 多个连续下划线合并为一个
            .replaceAll("^_+|_+$", "");  // 去掉首尾下划线

        // 限制长度为 50 个字符
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }

        // 如果清理后为空，使用时间戳
        if (sanitized.isEmpty()) {
            return "doc-" + System.currentTimeMillis();
        }

        return sanitized;
    }

    /**
     * 初始化必要的目录结构
     * 
     * 在服务启动时创建临时文件目录
     *
     * @throws BusinessException 目录创建失败时抛出
     */
    private void initDirectories() {
        try {
            Path tmpDir = Paths.get(fileProperties.getTmpDir()).toAbsolutePath();

            Files.createDirectories(tmpDir);

            log.info("目录初始化成功:");
            log.info("  临时目录: {}", tmpDir);
        } catch (IOException e) {
            log.error("目录初始化失败", e);
            throw new BusinessException("目录初始化失败", e);
        }
    }

    /**
     * 递归清理目录及其所有内容
     * 
     * 用于清理 MinerU 解压的临时文件
     *
     * @param directory 要清理的目录路径
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
