package com.fastgpt.docparser.service;

import com.fastgpt.docparser.entity.DocumentHistory;
import com.fastgpt.docparser.dto.DocumentHistoryDto;
import com.fastgpt.docparser.exception.BusinessException;
import com.fastgpt.docparser.repository.DocumentHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档历史记录服务
 */
@Service
public class DocumentHistoryService {

    private static final Logger log = LoggerFactory.getLogger(DocumentHistoryService.class);

    private final DocumentHistoryRepository historyRepository;
    private final AliyunOssImageService aliyunOssImageService;
    private final GitHubImageService gitHubImageService;

    public DocumentHistoryService(
            DocumentHistoryRepository historyRepository,
            AliyunOssImageService aliyunOssImageService,
            GitHubImageService gitHubImageService) {
        this.historyRepository = historyRepository;
        this.aliyunOssImageService = aliyunOssImageService;
        this.gitHubImageService = gitHubImageService;
    }

    /**
     * 保存解析历史记录
     */
    @Transactional
    public DocumentHistory saveHistory(
            String originalFilename,
            String documentId,
            String knowledgeBaseId,
            String markdownContent,
            Integer imageCount,
            Long processingTime,
            String imageStorage) {
        
        DocumentHistory history = DocumentHistory.builder()
                .originalFilename(originalFilename)
                .documentId(documentId)
                .knowledgeBaseId(knowledgeBaseId)
                .markdownContent(markdownContent)
                .imageCount(imageCount)
                .processingTime(processingTime)
                .imageStorage(imageStorage)
                .build();

        DocumentHistory saved = historyRepository.save(history);
        log.info("保存解析历史记录: id={}, filename={}, documentId={}", 
                saved.getId(), originalFilename, documentId);
        
        return saved;
    }

    /**
     * 获取所有历史记录（不包含markdown内容）
     */
    public List<DocumentHistoryDto> getAllHistories() {
        List<DocumentHistory> histories = historyRepository.findAllByOrderByCreatedAtDesc();
        return histories.stream()
                .map(DocumentHistoryDto::fromEntityWithoutContent)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取历史记录详情（包含markdown内容）
     */
    public DocumentHistoryDto getHistoryById(Long id) {
        DocumentHistory history = historyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("历史记录不存在: id=" + id));
        
        return DocumentHistoryDto.fromEntity(history);
    }

    /**
     * 删除历史记录及其关联的图片
     */
    @Transactional
    public void deleteHistory(Long id) {
        DocumentHistory history = historyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("历史记录不存在: id=" + id));

        log.info("删除历史记录: id={}, filename={}, documentId={}, knowledgeBaseId={}, imageStorage={}", 
                id, history.getOriginalFilename(), history.getDocumentId(), 
                history.getKnowledgeBaseId(), history.getImageStorage());

        // 删除图片文件夹
        try {
            if ("oss".equals(history.getImageStorage())) {
                // 删除阿里云OSS上的图片文件夹
                aliyunOssImageService.deleteDocumentFolder(
                        history.getKnowledgeBaseId(), 
                        history.getDocumentId());
                log.info("已删除OSS文件夹: {}/{}", 
                        history.getKnowledgeBaseId(), history.getDocumentId());
            } else if ("github".equals(history.getImageStorage())) {
                // 删除GitHub上的图片文件夹
                gitHubImageService.deleteDocumentFolder(
                        history.getKnowledgeBaseId(), 
                        history.getDocumentId());
                log.info("已删除GitHub文件夹: {}/{}", 
                        history.getKnowledgeBaseId(), history.getDocumentId());
            }
        } catch (Exception e) {
            log.error("删除图片文件夹失败，但继续删除数据库记录", e);
            // 即使删除图片失败，也继续删除数据库记录
        }

        // 删除数据库记录
        historyRepository.deleteById(id);
        log.info("已删除历史记录: id={}", id);
    }
}
