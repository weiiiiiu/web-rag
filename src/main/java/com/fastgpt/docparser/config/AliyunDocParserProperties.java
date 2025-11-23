package com.fastgpt.docparser.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云文档解析（大模型版）配置属性
 */
@Component
@ConfigurationProperties(prefix = "aliyun.docparser")
public class AliyunDocParserProperties {

    /**
     * 阿里云 Access Key ID
     */
    private String accessKeyId;

    /**
     * 阿里云 Access Key Secret
     */
    private String accessKeySecret;

    /**
     * API 端点
     */
    private String endpoint = "docmind-api.cn-hangzhou.aliyuncs.com";

    /**
     * 区域 ID
     */
    private String regionId = "cn-hangzhou";

    /**
     * 是否启用 LLM 增强，默认 true
     */
    private boolean llmEnhancement = true;

    /**
     * 增强模式，支持 VLM（视觉语言模型），默认 VLM
     */
    private String enhancementMode = "VLM";

    /**
     * 轮询间隔（毫秒），默认 3000ms
     */
    private long pollingInterval = 3000;

    /**
     * 最大轮询次数，默认 200 次（10 分钟）
     */
    private int maxPollingAttempts = 200;

    /**
     * 分块检索步长，默认 1000（每次检索的 layout 块数量）
     */
    private int layoutStepSize = 1000;

    /**
     * 是否输出 HTML 表格，默认 false
     */
    private boolean outputHtmlTable = false;

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

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public boolean isLlmEnhancement() {
        return llmEnhancement;
    }

    public void setLlmEnhancement(boolean llmEnhancement) {
        this.llmEnhancement = llmEnhancement;
    }

    public String getEnhancementMode() {
        return enhancementMode;
    }

    public void setEnhancementMode(String enhancementMode) {
        this.enhancementMode = enhancementMode;
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

    public int getLayoutStepSize() {
        return layoutStepSize;
    }

    public void setLayoutStepSize(int layoutStepSize) {
        this.layoutStepSize = layoutStepSize;
    }

    public boolean isOutputHtmlTable() {
        return outputHtmlTable;
    }

    public void setOutputHtmlTable(boolean outputHtmlTable) {
        this.outputHtmlTable = outputHtmlTable;
    }
}
