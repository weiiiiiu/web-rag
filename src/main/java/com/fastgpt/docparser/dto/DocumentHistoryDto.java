package com.fastgpt.docparser.dto;

import java.time.LocalDateTime;

/**
 * 文档历史记录DTO
 */
public class DocumentHistoryDto {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 原始文件名（包含后缀）
     */
    private String originalFilename;

    /**
     * 文档ID
     */
    private String documentId;

    /**
     * 知识库ID
     */
    private String knowledgeBaseId;

    /**
     * Markdown内容（列表查询时不返回，详情查询时返回）
     */
    private String markdownContent;

    /**
     * 图片数量
     */
    private Integer imageCount;

    /**
     * 处理时间（毫秒）
     */
    private Long processingTime;

    /**
     * 图片存储策略
     */
    private String imageStorage;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    // Constructors
    public DocumentHistoryDto() {
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String originalFilename;
        private String documentId;
        private String knowledgeBaseId;
        private String markdownContent;
        private Integer imageCount;
        private Long processingTime;
        private String imageStorage;
        private LocalDateTime createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder originalFilename(String originalFilename) {
            this.originalFilename = originalFilename;
            return this;
        }

        public Builder documentId(String documentId) {
            this.documentId = documentId;
            return this;
        }

        public Builder knowledgeBaseId(String knowledgeBaseId) {
            this.knowledgeBaseId = knowledgeBaseId;
            return this;
        }

        public Builder markdownContent(String markdownContent) {
            this.markdownContent = markdownContent;
            return this;
        }

        public Builder imageCount(Integer imageCount) {
            this.imageCount = imageCount;
            return this;
        }

        public Builder processingTime(Long processingTime) {
            this.processingTime = processingTime;
            return this;
        }

        public Builder imageStorage(String imageStorage) {
            this.imageStorage = imageStorage;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public DocumentHistoryDto build() {
            DocumentHistoryDto dto = new DocumentHistoryDto();
            dto.id = this.id;
            dto.originalFilename = this.originalFilename;
            dto.documentId = this.documentId;
            dto.knowledgeBaseId = this.knowledgeBaseId;
            dto.markdownContent = this.markdownContent;
            dto.imageCount = this.imageCount;
            dto.processingTime = this.processingTime;
            dto.imageStorage = this.imageStorage;
            dto.createdAt = this.createdAt;
            return dto;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public void setKnowledgeBaseId(String knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }

    public String getMarkdownContent() {
        return markdownContent;
    }

    public void setMarkdownContent(String markdownContent) {
        this.markdownContent = markdownContent;
    }

    public Integer getImageCount() {
        return imageCount;
    }

    public void setImageCount(Integer imageCount) {
        this.imageCount = imageCount;
    }

    public Long getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(Long processingTime) {
        this.processingTime = processingTime;
    }

    public String getImageStorage() {
        return imageStorage;
    }

    public void setImageStorage(String imageStorage) {
        this.imageStorage = imageStorage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 从实体转换为DTO（列表查询，不包含markdown内容）
     */
    public static DocumentHistoryDto fromEntityWithoutContent(com.fastgpt.docparser.entity.DocumentHistory entity) {
        return DocumentHistoryDto.builder()
                .id(entity.getId())
                .originalFilename(entity.getOriginalFilename())
                .documentId(entity.getDocumentId())
                .knowledgeBaseId(entity.getKnowledgeBaseId())
                .imageCount(entity.getImageCount())
                .processingTime(entity.getProcessingTime())
                .imageStorage(entity.getImageStorage())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * 从实体转换为DTO（详情查询，包含markdown内容）
     */
    public static DocumentHistoryDto fromEntity(com.fastgpt.docparser.entity.DocumentHistory entity) {
        return DocumentHistoryDto.builder()
                .id(entity.getId())
                .originalFilename(entity.getOriginalFilename())
                .documentId(entity.getDocumentId())
                .knowledgeBaseId(entity.getKnowledgeBaseId())
                .markdownContent(entity.getMarkdownContent())
                .imageCount(entity.getImageCount())
                .processingTime(entity.getProcessingTime())
                .imageStorage(entity.getImageStorage())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
