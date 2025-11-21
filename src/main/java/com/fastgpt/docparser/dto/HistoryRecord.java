package com.fastgpt.docparser.dto;

/**
 * 历史记录
 *
 * @author ZHONG WEI
 */
public class HistoryRecord {

    /**
     * 记录 ID
     */
    private String id;

    /**
     * 原文件名
     */
    private String originalFilename;

    /**
     * Markdown 内容
     */
    private String markdownContent;

    /**
     * 图片数量
     */
    private int imageCount;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 处理时间（毫秒）
     */
    private long processingTime;

    /**
     * 创建时间戳
     */
    private long createdAt;

    public HistoryRecord() {
    }

    public HistoryRecord(String id, String originalFilename, String markdownContent,
                        int imageCount, String filePath, long processingTime, long createdAt) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.markdownContent = markdownContent;
        this.imageCount = imageCount;
        this.filePath = filePath;
        this.processingTime = processingTime;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getMarkdownContent() {
        return markdownContent;
    }

    public void setMarkdownContent(String markdownContent) {
        this.markdownContent = markdownContent;
    }

    public int getImageCount() {
        return imageCount;
    }

    public void setImageCount(int imageCount) {
        this.imageCount = imageCount;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(long processingTime) {
        this.processingTime = processingTime;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public static class Builder {
        private String id;
        private String originalFilename;
        private String markdownContent;
        private int imageCount;
        private String filePath;
        private long processingTime;
        private long createdAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder originalFilename(String originalFilename) {
            this.originalFilename = originalFilename;
            return this;
        }

        public Builder markdownContent(String markdownContent) {
            this.markdownContent = markdownContent;
            return this;
        }

        public Builder imageCount(int imageCount) {
            this.imageCount = imageCount;
            return this;
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder processingTime(long processingTime) {
            this.processingTime = processingTime;
            return this;
        }

        public Builder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public HistoryRecord build() {
            return new HistoryRecord(id, originalFilename, markdownContent,
                    imageCount, filePath, processingTime, createdAt);
        }
    }
}
