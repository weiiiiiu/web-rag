package com.fastgpt.docparser.controller;

import com.fastgpt.docparser.dto.ApiResponse;
import com.fastgpt.docparser.dto.DocumentHistoryDto;
import com.fastgpt.docparser.dto.ParseResult;
import com.fastgpt.docparser.service.DocumentParseService;
import com.fastgpt.docparser.service.DocumentHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档解析控制器
 */
@RestController
@RequestMapping("/api/document")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentParseService documentParseService;
    private final DocumentHistoryService documentHistoryService;

    public DocumentController(
            DocumentParseService documentParseService,
            DocumentHistoryService documentHistoryService) {
        this.documentParseService = documentParseService;
        this.documentHistoryService = documentHistoryService;
    }

    /**
     * 上传并解析文档
     *
     * @param file 文档文件
     * @return 解析结果
     */
    @PostMapping("/parse")
    public ApiResponse<ParseResult> parseDocument(@RequestParam("file") MultipartFile file) {
        log.info("收到文档解析请求: {}", file.getOriginalFilename());

        try {
            ParseResult result = documentParseService.parseDocument(file);
            return ApiResponse.success("解析成功", result);
        } catch (Exception e) {
            log.error("文档解析失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取所有解析历史记录
     */
    @GetMapping("/history")
    public ApiResponse<List<DocumentHistoryDto>> getHistoryList() {
        log.info("获取解析历史列表");
        try {
            List<DocumentHistoryDto> histories = documentHistoryService.getAllHistories();
            return ApiResponse.success("查询成功", histories);
        } catch (Exception e) {
            log.error("查询历史列表失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 根据ID获取历史记录详情
     */
    @GetMapping("/history/{id}")
    public ApiResponse<DocumentHistoryDto> getHistoryDetail(@PathVariable Long id) {
        log.info("获取历史记录详情: id={}", id);
        try {
            DocumentHistoryDto history = documentHistoryService.getHistoryById(id);
            return ApiResponse.success("查询成功", history);
        } catch (Exception e) {
            log.error("查询历史详情失败: id={}", id, e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 删除历史记录
     */
    @DeleteMapping("/history/{id}")
    public ApiResponse<Void> deleteHistory(@PathVariable Long id) {
        log.info("删除历史记录: id={}", id);
        try {
            documentHistoryService.deleteHistory(id);
            return ApiResponse.success("删除成功");
        } catch (Exception e) {
            log.error("删除历史记录失败: id={}", id, e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("服务运行正常");
    }
}
