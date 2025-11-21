package com.fastgpt.docparser.dto;

import java.util.List;

/**
 * 对话响应 DTO
 *
 * @author ZHONG WEI
 */
public class ChatResponse {

    /**
     * 回答内容（Markdown 格式）
     */
    private String answer;

    /**
     * 参考的文本切片
     */
    private List<String> references;

    /**
     * 响应时间（毫秒）
     */
    private Long responseTime;

    public ChatResponse() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<String> getReferences() {
        return references;
    }

    public void setReferences(List<String> references) {
        this.references = references;
    }

    public Long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Long responseTime) {
        this.responseTime = responseTime;
    }

    public static class Builder {
        private final ChatResponse response = new ChatResponse();

        public Builder answer(String answer) {
            response.answer = answer;
            return this;
        }

        public Builder references(List<String> references) {
            response.references = references;
            return this;
        }

        public Builder responseTime(Long responseTime) {
            response.responseTime = responseTime;
            return this;
        }

        public ChatResponse build() {
            return response;
        }
    }
}
