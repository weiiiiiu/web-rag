package com.fastgpt.docparser.service;

import com.aliyun.bailian20231229.Client;
import com.aliyun.teaopenapi.models.Config;
import com.fastgpt.docparser.config.AliyunBailianProperties;
import com.fastgpt.docparser.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 阿里云百炼基础服务
 * 负责客户端初始化和基础 API 调用封装
 *
 * @author ZHONG WEI
 */
@Service
public class AliyunBailianService {

    private static final Logger log = LoggerFactory.getLogger(AliyunBailianService.class);

    private final AliyunBailianProperties properties;
    private final Client client;

    public AliyunBailianService(AliyunBailianProperties properties) {
        this.properties = properties;
        
        // 验证必要的配置参数
        validateConfiguration();
        
        this.client = createClient();
        log.info("=== 阿里云百炼配置信息 ===");
        log.info("Endpoint: {}", properties.getEndpoint());
        log.info("Workspace ID: {}", properties.getWorkspaceId());
        log.info("AccessKey ID: {}****", properties.getAccessKeyId() != null && properties.getAccessKeyId().length() > 4
            ? properties.getAccessKeyId().substring(0, 4) : "空");
        log.info("AccessKey Secret: {}****", properties.getAccessKeySecret() != null && properties.getAccessKeySecret().length() > 4
            ? "已配置" : "未配置");
        log.info("=========================");
    }

    /**
     * 验证配置参数
     */
    private void validateConfiguration() {
        List<String> errors = new ArrayList<>();
        
        if (properties.getAccessKeyId() == null || properties.getAccessKeyId().trim().isEmpty()) {
            errors.add("AccessKey ID 未配置");
        }
        
        if (properties.getAccessKeySecret() == null || properties.getAccessKeySecret().trim().isEmpty()) {
            errors.add("AccessKey Secret 未配置");
        }
        
        if (properties.getWorkspaceId() == null || properties.getWorkspaceId().trim().isEmpty()) {
            errors.add("Workspace ID 未配置");
        }
        
        if (properties.getEndpoint() == null || properties.getEndpoint().trim().isEmpty()) {
            errors.add("Endpoint 未配置");
        }
        
        if (!errors.isEmpty()) {
            String errorMsg = "阿里云百炼配置验证失败：\n" + String.join("\n", errors);
            log.error(errorMsg);
            throw new BusinessException(errorMsg);
        }
        
        log.info("阿里云百炼配置验证通过");
    }

    /**
     * 创建阿里云百炼客户端
     */
    private Client createClient() {
        try {
            // 使用凭据初始化
            com.aliyun.credentials.Client credential = new com.aliyun.credentials.Client(
                new com.aliyun.credentials.models.Config()
                    .setAccessKeyId(properties.getAccessKeyId())
                    .setAccessKeySecret(properties.getAccessKeySecret())
                    .setType("access_key")
            );

            Config config = new Config()
                .setCredential(credential)
                .setEndpoint(properties.getEndpoint());

            return new Client(config);
        } catch (Exception e) {
            log.error("创建阿里云百炼客户端失败", e);
            throw new BusinessException("创建阿里云百炼客户端失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取客户端实例
     */
    public Client getClient() {
        return client;
    }

    /**
     * 获取配置属性
     */
    public AliyunBailianProperties getProperties() {
        return properties;
    }

    /**
     * 获取业务空间 ID
     */
    public String getWorkspaceId() {
        return properties.getWorkspaceId();
    }

    /**
     * 获取默认类目 ID
     */
    public String getCategoryId() {
        return properties.getCategoryId();
    }

    /**
     * 获取应用 ID
     */
    public String getAppId() {
        return properties.getAppId();
    }

    /**
     * 等待指定时间（用于轮询）
     */
    public void waitPollingInterval() {
        try {
            Thread.sleep(properties.getPollingInterval());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("轮询等待被中断", e);
        }
    }
}
