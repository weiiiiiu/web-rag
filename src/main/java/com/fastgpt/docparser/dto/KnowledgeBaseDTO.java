package com.fastgpt.docparser.dto;

/**
 * 知识库 DTO
 *
 * @author ZHONG WEI
 */
public class KnowledgeBaseDTO {

    /**
     * 知识库 ID
     */
    private String id;

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Long createdAt;

    /**
     * 状态（PENDING, COMPLETED, FAILED）
     */
    private String status;

    public KnowledgeBaseDTO() {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public static class Builder {
        private final KnowledgeBaseDTO dto = new KnowledgeBaseDTO();

        public Builder id(String id) {
            dto.id = id;
            return this;
        }

        public Builder name(String name) {
            dto.name = name;
            return this;
        }

        public Builder description(String description) {
            dto.description = description;
            return this;
        }

        public Builder createdAt(Long createdAt) {
            dto.createdAt = createdAt;
            return this;
        }

        public Builder status(String status) {
            dto.status = status;
            return this;
        }

        public KnowledgeBaseDTO build() {
            return dto;
        }
    }
}
