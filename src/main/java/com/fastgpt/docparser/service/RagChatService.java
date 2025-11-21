package com.fastgpt.docparser.service;

import com.aliyun.bailian20231229.models.*;
import com.aliyun.teautil.models.RuntimeOptions;
import com.fastgpt.docparser.dto.ChatRequest;
import com.fastgpt.docparser.dto.ChatResponse;
import com.fastgpt.docparser.exception.BusinessException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * RAG 对话服务
 * 负责知识库检索和大模型回答生成
 *
 * @author ZHONG WEI
 */
@Service
public class RagChatService {

    private static final Logger log = LoggerFactory.getLogger(RagChatService.class);
    private static final String QWEN_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

    private final AliyunBailianService bailianService;
    private final OkHttpClient httpClient;
    private final Gson gson;

    public RagChatService(AliyunBailianService bailianService) {
        this.bailianService = bailianService;
        // 配置超时时间：连接超时 30 秒，读取超时 120 秒
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }


    /**
     * 检索知识库
     */
    private List<String> retrieveKnowledgeBase(String indexId, String query) {
        try {
            RetrieveRequest request = new RetrieveRequest()
                    .setIndexId(indexId)
                    .setQuery(query);

            RetrieveResponse response = bailianService.getClient().retrieveWithOptions(
                    bailianService.getWorkspaceId(),
                    request,
                    new HashMap<>(),
                    new RuntimeOptions()
            );

            // 提取检索到的文本切片（添加完整的空值检查）
            if (response != null &&
                response.getBody() != null &&
                response.getBody().getData() != null &&
                response.getBody().getData().getNodes() != null &&
                !response.getBody().getData().getNodes().isEmpty()) {

                return response.getBody().getData().getNodes().stream()
                        .map(node -> node.getText())
                        .filter(text -> text != null && !text.isEmpty())
                        .collect(Collectors.toList());
            }

            log.warn("知识库检索未返回任何结果，可能知识库为空或查询无匹配");
            return new ArrayList<>();

        } catch (Exception e) {
            log.error("检索知识库失败", e);
            throw new BusinessException("检索知识库失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构造 Prompt
     */
    private String buildPrompt(String question, List<String> references) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据以下参考信息回答问题。\n\n");
        prompt.append("重要：参考信息中的图片使用了 Markdown 格式 ![](图片链接)，");
        prompt.append("你必须在回答中完整保留这种 Markdown 图片格式，不要把图片转换成纯文本链接。\n");
        prompt.append("例如：如果参考信息中有 ![](https://example.com/image.jpg)，");
        prompt.append("你的回答中也要使用 ![](https://example.com/image.jpg) 或 ![描述](https://example.com/image.jpg)。\n\n");

        if (!references.isEmpty()) {
            prompt.append("参考信息：\n");
            for (int i = 0; i < references.size(); i++) {
                prompt.append(i + 1).append(". ").append(references.get(i)).append("\n\n");
            }
        }

        prompt.append("问题：").append(question).append("\n\n");
        prompt.append("回答要求：\n");
        prompt.append("1. 使用中文回答\n");
        prompt.append("2. 必须保留 Markdown 图片语法格式 ![](url)\n");
        prompt.append("3. 图片要放在相关描述的附近\n");

        return prompt.toString();
    }


    /**
     * 流式处理对话请求
     * 回调接口用于接收流式数据
     */
    public interface StreamCallback {
        void onData(String chunk);
        void onComplete();
        void onError(String error);
    }

    /**
     * 处理流式对话请求
     */
    public void chatStream(ChatRequest request, StreamCallback callback) {
        log.info("开始处理流式对话请求，知识库: {}, 问题: {}", request.getKnowledgeBaseId(), request.getQuestion());

        try {
            // Step 1: 检索知识库
            log.debug("Step 1: 检索知识库...");
            List<String> retrievedTexts = retrieveKnowledgeBase(
                    request.getKnowledgeBaseId(),
                    request.getQuestion()
            );

            // Step 2: 构造 Prompt
            log.debug("Step 2: 构造 Prompt...");
            String prompt = buildPrompt(request.getQuestion(), retrievedTexts);

            // Step 3: 调用通义千问流式 API
            log.debug("Step 3: 调用通义千问流式生成回答...");
            callQwenAPIStream(prompt, callback);

        } catch (Exception e) {
            log.error("流式对话处理失败", e);
            callback.onError("对话处理失败: " + e.getMessage());
        }
    }

    /**
     * 调用通义千问流式 API
     */
    private void callQwenAPIStream(String prompt, StreamCallback callback) {
        try {
            // 构造请求体
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", "qwen-turbo");

            JsonObject input = new JsonObject();

            // 构造消息数组
            com.google.gson.JsonArray messages = new com.google.gson.JsonArray();
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", "你是一个helpful的AI助手");
            messages.add(systemMessage);

            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content", prompt);
            messages.add(userMessage);

            input.add("messages", messages);
            requestBody.add("input", input);

            JsonObject parameters = new JsonObject();
            parameters.addProperty("result_format", "message");
            parameters.addProperty("incremental_output", true);  // 启用增量输出
            requestBody.add("parameters", parameters);

            // 发送请求
            Request request = new Request.Builder()
                    .url(QWEN_API_URL)
                    .post(RequestBody.create(
                            gson.toJson(requestBody),
                            MediaType.parse("application/json")
                    ))
                    .addHeader("Authorization", "Bearer " + bailianService.getProperties().getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-DashScope-SSE", "enable")  // 启用 SSE
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("通义千问流式 API 错误: {} - {}", response.code(), errorBody);
                    callback.onError("调用通义千问 API 失败: " + response.code() + ", " + errorBody);
                    return;
                }

                // 读取流式响应
                boolean completed = false;
                try (var source = response.body().source()) {
                    while (!source.exhausted()) {
                        String line = source.readUtf8Line();

                        if (line == null || line.trim().isEmpty()) {
                            continue;
                        }

                        // SSE 格式: data: {...}
                        if (line.startsWith("data:")) {
                            String jsonData = line.substring(5).trim();

                            // 检查是否是结束标记
                            if (jsonData.equals("[DONE]")) {
                                log.info("收到 [DONE] 标记");
                                callback.onComplete();
                                completed = true;
                                break;
                            }

                            try {
                                JsonObject jsonResponse = gson.fromJson(jsonData, JsonObject.class);

                                // 提取增量内容
                                if (jsonResponse.has("output") &&
                                    jsonResponse.getAsJsonObject("output").has("choices")) {

                                    com.google.gson.JsonArray choices = jsonResponse.getAsJsonObject("output")
                                            .getAsJsonArray("choices");

                                    if (choices.size() > 0) {
                                        JsonObject firstChoice = choices.get(0).getAsJsonObject();
                                        if (firstChoice.has("message")) {
                                            String content = firstChoice.getAsJsonObject("message")
                                                    .get("content").getAsString();

                                            if (content != null && !content.isEmpty()) {
                                                callback.onData(content);
                                            }
                                        }
                                    }
                                }

                                // 检查是否完成
                                if (jsonResponse.has("output") &&
                                    jsonResponse.getAsJsonObject("output").has("finish_reason")) {
                                    String finishReason = jsonResponse.getAsJsonObject("output")
                                            .get("finish_reason").getAsString();
                                    if ("stop".equals(finishReason)) {
                                        log.info("收到 finish_reason=stop");
                                        callback.onComplete();
                                        completed = true;
                                        break;
                                    }
                                }

                            } catch (Exception e) {
                                log.warn("解析流式响应失败: {}", jsonData, e);
                            }
                        }
                    }
                }

                // 如果流读取完但没有收到完成信号，手动调用完成
                if (!completed) {
                    log.info("流读取结束，但未收到完成信号，手动调用 onComplete");
                    callback.onComplete();
                }

            }

        } catch (Exception e) {
            log.error("调用通义千问流式 API 失败", e);
            callback.onError("调用通义千问流式 API 失败: " + e.getMessage());
        }
    }
}
