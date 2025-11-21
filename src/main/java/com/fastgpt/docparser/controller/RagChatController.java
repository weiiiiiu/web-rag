package com.fastgpt.docparser.controller;

import com.fastgpt.docparser.dto.ApiResponse;
import com.fastgpt.docparser.dto.ChatRequest;
import com.fastgpt.docparser.dto.ChatResponse;
import com.fastgpt.docparser.service.RagChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public RagChatController(RagChatService ragChatService) {
        this.ragChatService = ragChatService;
    }

    /**
     * 流式发送消息并获取回答
     */
    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(@RequestBody ChatRequest request) {
        log.info("收到流式对话请求，知识库: {}, 问题: {}", request.getKnowledgeBaseId(), request.getQuestion());

        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L); // 5分钟超时

        // 在异步线程中处理
        executorService.execute(() -> {
            try {
                ragChatService.chatStream(request, new RagChatService.StreamCallback() {
                    @Override
                    public void onData(String chunk) {
                        try {
                            emitter.send(SseEmitter.event()
                                    .data(chunk)
                                    .name("message"));
                        } catch (IOException e) {
                            log.error("发送流式数据失败", e);
                            emitter.completeWithError(e);
                        }
                    }

                    @Override
                    public void onComplete() {
                        try {
                            emitter.send(SseEmitter.event()
                                    .data("[DONE]")
                                    .name("done"));
                            emitter.complete();
                            log.info("流式对话完成");
                        } catch (IOException e) {
                            log.error("发送完成信号失败", e);
                            emitter.completeWithError(e);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        try {
                            emitter.send(SseEmitter.event()
                                    .data(error)
                                    .name("error"));
                            emitter.completeWithError(new RuntimeException(error));
                        } catch (IOException e) {
                            log.error("发送错误信息失败", e);
                            emitter.completeWithError(e);
                        }
                    }
                });
            } catch (Exception e) {
                log.error("流式对话处理异常", e);
                emitter.completeWithError(e);
            }
        });

        // 设置超时和完成回调
        emitter.onTimeout(() -> {
            log.warn("流式对话超时");
            emitter.complete();
        });

        emitter.onCompletion(() -> log.debug("SSE 连接已关闭"));

        return emitter;
    }
}
