package com.fastgpt.docparser.dto;

import java.util.List;

/**
 * 文档解析结果
 */
public class ParseResult {

    /**
     * 原文件名
     */
    private String originalFilename;

    /**
     * Markdown 内容
     */
    private String markdownContent;

    /**
     * 上传的图片数量
     */
    private int imageCount;

    /**
     * 图片 CDN 链接列表
     */
    private List<String> imageUrls;

    /**
     * 结果文件路径
     */
    private String resultFilePath;

    /**
     * 处理耗时（毫秒）
     */
    private long processingTime;

    public ParseResult() {
    }

    public ParseResult(String originalFilename, String markdownContent, int imageCount,
                      List<String> imageUrls, String resultFilePath, long processingTime) {
        this.originalFilename = originalFilename;
        this.markdownContent = markdownContent;
        this.imageCount = imageCount;
        this.imageUrls = imageUrls;
        this.resultFilePath = resultFilePath;
        this.processingTime = processingTime;
    }

    public static Builder builder() {
        return new Builder();
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

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public String getResultFilePath() {
        return resultFilePath;
    }

    public void setResultFilePath(String resultFilePath) {
        this.resultFilePath = resultFilePath;
    }

    public long getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(long processingTime) {
        this.processingTime = processingTime;
    }

    public static class Builder {
        private String originalFilename;
        private String markdownContent;
        private int imageCount;
        private List<String> imageUrls;
        private String resultFilePath;
        private long processingTime;

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

        public Builder imageUrls(List<String> imageUrls) {
            this.imageUrls = imageUrls;
            return this;
        }

        public Builder resultFilePath(String resultFilePath) {
            this.resultFilePath = resultFilePath;
            return this;
        }

        public Builder processingTime(long processingTime) {
            this.processingTime = processingTime;
            return this;
        }

        public ParseResult build() {
            return new ParseResult(originalFilename, markdownContent, imageCount,
                                  imageUrls, resultFilePath, processingTime);
        }
    }
}
