package com.fastgpt.docparser.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 文档解析历史记录实体
 */
@Entity
@Table(name = "document_history")
public class DocumentHistory {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 原始文件名（包含后缀）
     * 例如: pdf001.pdf
     */
    @Column(nullable = false, length = 255)
    private String originalFilename;

    /**
     * 文档ID（用于OSS/GitHub路径）
     * 例如: pdf001
     */
    @Column(nullable = false, length = 100)
    private String documentId;

    /**
     * 知识库ID
     * 例如: 测试知识库
     */
    @Column(nullable = false, length = 100)
    private String knowledgeBaseId;

    /**
     * 解析后的Markdown内容
     */
    @Column(columnDefinition = "LONGTEXT")
    private String markdownContent;

    /**
     * 图片数量
     */
    @Column(nullable = false)
    private Integer imageCount;

    /**
     * 处理时间（毫秒）
     */
    @Column(nullable = false)
    private Long processingTime;

    /**
     * 图片存储策略
     * oss: 阿里云OSS
     * github: GitHub图床
     */
    @Column(nullable = false, length = 20)
    private String imageStorage;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public DocumentHistory() {
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

        public DocumentHistory build() {
            DocumentHistory history = new DocumentHistory();
            history.id = this.id;
            history.originalFilename = this.originalFilename;
            history.documentId = this.documentId;
            history.knowledgeBaseId = this.knowledgeBaseId;
            history.markdownContent = this.markdownContent;
            history.imageCount = this.imageCount;
            history.processingTime = this.processingTime;
            history.imageStorage = this.imageStorage;
            history.createdAt = this.createdAt;
            return history;
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
}
