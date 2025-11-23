package com.fastgpt.docparser.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinerU 文档解析配置属性
 */
@Component
@ConfigurationProperties(prefix = "mineru")
public class MinerUProperties {

    /**
     * MinerU API Token
     */
    private String apiToken;

    /**
     * API 基础 URL
     */
    private String apiBaseUrl = "https://mineru.net/api/v4";

    /**
     * 模型版本：pipeline 或 vlm，默认 vlm
     */
    private String modelVersion = "vlm";

    /**
     * 是否开启公式识别，默认 true
     */
    private boolean enableFormula = true;

    /**
     * 是否开启表格识别，默认 true
     */
    private boolean enableTable = true;

    /**
     * 是否启动 OCR 功能，默认 false
     */
    private boolean isOcr = false;

    /**
     * 文档语言，默认 ch
     */
    private String language = "ch";

    /**
     * 轮询间隔（毫秒），默认 3000ms
     */
    private long pollingInterval = 3000;

    /**
     * 最大轮询次数，默认 200 次（10 分钟）
     */
    private int maxPollingAttempts = 200;

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public boolean isEnableFormula() {
        return enableFormula;
    }

    public void setEnableFormula(boolean enableFormula) {
        this.enableFormula = enableFormula;
    }

    public boolean isEnableTable() {
        return enableTable;
    }

    public void setEnableTable(boolean enableTable) {
        this.enableTable = enableTable;
    }

    public boolean isOcr() {
        return isOcr;
    }

    public void setOcr(boolean ocr) {
        isOcr = ocr;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
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

    /**
     * 获取申请上传链接的 URL
     */
    public String getFileUrlsBatchUrl() {
        return apiBaseUrl + "/file-urls/batch";
    }

    /**
     * 获取批量查询结果的 URL
     */
    public String getExtractResultsBatchUrl(String batchId) {
        return apiBaseUrl + "/extract-results/batch/" + batchId;
    }
}
