package com.fastgpt.docparser.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文档解析配置属性
 */
@Component
@ConfigurationProperties(prefix = "document")
public class DocumentProperties {

    /**
     * 解析器类型：aliyun（阿里云大模型版）或 mineru（MinerU）
     */
    private String parserType = "aliyun";

    /**
     * 图片存储策略：oss（阿里云 OSS）或 github（GitHub CDN）
     */
    private String imageStorage = "oss";

    public String getParserType() {
        return parserType;
    }

    public void setParserType(String parserType) {
        this.parserType = parserType;
    }

    public String getImageStorage() {
        return imageStorage;
    }

    public void setImageStorage(String imageStorage) {
        this.imageStorage = imageStorage;
    }

    /**
     * 是否使用阿里云解析器
     */
    public boolean isAliyun() {
        return "aliyun".equalsIgnoreCase(parserType);
    }

    /**
     * 是否使用 MinerU 解析器
     */
    public boolean isMinerU() {
        return "mineru".equalsIgnoreCase(parserType);
    }

    /**
     * 图片是否存储到阿里云 OSS
     */
    public boolean isOssStorage() {
        return "oss".equalsIgnoreCase(imageStorage);
    }

    /**
     * 图片是否存储到 GitHub
     */
    public boolean isGithubStorage() {
        return "github".equalsIgnoreCase(imageStorage);
    }
}
