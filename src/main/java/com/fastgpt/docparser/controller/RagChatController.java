package com.fastgpt.docparser.controller;

import com.fastgpt.docparser.dto.ApiResponse;
import com.fastgpt.docparser.dto.ChatRequest;
import com.fastgpt.docparser.dto.ChatResponse;
import com.fastgpt.docparser.service.RagChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * RAG 对话控制器
 *
 * @author ZHONG WEI
 */
@RestController
@RequestMapping("/api/rag-chat")
public class RagChatController {

    private static final Logger log = LoggerFactory.getLogger(RagChatController.class);

    private final RagChatService ragChatService;

    public RagChatController(RagChatService ragChatService) {
        this.ragChatService = ragChatService;
    }

    /**
     * 发送消息并获取回答
     */
    @PostMapping("/send")
    public ApiResponse<ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        log.info("收到对话请求，知识库: {}, 问题: {}", request.getKnowledgeBaseId(), request.getQuestion());

        try {
            ChatResponse response = ragChatService.chat(request);
            return ApiResponse.success("对话成功", response);
        } catch (Exception e) {
            log.error("对话失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
}
