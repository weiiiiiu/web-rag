package com.fastgpt.docparser.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.docmind_api20220711.Client;
import com.aliyun.docmind_api20220711.models.*;
import com.aliyun.teaopenapi.models.Config;
import com.fastgpt.docparser.config.AliyunDocParserProperties;
import com.fastgpt.docparser.dto.ParserResult;
import com.fastgpt.docparser.exception.BusinessException;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 阿里云文档解析服务
 */
@Service
public class AliyunDocParserService {

    private static final Logger log = LoggerFactory.getLogger(AliyunDocParserService.class);

    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)");
    private static final Pattern OSS_URL_PATTERN = Pattern.compile("https?://[^/]*\\.aliyuncs\\.com/.*");

    private final AliyunDocParserProperties properties;
    private final AliyunOssImageService ossImageService;
    private final GitHubImageService gitHubImageService;
    private final com.fastgpt.docparser.config.DocumentProperties documentProperties;
    private final Client client;
    private final OkHttpClient httpClient;

    /**
     * 构造函数
     *
     * @param properties 阿里云文档解析配置
     * @param ossImageService OSS 图片服务
     * @param gitHubImageService GitHub 图片服务
     * @param documentProperties 文档配置属性
     */
    public AliyunDocParserService(AliyunDocParserProperties properties,
                                   AliyunOssImageService ossImageService,
                                   GitHubImageService gitHubImageService,
                                   com.fastgpt.docparser.config.DocumentProperties documentProperties) {
        this.properties = properties;
        this.ossImageService = ossImageService;
        this.gitHubImageService = gitHubImageService;
        this.documentProperties = documentProperties;
        this.client = createClient();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    /**
     * 创建阿里云文档解析客户端
     *
     * @return 阿里云 SDK 客户端实例
     * @throws BusinessException 创建客户端失败时抛出
     */
    private Client createClient() {
        try {
            com.aliyun.credentials.models.Config credConfig = new com.aliyun.credentials.models.Config();
            credConfig.setType("access_key");
            credConfig.setAccessKeyId(properties.getAccessKeyId());
            credConfig.setAccessKeySecret(properties.getAccessKeySecret());

            com.aliyun.credentials.Client credentialClient = new com.aliyun.credentials.Client(credConfig);

            Config config = new Config();
            config.setCredential(credentialClient);
            config.setEndpoint(properties.getEndpoint());
            config.setRegionId(properties.getRegionId());
            config.setConnectTimeout(60000);
            config.setReadTimeout(60000);

            return new Client(config);
        } catch (Exception e) {
            log.error("创建阿里云文档解析客户端失败", e);
            throw new BusinessException("创建阿里云文档解析客户端失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析文档为 Markdown 格式
     *
     * @param filePath 文档文件路径
     * @param knowledgeBaseId 知识库 ID
     * @param documentId 文档 ID(用于 OSS 存储路径)
     * @return 解析结果,包含 Markdown 内容
     * @throws BusinessException 解析失败时抛出
     */
    public ParserResult parseToMarkdown(Path filePath, String knowledgeBaseId, String documentId) {
        try {
            log.info("步骤 1: 提交阿里云文档解析任务...");
            String jobId = submitDocParserJob(filePath);
            log.info("任务提交成功，Job ID: {}", jobId);

            log.info("步骤 2: 轮询查询解析状态...");
            pollForCompletion(jobId);
            log.info("文档解析完成");

            log.info("步骤 3: 检索解析结果...");
            String markdownContent = retrieveResultsStreaming(jobId);
            log.info("成功检索到 Markdown 内容，长度: {}", markdownContent.length());

            log.info("步骤 4: 处理图片...");
            String processedMarkdown = downloadAndReuploadImages(markdownContent, knowledgeBaseId, documentId);
            int imageCount = countOssImages(markdownContent);
            log.info("成功处理 {} 张图片", imageCount);

            return ParserResult.ofAliyun(processedMarkdown);

        } catch (Exception e) {
            log.error("阿里云文档解析失败", e);
            throw new BusinessException("阿里云文档解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析文档为 Markdown 格式(使用默认知识库和文档 ID)
     *
     * @param filePath 文档文件路径
     * @return 解析结果,包含 Markdown 内容
     * @throws BusinessException 解析失败时抛出
     */
    public ParserResult parseToMarkdown(Path filePath) {
        String defaultKnowledgeBaseId = "测试知识库";
        String defaultDocumentId = "doc-" + System.currentTimeMillis();
        return parseToMarkdown(filePath, defaultKnowledgeBaseId, defaultDocumentId);
    }

    /**
     * 提交文档解析任务到阿里云
     *
     * @param filePath 文档文件路径
     * @return 任务 ID
     * @throws Exception 提交任务失败时抛出
     */
    private String submitDocParserJob(Path filePath) throws Exception {
        String fileName = filePath.getFileName().toString();

        SubmitDocParserJobAdvanceRequest request = new SubmitDocParserJobAdvanceRequest();
        request.fileName = fileName;
        request.fileUrlObject = Files.newInputStream(filePath);
        request.llmEnhancement = properties.isLlmEnhancement();

        if (properties.isLlmEnhancement() && properties.getEnhancementMode() != null
                && !properties.getEnhancementMode().trim().isEmpty()) {
            request.enhancementMode = properties.getEnhancementMode();
        }

        if (properties.isOutputHtmlTable()) {
            request.outputHtmlTable = true;
        }

        com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions();
        runtime.connectTimeout = 60000;
        runtime.readTimeout = 60000;

        SubmitDocParserJobResponse response = client.submitDocParserJobAdvance(request, runtime);

        if (response.getBody() == null || response.getBody().data == null) {
            String errorMsg = "提交文档解析任务失败：响应数据为空";
            if (response.getBody() != null) {
                errorMsg += String.format(" [Code: %s, Message: %s]",
                        response.getBody().code, response.getBody().message);
            }
            throw new BusinessException(errorMsg);
        }

        String jobId = response.getBody().data.id;
        if (jobId == null || jobId.isEmpty()) {
            throw new BusinessException("提交文档解析任务失败：未获取到 Job ID");
        }

        return jobId;
    }

    /**
     * 轮询查询文档解析任务状态直到完成
     *
     * @param jobId 任务 ID
     * @throws Exception 查询失败或任务失败时抛出
     * @throws BusinessException 超时或解析失败时抛出
     */
    private void pollForCompletion(String jobId) throws Exception {
        int attempts = 0;
        long pollingInterval = properties.getPollingInterval();
        int maxAttempts = properties.getMaxPollingAttempts();

        while (attempts < maxAttempts) {
            attempts++;

            QueryDocParserStatusRequest request = new QueryDocParserStatusRequest();
            request.id = jobId;

            QueryDocParserStatusResponse response = client.queryDocParserStatus(request);

            if (response.getBody() == null || response.getBody().data == null) {
                throw new BusinessException("查询文档解析状态失败：响应数据为空");
            }

            QueryDocParserStatusResponseBody.QueryDocParserStatusResponseBodyData data = response.getBody().data;
            String status = data.status;

            log.info("轮询第 {} 次，状态: {}", attempts, status);

            if ("success".equalsIgnoreCase(status)) {
                log.info("文档解析成功完成");
                return;
            } else if ("Fail".equalsIgnoreCase(status)) {
                throw new BusinessException("文档解析失败");
            }

            Thread.sleep(pollingInterval);
        }

        throw new BusinessException("文档解析超时：已轮询 " + maxAttempts + " 次");
    }

    /**
     * 流式检索文档解析结果
     *
     * @param jobId 任务 ID
     * @return 完整的 Markdown 内容
     * @throws Exception 检索失败时抛出
     */
    private String retrieveResultsStreaming(String jobId) throws Exception {
        StringBuilder markdownBuilder = new StringBuilder();
        Integer layoutNum = 0;
        Integer layoutStepSize = properties.getLayoutStepSize();
        boolean hasMore = true;

        while (hasMore) {
            GetDocParserResultRequest request = new GetDocParserResultRequest();
            request.id = jobId;
            request.layoutNum = layoutNum;
            request.layoutStepSize = layoutStepSize;

            GetDocParserResultResponse response = client.getDocParserResult(request);

            if (response.getBody() == null || response.getBody().data == null) {
                break;
            }

            Object dataObj = response.getBody().data;
            JSONObject jsonData;

            if (dataObj instanceof String) {
                jsonData = JSON.parseObject((String) dataObj);
            } else if (dataObj instanceof java.util.Map) {
                jsonData = new JSONObject((java.util.Map<String, Object>) dataObj);
            } else {
                jsonData = (JSONObject) JSON.toJSON(dataObj);
            }

            JSONArray layouts = jsonData.getJSONArray("layouts");

            if (layouts == null || layouts.isEmpty()) {
                break;
            }

            for (int i = 0; i < layouts.size(); i++) {
                JSONObject layout = layouts.getJSONObject(i);
                String markdownContent = layout.getString("markdownContent");

                if (markdownContent != null && !markdownContent.isEmpty()) {
                    markdownBuilder.append(markdownContent).append("\n\n");
                }
            }

            layoutNum += layouts.size();

            if (layouts.size() < layoutStepSize) {
                hasMore = false;
            }
        }

        return markdownBuilder.toString().trim();
    }

    /**
     * 下载阿里云临时图片并重新上传到 OSS
     *
     * @param markdownContent 原始 Markdown 内容
     * @param knowledgeBaseId 知识库 ID
     * @param documentId 文档 ID
     * @return 处理后的 Markdown 内容(图片链接已替换为 OSS 永久链接)
     */
    private String downloadAndReuploadImages(String markdownContent, String knowledgeBaseId, String documentId) {
        Matcher matcher = IMAGE_PATTERN.matcher(markdownContent);
        StringBuffer result = new StringBuffer();

        List<String> processedUrls = new ArrayList<>();
        int imageIndex = 0;

        while (matcher.find()) {
            String altText = matcher.group(1);
            String imageUrl = matcher.group(2);

            if (OSS_URL_PATTERN.matcher(imageUrl).matches() && !processedUrls.contains(imageUrl)) {
                try {
                    imageIndex++;
                    log.debug("下载临时图片: {}", imageUrl);
                    byte[] imageBytes = downloadImageBytes(imageUrl);
                    log.debug("图片下载完成，大小: {} bytes", imageBytes.length);
                    String fileName = String.format("image_%d%s", imageIndex, getFileExtension(imageUrl));
                    
                    // 根据配置选择图片存储服务
                    String uploadedUrl;
                    if (documentProperties.isOssStorage()) {
                        uploadedUrl = ossImageService.uploadImageBytes(imageBytes, knowledgeBaseId, documentId, fileName);
                    } else {
                        uploadedUrl = gitHubImageService.uploadImageBytes(imageBytes, fileName);
                    }

                    matcher.appendReplacement(result, "![" + altText + "](" + uploadedUrl + ")");
                    processedUrls.add(imageUrl);

                } catch (Exception e) {
                    log.error("处理图片失败: {}", imageUrl, e);
                    matcher.appendReplacement(result, "![" + altText + "](" + imageUrl + ")");
                }
            } else {
                matcher.appendReplacement(result, "![" + altText + "](" + imageUrl + ")");
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 下载图片字节数据
     *
     * @param imageUrl 图片 URL
     * @return 图片字节数据
     * @throws Exception 下载失败时抛出
     */
    private byte[] downloadImageBytes(String imageUrl) throws Exception {
        Request request = new Request.Builder()
                .url(imageUrl)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new BusinessException("下载图片失败: HTTP " + response.code());
            }

            if (response.body() == null) {
                throw new BusinessException("下载图片失败：响应体为空");
            }

            return response.body().bytes();
        }
    }

    /**
     * 从 URL 中提取文件扩展名
     *
     * @param url 图片 URL
     * @return 文件扩展名(如 .png、.jpg),默认返回 .png
     */
    private String getFileExtension(String url) {
        int questionMarkIndex = url.indexOf('?');
        String urlWithoutParams = questionMarkIndex > 0 ? url.substring(0, questionMarkIndex) : url;

        int lastDotIndex = urlWithoutParams.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < urlWithoutParams.length() - 1) {
            String ext = urlWithoutParams.substring(lastDotIndex);

            if (ext.matches("\\.[a-zA-Z0-9]{2,5}")) {
                return ext;
            }
        }

        return ".png";
    }

    /**
     * 统计 Markdown 中的 OSS 图片数量
     *
     * @param markdownContent Markdown 内容
     * @return OSS 图片数量
     */
    private int countOssImages(String markdownContent) {
        Matcher matcher = IMAGE_PATTERN.matcher(markdownContent);
        int count = 0;

        while (matcher.find()) {
            String imageUrl = matcher.group(2);
            if (OSS_URL_PATTERN.matcher(imageUrl).matches()) {
                count++;
            }
        }

        return count;
    }
}
