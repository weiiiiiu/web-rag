package com.fastgpt.docparser.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.DeleteObjectsResult;
import com.aliyun.oss.model.ObjectMetadata;
import com.fastgpt.docparser.config.AliyunOssProperties;
import com.fastgpt.docparser.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 阿里云 OSS 图片上传服务
 */
@Service
public class AliyunOssImageService {

    private static final Logger log = LoggerFactory.getLogger(AliyunOssImageService.class);

    private final AliyunOssProperties ossProperties;
    private final OSS ossClient;

    public AliyunOssImageService(AliyunOssProperties ossProperties) {
        this.ossProperties = ossProperties;

        this.ossClient = new OSSClientBuilder().build(
            ossProperties.getEndpoint(),
            ossProperties.getAccessKeyId(),
            ossProperties.getAccessKeySecret()
        );

        log.info("阿里云 OSS 客户端初始化成功，Bucket: {}, Endpoint: {}",
            ossProperties.getBucketName(), ossProperties.getEndpoint());
    }

    public String uploadImageBytes(byte[] imageBytes, String knowledgeBaseId, String documentId, String fileName) {
        // 使用内容哈希生成文件名，相同内容的图片会得到相同的文件名，自动去重
        String hashFileName = generateHashFileName(imageBytes, fileName);
        String objectKey = buildObjectKey(knowledgeBaseId, documentId, hashFileName);
        return uploadToOss(imageBytes, objectKey, hashFileName);
    }

    /**
     * 基于图片内容哈希生成文件名
     * 
     * 原理：
     * 1. 使用 SHA-256 算法计算图片字节内容的哈希值
     * 2. 相同内容的图片会产生相同的哈希值（类似指纹）
     * 3. 哈希值作为文件名，实现自动去重
     * 
     * 示例：
     * - 图片A内容 → SHA-256 → "a3f5b9c2d8e1" → a3f5b9c2d8e1.png
     * - 图片B内容相同 → SHA-256 → "a3f5b9c2d8e1" → a3f5b9c2d8e1.png (覆盖)
     * - 图片C内容不同 → SHA-256 → "7e2a4b6c9f1d" → 7e2a4b6c9f1d.png (新文件)
     */
    private String generateHashFileName(byte[] imageBytes, String originalFileName) {
        try {
            // 计算图片内容的 SHA-256 哈希值
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(imageBytes);
            
            // 转换为 16 进制字符串（取前 16 位，32个字符）
            StringBuilder hexHash = new StringBuilder();
            for (int i = 0; i < Math.min(16, hashBytes.length); i++) {
                String hex = Integer.toHexString(0xff & hashBytes[i]);
                if (hex.length() == 1) hexHash.append('0');
                hexHash.append(hex);
            }
            
            // 保留原始文件扩展名
            String extension = "";
            int dotIndex = originalFileName.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = originalFileName.substring(dotIndex);
            } else {
                extension = ".png";
            }
            
            String hashFileName = hexHash.toString() + extension;
            log.info("生成哈希文件名: {} → {} (图片大小: {} bytes)", originalFileName, hashFileName, imageBytes.length);
            
            return hashFileName;
            
        } catch (Exception e) {
            log.warn("生成哈希文件名失败，使用原始文件名: {}", e.getMessage());
            return originalFileName;
        }
    }

    private String uploadToOss(byte[] imageBytes, String objectKey, String fileName) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(imageBytes.length);
            metadata.setContentType(getContentType(fileName));

            InputStream inputStream = new ByteArrayInputStream(imageBytes);
            ossClient.putObject(ossProperties.getBucketName(), objectKey, inputStream, metadata);

            String publicUrl = buildPublicUrl(objectKey);
            log.info("图片上传成功: {} -> {}", fileName, publicUrl);
            return publicUrl;

        } catch (Exception e) {
            log.error("上传图片到 OSS 失败: objectKey={}", objectKey, e);
            throw new BusinessException("上传图片到 OSS 失败: " + e.getMessage(), e);
        }
    }

    private String buildObjectKey(String knowledgeBaseId, String documentId, String fileName) {
        return String.format("%s/%s/%s", knowledgeBaseId, documentId, fileName);
    }

    private String buildPublicUrl(String objectKey) {
        if (ossProperties.getPublicUrl() != null && !ossProperties.getPublicUrl().isEmpty()) {
            String baseUrl = ossProperties.getPublicUrl();
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }
            return baseUrl + objectKey;
        }

        return String.format("https://%s.%s/%s",
            ossProperties.getBucketName(),
            ossProperties.getEndpoint(),
            objectKey);
    }

    private String getContentType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }

        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".png")) {
            return "image/png";
        } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerName.endsWith(".webp")) {
            return "image/webp";
        } else {
            return "application/octet-stream";
        }
    }

    @PreDestroy
    public void shutdown() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("阿里云 OSS 客户端已关闭");
        }
    }
}
