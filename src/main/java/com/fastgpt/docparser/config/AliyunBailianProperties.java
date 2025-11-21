package com.fastgpt.docparser.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云百炼配置属性
 *
 * @author ZHONG WEI
 */
@Component
@ConfigurationProperties(prefix = "aliyun.bailian")
public class AliyunBailianProperties {

    /**
     * Access Key ID
     */
    private String accessKeyId;

    /**
     * Access Key Secret
     */
    private String accessKeySecret;

    /**
     * 业务空间 ID
     */
    private String workspaceId;

    /**
     * API 接入点
     */
    private String endpoint = "bailian.cn-beijing.aliyuncs.com";

    /**
     * 应用 ID（用于对话功能）
     */
    private String appId;

    /**
     * 默认类目 ID
     */
    private String categoryId = "default";

    /**
     * 通义千问 API Key（用于大模型对话）
     */
    private String apiKey;

    /**
     * 轮询间隔（毫秒）
     */
    private long pollingInterval = 3000;

    /**
     * 最大轮询次数
     */
    private int maxPollingAttempts = 200;

    // Getters and Setters

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

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public long getPollingInterval() {
        return pollingInterval;
    }

    public void setPollingInterval(long pollingInterval) {
        this.pollingInterval = pollingInterval;
    }

    public int getMaxPollingAttempts() {
        return maxPollingAttempts;
    }

    public void setMaxPollingAttempts(int maxPollingAttempts) {
        this.maxPollingAttempts = maxPollingAttempts;
    }
}
