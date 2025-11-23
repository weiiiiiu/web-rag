package com.fastgpt.docparser.service;

import com.fastgpt.docparser.config.DocumentProperties;
import com.fastgpt.docparser.exception.BusinessException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 处理服务
 */
@Service
public class MarkdownProcessService {

    private static final Logger log = LoggerFactory.getLogger(MarkdownProcessService.class);

    private final DocumentProperties documentProperties;
    private final GitHubImageService gitHubImageService;
    private final AliyunOssImageService aliyunOssImageService;
    private final OkHttpClient httpClient;

    // 匹配 Markdown 图片语法：![alt](url)
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)");

    public MarkdownProcessService(
            DocumentProperties documentProperties,
            GitHubImageService gitHubImageService,
            AliyunOssImageService aliyunOssImageService) {
        this.documentProperties = documentProperties;
        this.gitHubImageService = gitHubImageService;
        this.aliyunOssImageService = aliyunOssImageService;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }



    /**
     * 处理 MinerU 返回的 Markdown 内容
     * 
     * MinerU 返回的 Markdown 包含:
     * - 远程 MinerU CDN 图片 URL (需下载后上传到配置的存储服务)
     * - 本地相对路径图片(如 images/xxx.jpg,需直接上传)
     * 
     * 处理流程:
     * 1. 遍历 Markdown 中的所有图片
     * 2. 远程图片: 下载到本地临时文件
     * 3. 本地图片: 直接读取
     * 4. 上传到存储服务(阿里云 OSS 或 GitHub)
     * 5. 替换 Markdown 中的图片链接为 CDN URL
     *
     * @param markdownContent 原始 Markdown 内容
     * @param extractDir MinerU 解压目录(包含本地图片文件)
     * @param knowledgeBaseId 知识库 ID
     * @param documentId 文档 ID
     * @return 处理后的 Markdown 内容和图片链接列表
     */
    public ProcessedMarkdown processMarkdownImages(
            String markdownContent, 
            Path extractDir, 
            String knowledgeBaseId, 
            String documentId) {
        log.info("开始处理 MinerU Markdown 内容,图片存储策略: {}", 
                documentProperties.isOssStorage() ? "阿里云 OSS" : "GitHub CDN");

        Map<String, String> imageCdnUrls = new HashMap<>();
        Matcher matcher = IMAGE_PATTERN.matcher(markdownContent);
        int imageIndex = 0;

        while (matcher.find()) {
            String alt = matcher.group(1);
            String imagePath = matcher.group(2);

            try {
                imageIndex++;
                String imageUrl;
                
                // 判断是远程 URL 还是本地路径
                if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                    // 远程 MinerU CDN 图片:下载后上传
                    log.debug("处理远程图片: {}", imagePath);
                    imageUrl = downloadAndUploadRemoteImage(imagePath, extractDir, knowledgeBaseId, documentId, imageIndex);
                } else {
                    // 本地图片:直接上传
                    log.debug("处理本地图片: {}", imagePath);
                    Path localPath = extractDir.resolve(imagePath);
                    if (Files.exists(localPath)) {
                        String fileName = localPath.getFileName().toString();
                        imageUrl = uploadImage(localPath, fileName, knowledgeBaseId, documentId, imageIndex);
                    } else {
                        log.warn("本地图片文件不存在: {}", localPath);
                        continue;
                    }
                }

                imageCdnUrls.put(imagePath, imageUrl);
                log.info("图片上传成功: {} -> {}", imagePath, imageUrl);
                
            } catch (Exception e) {
                log.error("处理图片失败: {}", imagePath, e);
                // 继续处理其他图片
            }
        }

        // 替换 Markdown 中的图片链接
        String processedMarkdown = replaceImageUrls(markdownContent, imageCdnUrls);

        log.info("Markdown 处理完成,上传了 {} 张图片", imageCdnUrls.size());

        return new ProcessedMarkdown(processedMarkdown, new ArrayList<>(imageCdnUrls.values()));
    }

    /**
     * 根据配置上传图片
     * 
     * @param imagePath 本地图片路径
     * @param fileName 原始文件名
     * @param knowledgeBaseId 知识库 ID
     * @param documentId 文档 ID
     * @param imageIndex 图片序号(用于生成统一的文件名)
     * @return 上传后的 CDN URL
     */
    private String uploadImage(
            Path imagePath, 
            String fileName, 
            String knowledgeBaseId, 
            String documentId, 
            int imageIndex) throws IOException {
        // 统一文件名格式: image_1.jpg, image_2.jpg
        String extension = fileName.substring(fileName.lastIndexOf('.'));
        String unifiedFileName = "image_" + imageIndex + extension;
        
        if (documentProperties.isOssStorage()) {
            // 上传到阿里云 OSS
            byte[] imageBytes = Files.readAllBytes(imagePath);
            return aliyunOssImageService.uploadImageBytes(imageBytes, knowledgeBaseId, documentId, unifiedFileName);
        } else {
            // 上传到 GitHub
            return gitHubImageService.uploadImage(imagePath, unifiedFileName);
        }
    }

    /**
     * 下载远程图片并上传
     * 
     * @param imageUrl 远程图片 URL
     * @param tmpDir 临时目录
     * @param knowledgeBaseId 知识库 ID
     * @param documentId 文档 ID
     * @param imageIndex 图片序号
     * @return 上传后的 CDN URL
     */
    private String downloadAndUploadRemoteImage(
            String imageUrl, 
            Path tmpDir, 
            String knowledgeBaseId, 
            String documentId, 
            int imageIndex) throws IOException {
        // 1. 下载图片到临时文件
        String fileName = "image_" + imageIndex + ".jpg";
        Path tempFile = tmpDir.resolve(fileName);
        
        Request request = new Request.Builder()
                .url(imageUrl)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("下载图片失败: HTTP " + response.code());
            }

            if (response.body() == null) {
                throw new IOException("图片内容为空");
            }

            // 保存到临时文件
            Files.write(tempFile, response.body().bytes());
        }

        // 2. 上传图片
        String uploadedUrl = uploadImage(tempFile, fileName, knowledgeBaseId, documentId, imageIndex);

        // 3. 删除临时文件
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException e) {
            log.warn("删除临时文件失败: {}", tempFile, e);
        }

        return uploadedUrl;
    }

    /**
     * 替换 Markdown 中的图片 URL
     */
    private String replaceImageUrls(String markdownContent, Map<String, String> imageCdnUrls) {
        String result = markdownContent;

        for (Map.Entry<String, String> entry : imageCdnUrls.entrySet()) {
            String originalUrl = entry.getKey();
            String cdnUrl = entry.getValue();

            // 替换所有出现的原始 URL
            result = result.replace(originalUrl, cdnUrl);
        }

        return result;
    }

    /**
     * 处理后的 Markdown
     */
    public static class ProcessedMarkdown {
        public final String content;
        public final List<String> imageUrls;

        public ProcessedMarkdown(String content, List<String> imageUrls) {
            this.content = content;
            this.imageUrls = imageUrls;
        }
    }
}
