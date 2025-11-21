package com.fastgpt.docparser.service;

import com.fastgpt.docparser.config.GitHubProperties;
import com.fastgpt.docparser.exception.BusinessException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

/**
 * GitHub 图床上传服务
 */
@Service
public class GitHubImageService {

    private static final Logger log = LoggerFactory.getLogger(GitHubImageService.class);

    private final GitHubProperties gitHubProperties;
    private final OkHttpClient httpClient;
    private final Gson gson;

    public GitHubImageService(GitHubProperties gitHubProperties) {
        this.gitHubProperties = gitHubProperties;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    /**
     * 上传图片到 GitHub
     *
     * @param imagePath 图片文件路径
     * @param originalName 原始文件名
     * @return CDN 图片链接
     */
    public String uploadImage(Path imagePath, String originalName) throws IOException {
        // 读取图片文件
        byte[] imageBytes = Files.readAllBytes(imagePath);
        String base64Content = Base64.getEncoder().encodeToString(imageBytes);

        // 生成唯一文件名
        String fileName = generateFileName(originalName);
        String filePath = gitHubProperties.getPathPrefix() + fileName;

        // 构建 GitHub API URL
        String apiUrl = String.format("%s/repos/%s/contents/%s",
                gitHubProperties.getApiBaseUrl(),
                gitHubProperties.getRepo(),
                filePath);

        // 构建请求体
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("message", "Upload image: " + fileName);
        requestBody.addProperty("content", base64Content);
        requestBody.addProperty("branch", gitHubProperties.getBranch());

        RequestBody body = RequestBody.create(
                gson.toJson(requestBody),
                MediaType.parse("application/json; charset=utf-8")
        );

        // 构建请求
        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "token " + gitHubProperties.getToken())
                .addHeader("Accept", "application/vnd.github.v3+json")
                .put(body)
                .build();

        // 发送请求
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("GitHub API 上传失败: {} - {}", response.code(), errorBody);
                throw new BusinessException("图片上传到 GitHub 失败: " + response.code());
            }

            // 返回 CDN 链接
            String cdnUrl = gitHubProperties.getCdnBaseUrl() + fileName;
            log.info("图片上传成功: {} -> {}", originalName, cdnUrl);
            return cdnUrl;
        }
    }

    /**
     * 批量上传图片
     *
     * @param imagePaths 图片路径列表
     * @return CDN 链接列表
     */
    public java.util.List<String> uploadImages(java.util.List<Path> imagePaths) {
        java.util.List<String> cdnUrls = new java.util.ArrayList<>();

        for (Path imagePath : imagePaths) {
            try {
                String cdnUrl = uploadImage(imagePath, imagePath.getFileName().toString());
                cdnUrls.add(cdnUrl);
            } catch (IOException e) {
                log.error("上传图片失败: {}", imagePath, e);
                throw new BusinessException("上传图片失败: " + imagePath.getFileName());
            }
        }

        return cdnUrls;
    }

    /**
     * 生成唯一文件名
     * 格式：yyyyMMdd/uuid_原始文件名
     */
    private String generateFileName(String originalName) {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        // 保留原始文件扩展名
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalName.substring(dotIndex);
        }

        return String.format("%s/%s_%s%s", datePrefix, uuid, sanitizeFileName(originalName), extension);
    }

    /**
     * 清理文件名（移除特殊字符）
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "image";
        }

        // 移除扩展名
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileName = fileName.substring(0, dotIndex);
        }

        // 只保留字母、数字、下划线和连字符
        return fileName.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
