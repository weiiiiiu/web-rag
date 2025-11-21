package com.fastgpt.docparser.controller;

import com.fastgpt.docparser.dto.ApiResponse;
import com.fastgpt.docparser.dto.KnowledgeBaseDTO;
import com.fastgpt.docparser.service.KnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库管理控制器
 *
 * @author ZHONG WEI
 */
@RestController
@RequestMapping("/api/knowledge-base")
public class KnowledgeBaseController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseController.class);

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /**
     * 创建知识库
     */
    @PostMapping("/create")
    public ApiResponse<KnowledgeBaseDTO> createKnowledgeBase(
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description) {

        log.info("收到创建知识库请求: {}", name);

        try {
            KnowledgeBaseDTO knowledgeBase = knowledgeBaseService.createKnowledgeBase(name, description);
            return ApiResponse.success("知识库创建成功", knowledgeBase);
        } catch (Exception e) {
            log.error("创建知识库失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 上传文档到知识库
     */
    @PostMapping("/{indexId}/upload")
    public ApiResponse<String> uploadDocument(
            @PathVariable String indexId,
            @RequestParam("file") MultipartFile file) {

        log.info("收到上传文档请求，知识库: {}, 文件: {}", indexId, file.getOriginalFilename());

        try {
            String fileId = knowledgeBaseService.uploadDocument(indexId, file);
            return ApiResponse.success("文档上传成功", fileId);
        } catch (Exception e) {
            log.error("上传文档失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取知识库列表
     */
    @GetMapping("/list")
    public ApiResponse<List<KnowledgeBaseDTO>> listKnowledgeBases() {
        log.info("收到查询知识库列表请求");

        try {
            List<KnowledgeBaseDTO> knowledgeBases = knowledgeBaseService.listKnowledgeBases();
            return ApiResponse.success(knowledgeBases);
        } catch (Exception e) {
            log.error("查询知识库列表失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/{indexId}")
    public ApiResponse<Void> deleteKnowledgeBase(@PathVariable String indexId) {
        log.info("收到删除知识库请求: {}", indexId);

        try {
            knowledgeBaseService.deleteKnowledgeBase(indexId);
            return ApiResponse.success("知识库删除成功", null);
        } catch (Exception e) {
            log.error("删除知识库失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
}
