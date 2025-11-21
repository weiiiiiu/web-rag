package com.fastgpt.docparser.dto;

/**
 * 上传文档到知识库请求 DTO
 *
 * @author ZHONG WEI
 */
public class UploadDocRequest {

    /**
     * 知识库 ID
     */
    private String knowledgeBaseId;

    /**
     * 文件名
     */
    private String filename;

    /**
     * 是否需要先转换为 Markdown（默认 true）
     */
    private Boolean convertToMarkdown = true;

    public UploadDocRequest() {
    }

    public UploadDocRequest(String knowledgeBaseId, String filename) {
        this.knowledgeBaseId = knowledgeBaseId;
        this.filename = filename;
    }

    public String getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public void setKnowledgeBaseId(String knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Boolean getConvertToMarkdown() {
        return convertToMarkdown;
    }

    public void setConvertToMarkdown(Boolean convertToMarkdown) {
        this.convertToMarkdown = convertToMarkdown;
    }
}
