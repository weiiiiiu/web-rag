package com.fastgpt.docparser.repository;

import com.fastgpt.docparser.entity.DocumentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档历史记录仓库
 */
@Repository
public interface DocumentHistoryRepository extends JpaRepository<DocumentHistory, Long> {

    /**
     * 按创建时间倒序查询所有历史记录
     */
    List<DocumentHistory> findAllByOrderByCreatedAtDesc();

    /**
     * 根据文档ID查询历史记录
     */
    List<DocumentHistory> findByDocumentIdOrderByCreatedAtDesc(String documentId);

    /**
     * 根据知识库ID查询历史记录
     */
    List<DocumentHistory> findByKnowledgeBaseIdOrderByCreatedAtDesc(String knowledgeBaseId);
}
