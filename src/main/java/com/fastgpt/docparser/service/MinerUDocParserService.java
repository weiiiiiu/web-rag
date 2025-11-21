package com.fastgpt.docparser.service;

import com.fastgpt.docparser.config.FileProperties;
import com.fastgpt.docparser.config.MinerUProperties;
import com.fastgpt.docparser.exception.BusinessException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * MinerU 文档解析服务
 */
@Service
public class MinerUDocParserService {

    private static final Logger log = LoggerFactory.getLogger(MinerUDocParserService.class);
    // Markdown 图片链接的正则表达式
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)");

    private final MinerUProperties minerUProperties;
    private final FileProperties fileProperties;
    private final GitHubImageService gitHubImageService;
    private final OkHttpClient httpClient;

    public MinerUDocParserService(MinerUProperties minerUProperties, 
                                   FileProperties fileProperties,
                                   GitHubImageService gitHubImageService) {
        this.minerUProperties = minerUProperties;
        this.fileProperties = fileProperties;
        this.gitHubImageService = gitHubImageService;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    /**
     * 解析文档为 Markdown
     *
     * @param filePath 文档文件路径
     * @return 解析结果（包含 markdown 内容和解压目录）
     */
    public ParseResult parseToMarkdown(Path filePath) {
        try {
            // 1. 申请上传链接
            log.info("步骤 1: 申请 MinerU 上传链接...");
            String fileName = filePath.getFileName().toString();
            UploadUrlResponse uploadUrlResponse = applyUploadUrl(fileName);
            String batchId = uploadUrlResponse.batchId;
            String uploadUrl = uploadUrlResponse.uploadUrl;
            log.info("获取到 batch_id: {}, upload_url: {}", batchId, uploadUrl);

            // 2. 上传文件
            log.info("步骤 2: 上传文件到 MinerU...");
            uploadFile(filePath, uploadUrl);
            log.info("文件上传成功");

            // 3. 轮询获取解析结果
            log.info("步骤 3: 轮询获取解析结果...");
            String zipUrl = pollForResult(batchId);
            log.info("解析完成，结果 ZIP URL: {}", zipUrl);

            // 4. 下载并解压 ZIP 文件
            log.info("步骤 4: 下载并解压结果文件...");
            Path extractDir = downloadAndExtractZip(zipUrl, batchId);
            log.info("结果已解压到: {}", extractDir);

            // 5. 读取 Markdown 文件
            log.info("步骤 5: 读取 Markdown 内容...");
            String markdownContent = readMarkdownFromExtractedFiles(extractDir);
            log.info("成功读取 Markdown 内容，长度: {}", markdownContent.length());

            // 6. 上传图片并替换 Markdown 中的图片链接
            log.info("步骤 6: 上传图片到 GitHub 图床...");
            String processedMarkdown = uploadImagesAndReplaceLinks(markdownContent, extractDir);
            int imageCount = countImages(markdownContent);
            log.info("成功处理 {} 张图片", imageCount);

            // 返回结果，包含处理后的 markdown 和解压目录
            return new ParseResult(processedMarkdown, extractDir);

        } catch (Exception e) {
            log.error("MinerU 文档解析失败", e);
            throw new BusinessException("MinerU 文档解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 申请上传链接
     */
    private UploadUrlResponse applyUploadUrl(String fileName) throws IOException {
        String url = minerUProperties.getFileUrlsBatchUrl();

        // 构建请求体
        JsonObject requestBody = new JsonObject();
        JsonArray filesArray = new JsonArray();
        JsonObject fileObject = new JsonObject();
        fileObject.addProperty("name", fileName);
        filesArray.add(fileObject);
        requestBody.add("files", filesArray);
        requestBody.addProperty("model_version", minerUProperties.getModelVersion());
        requestBody.addProperty("enable_formula", minerUProperties.isEnableFormula());
        requestBody.addProperty("enable_table", minerUProperties.isEnableTable());

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + minerUProperties.getApiToken())
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("申请上传链接失败: " + response.code() + " " + response.message());
            }

            String responseBody = response.body().string();
            log.debug("申请上传链接响应: {}", responseBody);

            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            int code = jsonResponse.get("code").getAsInt();

            if (code != 0) {
                String msg = jsonResponse.get("msg").getAsString();
                throw new IOException("申请上传链接失败: " + msg);
            }

            JsonObject data = jsonResponse.getAsJsonObject("data");
            String batchId = data.get("batch_id").getAsString();
            JsonArray fileUrls = data.getAsJsonArray("file_urls");
            String uploadUrl = fileUrls.get(0).getAsString();

            return new UploadUrlResponse(batchId, uploadUrl);
        }
    }

    /**
     * 上传文件
     */
    private void uploadFile(Path filePath, String uploadUrl) throws IOException {
        // 读取文件字节
        byte[] fileBytes = Files.readAllBytes(filePath);

        // 根据 MinerU 文档，上传文件时不需要设置 Content-Type
        // 所以这里 MediaType 传 null
        RequestBody body = RequestBody.create(fileBytes, null);

        Request request = new Request.Builder()
                .url(uploadUrl)
                .put(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("文件上传失败: " + response.code() + " " + response.message());
            }
            log.debug("文件上传响应状态: {}", response.code());
        }
    }

    /**
     * 轮询获取解析结果
     */
    private String pollForResult(String batchId) throws IOException, InterruptedException {
        String url = minerUProperties.getExtractResultsBatchUrl(batchId);

        int attempts = 0;
        while (attempts < minerUProperties.getMaxPollingAttempts()) {
            attempts++;

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + minerUProperties.getApiToken())
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("查询解析结果失败: " + response.code() + " " + response.message());
                }

                String responseBody = response.body().string();
                log.debug("查询解析结果响应 (第 {} 次): {}", attempts, responseBody);

                JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                int code = jsonResponse.get("code").getAsInt();

                if (code != 0) {
                    String msg = jsonResponse.get("msg").getAsString();
                    throw new IOException("查询解析结果失败: " + msg);
                }

                JsonObject data = jsonResponse.getAsJsonObject("data");
                JsonArray extractResults = data.getAsJsonArray("extract_result");

                if (extractResults.size() == 0) {
                    throw new IOException("未找到解析结果");
                }

                JsonObject result = extractResults.get(0).getAsJsonObject();
                String state = result.get("state").getAsString();

                log.info("解析状态: {} (第 {} 次查询)", state, attempts);

                if ("done".equals(state)) {
                    // 解析完成
                    return result.get("full_zip_url").getAsString();
                } else if ("failed".equals(state)) {
                    // 解析失败
                    String errMsg = result.has("err_msg") ? result.get("err_msg").getAsString() : "未知错误";
                    throw new IOException("文档解析失败: " + errMsg);
                } else if ("running".equals(state) && result.has("extract_progress")) {
                    // 正在解析，显示进度
                    JsonObject progress = result.getAsJsonObject("extract_progress");
                    int extractedPages = progress.get("extracted_pages").getAsInt();
                    int totalPages = progress.get("total_pages").getAsInt();
                    log.info("解析进度: {}/{} 页", extractedPages, totalPages);
                }

                // 等待后继续轮询
                Thread.sleep(minerUProperties.getPollingInterval());
            }
        }

        throw new IOException("解析超时，已轮询 " + attempts + " 次");
    }

    /**
     * 下载并解压 ZIP 文件
     */
    private Path downloadAndExtractZip(String zipUrl, String batchId) throws IOException {
        // 创建临时目录存放解压文件
        Path tmpDir = Paths.get(fileProperties.getTmpDir()).toAbsolutePath();
        Path extractDir = tmpDir.resolve("mineru_" + batchId);
        Files.createDirectories(extractDir);

        // 下载 ZIP 文件
        Path zipFilePath = tmpDir.resolve(batchId + ".zip");

        Request request = new Request.Builder()
                .url(zipUrl)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("下载 ZIP 文件失败: " + response.code() + " " + response.message());
            }

            // 保存 ZIP 文件
            try (InputStream inputStream = response.body().byteStream();
                 FileOutputStream outputStream = new FileOutputStream(zipFilePath.toFile())) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        }

        log.info("ZIP 文件已下载到: {}", zipFilePath);

        // 解压 ZIP 文件
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath.toFile()))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path entryPath = extractDir.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    // 确保父目录存在
                    Files.createDirectories(entryPath.getParent());

                    // 写入文件
                    try (FileOutputStream fos = new FileOutputStream(entryPath.toFile())) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zipInputStream.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zipInputStream.closeEntry();
            }
        }

        // 删除临时 ZIP 文件
        Files.deleteIfExists(zipFilePath);

        return extractDir;
    }

    /**
     * 从解压的文件中读取 Markdown 内容
     */
    private String readMarkdownFromExtractedFiles(Path extractDir) throws IOException {
        // MinerU 解析结果通常包含一个 .md 文件
        // 查找第一个 .md 文件
        try (var stream = Files.walk(extractDir)) {
            Path mdFile = stream
                    .filter(p -> p.toString().endsWith(".md"))
                    .findFirst()
                    .orElseThrow(() -> new IOException("未找到 Markdown 文件"));

            log.info("找到 Markdown 文件: {}", mdFile);
            return Files.readString(mdFile);
        }
    }

    /**
     * 上传图片到 GitHub 并替换 Markdown 中的图片链接
     */
    private String uploadImagesAndReplaceLinks(String markdownContent, Path extractDir) throws IOException {
        Matcher matcher = IMAGE_PATTERN.matcher(markdownContent);
        StringBuffer result = new StringBuffer();
        int uploadCount = 0;

        while (matcher.find()) {
            String altText = matcher.group(1);
            String imagePath = matcher.group(2);

            // 跳过已经是 HTTP/HTTPS 链接的图片
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                log.debug("跳过网络图片: {}", imagePath);
                continue;
            }

            try {
                // 解析相对路径，找到实际的图片文件
                Path imageFile = extractDir.resolve(imagePath).normalize();
                
                if (!Files.exists(imageFile)) {
                    log.warn("图片文件不存在: {}", imageFile);
                    continue;
                }

                // 上传图片到 GitHub
                log.debug("上传图片: {}", imageFile.getFileName());
                String cdnUrl = gitHubImageService.uploadImage(imageFile, imageFile.getFileName().toString());

                // 替换为 CDN 链接
                String replacement = String.format("![%s](%s)", altText, cdnUrl);
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                uploadCount++;
                log.info("图片已上传并替换: {} -> {}", imagePath, cdnUrl);

            } catch (Exception e) {
                log.error("上传图片失败: {}", imagePath, e);
                // 失败时保留原链接
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }

        matcher.appendTail(result);
        log.info("共成功上传 {} 张图片", uploadCount);
        return result.toString();
    }

    /**
     * 统计 Markdown 中的图片数量
     */
    private int countImages(String markdownContent) {
        Matcher matcher = IMAGE_PATTERN.matcher(markdownContent);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * 上传链接响应
     */
    private static class UploadUrlResponse {
        String batchId;
        String uploadUrl;

        UploadUrlResponse(String batchId, String uploadUrl) {
            this.batchId = batchId;
            this.uploadUrl = uploadUrl;
        }
    }

    /**
     * 解析结果
     */
    public static class ParseResult {
        public String markdownContent;
        public Path extractDir;

        public ParseResult(String markdownContent, Path extractDir) {
            this.markdownContent = markdownContent;
            this.extractDir = extractDir;
        }
    }
}
