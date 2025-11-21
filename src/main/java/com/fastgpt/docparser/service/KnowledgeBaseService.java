package com.fastgpt.docparser.service;

import com.aliyun.bailian20231229.models.*;
import com.aliyun.teautil.models.RuntimeOptions;
import com.fastgpt.docparser.dto.KnowledgeBaseDTO;
import com.fastgpt.docparser.exception.BusinessException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库管理服务
 * 负责知识库的创建、文档上传、列表查询、删除等操作
 *
 * @author ZHONG WEI
 */
@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);
    private static final String PARSER_TYPE = "DASHSCOPE_DOCMIND";

    private final AliyunBailianService bailianService;
    private final MinerUDocParserService minerUDocParserService;
    private final OkHttpClient httpClient;

    public KnowledgeBaseService(AliyunBailianService bailianService,
                                MinerUDocParserService minerUDocParserService) {
        this.bailianService = bailianService;
        this.minerUDocParserService = minerUDocParserService;
        this.httpClient = new OkHttpClient();
    }

    /**
     * 创建知识库
     */
    public KnowledgeBaseDTO createKnowledgeBase(String name, String description) {
        log.info("开始创建知识库: {}", name);
        log.info("使用 Workspace ID: {}", bailianService.getWorkspaceId());

        try {
            // 创建知识库请求
            // SinkType 是必需参数，使用 BUILT_IN 表示使用内置存储
            CreateIndexRequest request = new CreateIndexRequest()
                    .setStructureType("unstructured")
                    .setName(name)
                    .setDescription(description)
                    .setSinkType("BUILT_IN");

            log.debug("创建知识库请求参数: name={}, structureType={}, sinkType={}",
                name, "unstructured", "BUILT_IN");

            CreateIndexResponse response = bailianService.getClient().createIndexWithOptions(
                    bailianService.getWorkspaceId(),
                    request,
                    new HashMap<>(),
                    new RuntimeOptions()
            );

            // 添加响应日志用于调试
            log.debug("创建知识库响应: {}", response);

            // 检查响应是否为空
            if (response == null || response.getBody() == null) {
                log.error("创建知识库失败：API 返回空响应");
                throw new BusinessException("创建知识库失败：API 返回空响应");
            }

            // 记录响应信息
            log.info("HTTP 状态码: {}", response.getStatusCode());
            log.info("请求 ID: {}", response.getBody().getRequestId());
            if (response.getBody().getCode() != null) {
                log.warn("响应 Code: {}", response.getBody().getCode());
            }
            if (response.getBody().getMessage() != null) {
                log.warn("响应 Message: {}", response.getBody().getMessage());
            }

            // 检查响应状态码
            if (response.getStatusCode() != null && response.getStatusCode() != 200) {
                log.error("创建知识库失败：HTTP 状态码 {}", response.getStatusCode());
                String errorMsg = String.format("创建知识库失败：HTTP 状态码 %d", response.getStatusCode());
                if (response.getBody().getMessage() != null) {
                    errorMsg += ", 错误信息: " + response.getBody().getMessage();
                }
                throw new BusinessException(errorMsg);
            }

            // 检查 data 字段
            if (response.getBody().getData() == null) {
                log.error("创建知识库失败：响应 data 字段为空");
                log.error("完整响应体: {}", response.getBody());
                log.error("请求 ID: {}", response.getBody().getRequestId());
                
                // 构建详细的错误信息
                StringBuilder errorMsg = new StringBuilder("创建知识库失败：API 返回的 data 字段为空。\n");
                errorMsg.append("可能的原因：\n");
                errorMsg.append("1. Workspace ID 配置错误或不存在\n");
                errorMsg.append("2. AccessKey 没有操作该 Workspace 的权限\n");
                errorMsg.append("3. 百炼服务未开通或已过期\n");
                errorMsg.append("\n当前配置：\n");
                errorMsg.append("- Workspace ID: ").append(bailianService.getWorkspaceId()).append("\n");
                errorMsg.append("- Endpoint: ").append(bailianService.getProperties().getEndpoint()).append("\n");
                if (response.getBody().getCode() != null) {
                    errorMsg.append("- 错误码: ").append(response.getBody().getCode()).append("\n");
                }
                if (response.getBody().getMessage() != null) {
                    errorMsg.append("- 错误信息: ").append(response.getBody().getMessage()).append("\n");
                }
                errorMsg.append("- 请求 ID: ").append(response.getBody().getRequestId());
                
                throw new BusinessException(errorMsg.toString());
            }

            // 获取知识库 ID
            String indexId = response.getBody().getData().getId();
            if (indexId == null || indexId.isEmpty()) {
                log.error("创建知识库失败：返回的 ID 为空");
                throw new BusinessException("创建知识库失败：返回的知识库 ID 为空");
            }

            log.info("知识库创建成功，ID: {}", indexId);

            return KnowledgeBaseDTO.builder()
                    .id(indexId)
                    .name(name)
                    .description(description)
                    .createdAt(System.currentTimeMillis())
                    .status("CREATED")
                    .build();

        } catch (Exception e) {
            log.error("创建知识库失败", e);
            throw new BusinessException("创建知识库失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传文档到知识库（先转 Markdown）
     */
    public String uploadDocument(String indexId, MultipartFile file) {
        log.info("开始上传文档到知识库 {}: {}", indexId, file.getOriginalFilename());

        try {
            // Step 1: 使用 MinerU 将文档转换为 Markdown
            log.info("Step 1: 调用 MinerU 转换文档为 Markdown...");
            Path tmpFilePath = saveToTmpDir(file);
            MinerUDocParserService.ParseResult parseResult = minerUDocParserService.parseToMarkdown(tmpFilePath);

            // parseResult.markdownContent 已经是处理过图片并上传到 GitHub 的版本
            // 保存处理后的 Markdown 文件
            String markdownFilename = file.getOriginalFilename().replaceFirst("\\.[^.]+$", ".md");
            Path markdownPath = tmpFilePath.getParent().resolve(markdownFilename);
            Files.writeString(markdownPath, parseResult.markdownContent);
            log.info("Markdown 文件已保存（图片已转为 GitHub CDN 链接）: {}", markdownPath);

            // Step 2: 申请文件上传租约
            log.info("Step 2: 申请文件上传租约...");
            ApplyFileUploadLeaseResponse leaseResponse = applyFileUploadLease(markdownFilename, markdownPath);
            log.info("租约申请成功，LeaseId: {}", leaseResponse.getBody().getData().getFileUploadLeaseId());

            // Step 3: 上传 Markdown 文件到阿里云
            log.info("Step 3: 上传 Markdown 文件到阿里云...");
            uploadFileToAliyun(leaseResponse, markdownPath);

            // Step 4: 添加文件到类目
            log.info("Step 4: 添加文件到类目...");
            String fileId = addFileToCategory(leaseResponse);
            log.info("文件已添加到类目，FileId: {}", fileId);

            // Step 5: 等待文件解析完成
            log.info("Step 5: 等待文件解析完成...");
            waitForFileParsing(fileId);

            // Step 6: 追加文件到知识库
            log.info("Step 6: 追加文件到知识库...");
            String jobId = submitIndexAddDocumentsJob(indexId, fileId);
            log.info("索引任务已提交，JobId: {}", jobId);

            // Step 7: 等待索引任务完成
            log.info("Step 7: 等待索引构建完成...");
            waitForIndexJobCompletion(indexId, jobId);

            // 清理临时文件
            Files.deleteIfExists(tmpFilePath);
            Files.deleteIfExists(markdownPath);
            log.info("临时文件已清理");

            log.info("文档上传成功，FileId: {}", fileId);
            return fileId;

        } catch (Exception e) {
            log.error("上传文档失败", e);
            throw new BusinessException("上传文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 申请文件上传租约（返回租约响应）
     */
    private ApplyFileUploadLeaseResponse applyFileUploadLease(String filename, Path filePath) throws Exception {
        byte[] fileBytes = Files.readAllBytes(filePath);
        String md5 = calculateMD5(fileBytes);

        ApplyFileUploadLeaseRequest request = new ApplyFileUploadLeaseRequest()
                .setFileName(filename)
                .setMd5(md5)
                .setSizeInBytes(String.valueOf(fileBytes.length));

        ApplyFileUploadLeaseResponse response = bailianService.getClient().applyFileUploadLeaseWithOptions(
                bailianService.getCategoryId(),
                bailianService.getWorkspaceId(),
                request,
                new HashMap<>(),
                new RuntimeOptions()
        );

        return response;
    }

    /**
     * 上传文件到阿里云临时存储
     */
    private void uploadFileToAliyun(ApplyFileUploadLeaseResponse leaseResponse, Path filePath) throws Exception {
        // 获取上传参数
        String uploadUrl = leaseResponse.getBody().getData().getParam().getUrl();
        Object headersObj = leaseResponse.getBody().getData().getParam().getHeaders();
        String xBailianExtra = "";
        String contentType = "";

        if (headersObj instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, String> headers = (java.util.Map<String, String>) headersObj;
            xBailianExtra = headers.getOrDefault("X-bailian-extra", "");
            contentType = headers.getOrDefault("Content-Type", "");
        }

        // 读取 Markdown 文件内容
        byte[] fileBytes = Files.readAllBytes(filePath);

        // 构建上传请求
        Request.Builder requestBuilder = new Request.Builder()
                .url(uploadUrl)
                .put(RequestBody.create(fileBytes, null))
                .addHeader("X-bailian-extra", xBailianExtra);

        if (contentType != null && !contentType.isEmpty()) {
            requestBuilder.addHeader("Content-Type", contentType);
        }

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Markdown 文件上传失败: " + response.code());
            }
            log.info("Markdown 文件上传成功");
        }
    }

    /**
     * 添加文件到类目
     */
    private String addFileToCategory(ApplyFileUploadLeaseResponse leaseResponse) throws Exception {
        AddFileRequest request = new AddFileRequest()
                .setLeaseId(leaseResponse.getBody().getData().getFileUploadLeaseId())
                .setParser(PARSER_TYPE)
                .setCategoryId(bailianService.getCategoryId());

        AddFileResponse response = bailianService.getClient().addFileWithOptions(
                bailianService.getWorkspaceId(),
                request,
                new HashMap<>(),
                new RuntimeOptions()
        );

        return response.getBody().getData().getFileId();
    }

    /**
     * 等待文件解析完成
     */
    private void waitForFileParsing(String fileId) throws Exception {
        int attempts = 0;
        int maxAttempts = bailianService.getProperties().getMaxPollingAttempts();

        while (attempts < maxAttempts) {
            DescribeFileResponse response = bailianService.getClient().describeFileWithOptions(
                    bailianService.getWorkspaceId(),
                    fileId,
                    new HashMap<>(),
                    new RuntimeOptions()
            );

            String status = response.getBody().getData().getStatus();
            log.debug("文件解析状态: {}", status);

            if ("PARSE_SUCCESS".equals(status)) {
                log.info("文件解析完成");
                return;
            } else if ("PARSE_FAILED".equals(status)) {
                throw new BusinessException("文件解析失败");
            }

            bailianService.waitPollingInterval();
            attempts++;
        }

        throw new BusinessException("文件解析超时");
    }

    /**
     * 提交索引追加文档任务
     */
    private String submitIndexAddDocumentsJob(String indexId, String fileId) throws Exception {
        SubmitIndexAddDocumentsJobRequest request = new SubmitIndexAddDocumentsJobRequest()
                .setIndexId(indexId)
                .setDocumentIds(List.of(fileId))
                .setSourceType("DATA_CENTER_FILE");

        SubmitIndexAddDocumentsJobResponse response = bailianService.getClient().submitIndexAddDocumentsJobWithOptions(
                bailianService.getWorkspaceId(),
                request,
                new HashMap<>(),
                new RuntimeOptions()
        );

        return response.getBody().getData().getId();
    }

    /**
     * 等待索引任务完成
     */
    private void waitForIndexJobCompletion(String indexId, String jobId) throws Exception {
        int attempts = 0;
        int maxAttempts = bailianService.getProperties().getMaxPollingAttempts();

        while (attempts < maxAttempts) {
            GetIndexJobStatusRequest request = new GetIndexJobStatusRequest()
                    .setIndexId(indexId)
                    .setJobId(jobId);

            GetIndexJobStatusResponse response = bailianService.getClient().getIndexJobStatusWithOptions(
                    bailianService.getWorkspaceId(),
                    request,
                    new HashMap<>(),
                    new RuntimeOptions()
            );

            String status = response.getBody().getData().getStatus();
            log.debug("索引任务状态: {}", status);

            if ("COMPLETED".equals(status)) {
                log.info("索引构建完成");
                return;
            } else if ("FAILED".equals(status)) {
                throw new BusinessException("索引构建失败");
            }

            bailianService.waitPollingInterval();
            attempts++;
        }

        throw new BusinessException("索引构建超时");
    }

    /**
     * 查询知识库列表
     */
    public List<KnowledgeBaseDTO> listKnowledgeBases() {
        try {
            ListIndicesRequest request = new ListIndicesRequest();
            ListIndicesResponse response = bailianService.getClient().listIndicesWithOptions(
                    bailianService.getWorkspaceId(),
                    request,
                    new HashMap<>(),
                    new RuntimeOptions()
            );

            // 检查响应数据是否为空
            if (response == null ||
                response.getBody() == null ||
                response.getBody().getData() == null ||
                response.getBody().getData().getIndices() == null) {

                log.info("业务空间下暂无知识库");
                return new ArrayList<>();
            }

            return response.getBody().getData().getIndices().stream()
                    .map(index -> KnowledgeBaseDTO.builder()
                            .id(index.getId())
                            .name(index.getName())
                            .description(index.getDescription())
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("查询知识库列表失败", e);
            throw new BusinessException("查询知识库列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除知识库
     */
    public void deleteKnowledgeBase(String indexId) {
        try {
            DeleteIndexRequest request = new DeleteIndexRequest().setIndexId(indexId);
            bailianService.getClient().deleteIndexWithOptions(
                    bailianService.getWorkspaceId(),
                    request,
                    new HashMap<>(),
                    new RuntimeOptions()
            );

            log.info("知识库删除成功: {}", indexId);

        } catch (Exception e) {
            log.error("删除知识库失败", e);
            throw new BusinessException("删除知识库失败: " + e.getMessage(), e);
        }
    }

    /**
     * 保存文件到临时目录
     */
    private Path saveToTmpDir(MultipartFile file) throws IOException {
        Path tmpDir = Path.of(System.getProperty("java.io.tmpdir"));
        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = tmpDir.resolve(filename);
        file.transferTo(filePath.toFile());
        return filePath;
    }

    /**
     * 计算文件 MD5
     */
    private String calculateMD5(byte[] data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new BusinessException("计算 MD5 失败", e);
        }
    }
}
