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
     * 处理对话请求
     */
    public ChatResponse chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("开始处理对话请求，知识库: {}, 问题: {}", request.getKnowledgeBaseId(), request.getQuestion());

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

            // Step 3: 调用通义千问生成回答
            log.debug("Step 3: 调用通义千问生成回答...");
            String answer = callQwenAPI(prompt);

            long responseTime = System.currentTimeMillis() - startTime;
            log.info("对话处理完成，耗时: {}ms", responseTime);

            return ChatResponse.builder()
                    .answer(answer)
                    .references(retrievedTexts)
                    .responseTime(responseTime)
                    .build();

        } catch (Exception e) {
            log.error("对话处理失败", e);
            throw new BusinessException("对话处理失败: " + e.getMessage(), e);
        }
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
        prompt.append("请根据以下参考信息回答问题。");
        prompt.append("如果参考信息中有图片链接（Markdown 格式），请在回答中保留这些图片的 Markdown 格式。\n\n");

        if (!references.isEmpty()) {
            prompt.append("参考信息：\n");
            for (int i = 0; i < references.size(); i++) {
                prompt.append(i + 1).append(". ").append(references.get(i)).append("\n\n");
            }
        }

        prompt.append("问题：").append(question).append("\n\n");
        prompt.append("请用中文回答，如果参考信息中包含图片，请在回答中展示相关图片。");

        return prompt.toString();
    }

    /**
     * 调用通义千问 API 生成回答
     */
    private String callQwenAPI(String prompt) {
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
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("通义千问 API 错误: {} - {}", response.code(), errorBody);
                    throw new BusinessException("调用通义千问 API 失败: " + response.code() + ", " + errorBody);
                }

                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                // 提取回答
                if (jsonResponse.has("output") &&
                    jsonResponse.getAsJsonObject("output").has("choices")) {

                    com.google.gson.JsonArray choices = jsonResponse.getAsJsonObject("output")
                            .getAsJsonArray("choices");

                    if (choices.size() > 0) {
                        JsonObject firstChoice = choices.get(0).getAsJsonObject();
                        if (firstChoice.has("message")) {
                            return firstChoice.getAsJsonObject("message")
                                    .get("content").getAsString();
                        }
                    }
                }

                throw new BusinessException("通义千问返回空结果");
            }

        } catch (Exception e) {
            log.error("调用通义千问 API 失败", e);
            throw new BusinessException("调用通义千问 API 失败: " + e.getMessage(), e);
        }
    }
}
