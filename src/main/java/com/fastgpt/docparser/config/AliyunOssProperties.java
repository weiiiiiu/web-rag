package com.fastgpt.docparser.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置属性
 * 用于上传文档解析后的图片到阿里云 OSS 存储
 */
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class AliyunOssProperties {

    /**
     * 阿里云 AccessKey ID
     */
    private String accessKeyId;

    /**
     * 阿里云 AccessKey Secret
     */
    private String accessKeySecret;

    /**
     * OSS Endpoint (如: oss-cn-beijing.aliyuncs.com)
     */
    private String endpoint;

    /**
     * OSS Bucket 名称
     */
    private String bucketName;

    /**
     * 外网访问域名 (如: https://bucket-name.oss-cn-beijing.aliyuncs.com)
     * 如果配置了自定义域名，也可以使用自定义域名
     */
    private String publicUrl;

    /**
     * 是否启用 OSS 图片上传
     * true: 使用阿里云 OSS
     * false: 使用 GitHub CDN (默认)
     */
    private boolean enabled = true;

    // Getter and Setter methods

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
