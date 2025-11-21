package com.fastgpt.docparser.dto;

/**
 * 对话请求 DTO
 *
 * @author ZHONG WEI
 */
public class ChatRequest {

    /**
     * 知识库 ID
     */
    private String knowledgeBaseId;

    /**
     * 用户问题
     */
    private String question;

    /**
     * 是否流式返回（可选）
     */
    private Boolean stream = false;

    public ChatRequest() {
    }

    public ChatRequest(String knowledgeBaseId, String question) {
        this.knowledgeBaseId = knowledgeBaseId;
        this.question = question;
    }

    public String getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public void setKnowledgeBaseId(String knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }
}
