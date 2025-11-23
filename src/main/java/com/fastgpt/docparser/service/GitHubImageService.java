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
import java.security.MessageDigest;
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
     * 上传图片字节数组到 GitHub
     *
     * @param imageBytes 图片字节数据
     * @param knowledgeBaseId 知识库ID
     * @param documentId 文档ID
     * @param fileName 文件名
     * @return CDN 图片链接
     */
    public String uploadImageBytes(byte[] imageBytes, String knowledgeBaseId, String documentId, String fileName) throws IOException {
        String base64Content = Base64.getEncoder().encodeToString(imageBytes);

        // 使用内容哈希生成文件名，相同内容的图片会得到相同的文件名，自动去重
        String hashFileName = generateHashFileName(imageBytes, fileName);
        
        // 新的路径结构: {pathPrefix}{知识库ID}/{文档ID}/{图片名}
        String filePath = String.format("%s%s/%s/%s", 
                gitHubProperties.getPathPrefix(), 
                sanitizePath(knowledgeBaseId), 
                sanitizePath(documentId), 
                hashFileName);

        // 构建 GitHub API URL
        String apiUrl = String.format("%s/repos/%s/contents/%s",
                gitHubProperties.getApiBaseUrl(),
                gitHubProperties.getRepo(),
                filePath);

        // 构建请求体
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("message", "Upload image: " + hashFileName);
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
            String relativePath = String.format("%s/%s/%s", 
                    sanitizePath(knowledgeBaseId), 
                    sanitizePath(documentId), 
                    hashFileName);
            String cdnUrl = gitHubProperties.getCdnBaseUrl() + relativePath;
            log.info("图片上传成功: {} -> {}", fileName, cdnUrl);
            return cdnUrl;
        }
    }

    /**
     * 清理路径，去除特殊字符
     */
    private String sanitizePath(String path) {
        if (path == null) {
            return "default";
        }
        // 只保留字母、数字、中文、下划线、连字符
        return path.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]", "_");
    }

    /**
     * 上传图片字节数组到 GitHub（旧方法，保留兼容性）
     *
     * @param imageBytes 图片字节数据
     * @param fileName 文件名
     * @return CDN 图片链接
     */
    public String uploadImageBytes(byte[] imageBytes, String fileName) throws IOException {
        String base64Content = Base64.getEncoder().encodeToString(imageBytes);

        // 使用内容哈希生成文件名，相同内容的图片会得到相同的文件名，自动去重
        String uniqueFileName = generateHashFileName(imageBytes, fileName);
        String filePath = gitHubProperties.getPathPrefix() + uniqueFileName;

        // 构建 GitHub API URL
        String apiUrl = String.format("%s/repos/%s/contents/%s",
                gitHubProperties.getApiBaseUrl(),
                gitHubProperties.getRepo(),
                filePath);

        // 构建请求体
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("message", "Upload image: " + uniqueFileName);
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
            String cdnUrl = gitHubProperties.getCdnBaseUrl() + uniqueFileName;
            log.info("图片上传成功: {} -> {}", fileName, cdnUrl);
            return cdnUrl;
        }
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
        return uploadImageBytes(imageBytes, originalName);
    }

    /**
     * 上传图片到 GitHub（旧实现，保留兼容性）
     *
     * @param imagePath 图片文件路径
     * @param originalName 原始文件名
     * @return CDN 图片链接
     */
    private String uploadImageOld(Path imagePath, String originalName) throws IOException {
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
     * 基于图片内容哈希生成文件名
     * 
     * 原理：使用 SHA-256 算法计算图片内容的指纹
     * 相同内容的图片 → 相同哈希值 → 相同文件名 → 自动去重
     */
    private String generateHashFileName(byte[] imageBytes, String originalName) {
        try {
            // 计算 SHA-256 哈希值
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(imageBytes);
            
            // 转换为 16 进制字符串（取前 16 位）
            StringBuilder hexHash = new StringBuilder();
            for (int i = 0; i < Math.min(16, hashBytes.length); i++) {
                String hex = Integer.toHexString(0xff & hashBytes[i]);
                if (hex.length() == 1) hexHash.append('0');
                hexHash.append(hex);
            }
            
            // 保留原始文件扩展名
            String extension = "";
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = originalName.substring(dotIndex);
            } else {
                extension = ".png";
            }
            
            return hexHash.toString() + extension;
            
        } catch (Exception e) {
            log.warn("生成哈希文件名失败，使用随机文件名: {}", e.getMessage());
            return generateFileName(originalName);
        }
    }

    /**
     * 生成唯一文件名（降级方案）
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

    /**
     * 删除整个文档文件夹
     * 删除路径: {pathPrefix}{知识库ID}/{文档ID}/ 下的所有文件
     * 
     * 注意：GitHub API 不支持直接删除文件夹，需要逐个删除文件
     * 为了简化实现，这里只记录日志，实际删除需要列出所有文件后逐个删除
     * 
     * @param knowledgeBaseId 知识库ID
     * @param documentId 文档ID
     */
    public void deleteDocumentFolder(String knowledgeBaseId, String documentId) {
        String folderPath = String.format("%s%s/%s", 
                gitHubProperties.getPathPrefix(), 
                sanitizePath(knowledgeBaseId), 
                sanitizePath(documentId));
        
        log.warn("GitHub 不支持直接删除文件夹，请手动删除: {}", folderPath);
        log.warn("或者实现列举文件夹内容后逐个删除的逻辑");
        
        // TODO: 实现列举文件夹内容并逐个删除的逻辑
        // 1. 使用 GitHub API 列出该路径下的所有文件
        // 2. 逐个调用 DELETE API 删除文件
        // 由于 GitHub API 限制较多，这里暂时只记录日志
    }
}
