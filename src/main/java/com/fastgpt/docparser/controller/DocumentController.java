package com.fastgpt.docparser.controller;

import com.fastgpt.docparser.dto.ApiResponse;
import com.fastgpt.docparser.dto.ParseResult;
import com.fastgpt.docparser.service.DocumentParseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档解析控制器
 */
@RestController
@RequestMapping("/api/document")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentParseService documentParseService;

    public DocumentController(DocumentParseService documentParseService) {
        this.documentParseService = documentParseService;
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
     * 健康检查接口
     */
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("服务运行正常");
    }
}
