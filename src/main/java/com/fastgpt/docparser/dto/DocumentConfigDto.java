package com.fastgpt.docparser.dto;

/**
 * 文档解析配置 DTO
 */
public class DocumentConfigDto {

    /**
     * 解析器类型：aliyun 或 mineru
     */
    private String parserType;

    /**
     * 图片存储策略：oss 或 github
     */
    private String imageStorage;

    // Constructors
    public DocumentConfigDto() {
    }

    public DocumentConfigDto(String parserType, String imageStorage) {
        this.parserType = parserType;
        this.imageStorage = imageStorage;
    }

    // Getters and Setters
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
}
