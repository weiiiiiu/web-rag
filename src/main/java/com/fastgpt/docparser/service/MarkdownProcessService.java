package com.fastgpt.docparser.service;

import com.fastgpt.docparser.exception.BusinessException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 处理服务
 */
@Service
public class MarkdownProcessService {

    private static final Logger log = LoggerFactory.getLogger(MarkdownProcessService.class);

    private final GitHubImageService gitHubImageService;
    private final OkHttpClient httpClient;

    // 匹配 Markdown 图片语法：![alt](url)
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)");

    public MarkdownProcessService(GitHubImageService gitHubImageService) {
        this.gitHubImageService = gitHubImageService;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    /**
     * 处理 Markdown 内容：提取图片、上传到 GitHub、替换链接
     *
     * @param markdownContent 原始 Markdown 内容
     * @param tmpDir 临时目录
     * @return 处理后的 Markdown 内容和图片链接列表
     */
    public ProcessedMarkdown processMarkdown(String markdownContent, Path tmpDir) {
        log.info("开始处理 Markdown 内容");

        // 提取图片 URL
        List<ImageInfo> imageInfos = extractImages(markdownContent);

        if (imageInfos.isEmpty()) {
            log.info("Markdown 中没有图片");
            return new ProcessedMarkdown(markdownContent, Collections.emptyList());
        }

        log.info("找到 {} 张图片", imageInfos.size());

        // 下载图片到临时目录
        Map<String, Path> downloadedImages = downloadImages(imageInfos, tmpDir);

        // 上传图片到 GitHub
        Map<String, String> imageCdnUrls = uploadImagesToGitHub(downloadedImages);

        // 替换 Markdown 中的图片链接
        String processedMarkdown = replaceImageUrls(markdownContent, imageCdnUrls);

        // 清理临时图片文件
        cleanupTempImages(downloadedImages.values());

        log.info("Markdown 处理完成，上传了 {} 张图片", imageCdnUrls.size());

        return new ProcessedMarkdown(processedMarkdown, new ArrayList<>(imageCdnUrls.values()));
    }

    /**
     * 处理 Markdown 内容（本地图片）：上传本地图片到 GitHub、替换链接
     *
     * @param markdownContent 原始 Markdown 内容
     * @param extractDir MinerU 解压目录（包含 images 文件夹）
     * @return 处理后的 Markdown 内容和图片链接列表
     */
    public ProcessedMarkdown processMarkdownWithLocalImages(String markdownContent, Path extractDir) {
        log.info("开始处理 Markdown 内容（本地图片）");

        // 提取所有图片引用
        List<LocalImageInfo> imageInfos = extractLocalImages(markdownContent, extractDir);

        if (imageInfos.isEmpty()) {
            log.info("Markdown 中没有本地图片");
            return new ProcessedMarkdown(markdownContent, Collections.emptyList());
        }

        log.info("找到 {} 张本地图片", imageInfos.size());

        // 上传图片到 GitHub 并获取 CDN URL
        Map<String, String> imageCdnUrls = new HashMap<>();
        for (LocalImageInfo imageInfo : imageInfos) {
            try {
                String cdnUrl = gitHubImageService.uploadImage(imageInfo.localPath, imageInfo.fileName);
                imageCdnUrls.put(imageInfo.markdownPath, cdnUrl);
                log.info("上传图片成功: {} -> {}", imageInfo.fileName, cdnUrl);
            } catch (Exception e) {
                log.error("上传图片失败: {}", imageInfo.fileName, e);
                // 继续处理其他图片
            }
        }

        // 替换 Markdown 中的图片链接
        String processedMarkdown = replaceImageUrls(markdownContent, imageCdnUrls);

        log.info("Markdown 处理完成，上传了 {} 张图片", imageCdnUrls.size());

        return new ProcessedMarkdown(processedMarkdown, new ArrayList<>(imageCdnUrls.values()));
    }

    /**
     * 提取本地图片信息
     */
    private List<LocalImageInfo> extractLocalImages(String markdownContent, Path extractDir) {
        List<LocalImageInfo> imageInfos = new ArrayList<>();
        Matcher matcher = IMAGE_PATTERN.matcher(markdownContent);

        while (matcher.find()) {
            String alt = matcher.group(1);
            String imagePath = matcher.group(2);

            // 只处理相对路径（本地图片）
            if (!imagePath.startsWith("http://") && !imagePath.startsWith("https://")) {
                // 构建实际文件路径
                Path actualPath = extractDir.resolve(imagePath);

                if (Files.exists(actualPath)) {
                    String fileName = actualPath.getFileName().toString();
                    imageInfos.add(new LocalImageInfo(imagePath, actualPath, fileName));
                } else {
                    log.warn("图片文件不存在: {}", actualPath);
                }
            }
        }

        return imageInfos;
    }

    /**
     * 提取 Markdown 中的图片信息
     */
    private List<ImageInfo> extractImages(String markdownContent) {
        List<ImageInfo> imageInfos = new ArrayList<>();
        Matcher matcher = IMAGE_PATTERN.matcher(markdownContent);

        while (matcher.find()) {
            String alt = matcher.group(1);
            String url = matcher.group(2);

            // 只处理 HTTP/HTTPS 图片
            if (url.startsWith("http://") || url.startsWith("https://")) {
                imageInfos.add(new ImageInfo(alt, url));
            }
        }

        return imageInfos;
    }

    /**
     * 下载图片到临时目录
     */
    private Map<String, Path> downloadImages(List<ImageInfo> imageInfos, Path tmpDir) {
        Map<String, Path> downloadedImages = new HashMap<>();

        for (ImageInfo imageInfo : imageInfos) {
            try {
                Path imagePath = downloadImage(imageInfo.url, tmpDir);
                downloadedImages.put(imageInfo.url, imagePath);
                log.debug("图片下载成功: {}", imageInfo.url);
            } catch (IOException e) {
                log.warn("图片下载失败: {}", imageInfo.url, e);
                // 继续处理其他图片
            }
        }

        return downloadedImages;
    }

    /**
     * 下载单个图片
     */
    private Path downloadImage(String url, Path tmpDir) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("下载图片失败: " + response.code());
            }

            if (response.body() == null) {
                throw new IOException("图片内容为空");
            }

            // 生成临时文件名
            String fileName = generateTempFileName(url);
            Path imagePath = tmpDir.resolve(fileName);

            // 保存图片
            Files.write(imagePath, response.body().bytes());

            return imagePath;
        }
    }

    /**
     * 上传图片到 GitHub
     */
    private Map<String, String> uploadImagesToGitHub(Map<String, Path> downloadedImages) {
        Map<String, String> imageCdnUrls = new HashMap<>();

        for (Map.Entry<String, Path> entry : downloadedImages.entrySet()) {
            String originalUrl = entry.getKey();
            Path imagePath = entry.getValue();

            try {
                String cdnUrl = gitHubImageService.uploadImage(imagePath, imagePath.getFileName().toString());
                imageCdnUrls.put(originalUrl, cdnUrl);
                log.debug("图片上传成功: {} -> {}", originalUrl, cdnUrl);
            } catch (IOException e) {
                log.error("图片上传失败: {}", originalUrl, e);
                // 继续处理其他图片
            }
        }

        return imageCdnUrls;
    }

    /**
     * 替换 Markdown 中的图片 URL
     */
    private String replaceImageUrls(String markdownContent, Map<String, String> imageCdnUrls) {
        String result = markdownContent;

        for (Map.Entry<String, String> entry : imageCdnUrls.entrySet()) {
            String originalUrl = entry.getKey();
            String cdnUrl = entry.getValue();

            // 替换所有出现的原始 URL
            result = result.replace(originalUrl, cdnUrl);
        }

        return result;
    }

    /**
     * 清理临时图片文件
     */
    private void cleanupTempImages(Collection<Path> imagePaths) {
        for (Path imagePath : imagePaths) {
            try {
                Files.deleteIfExists(imagePath);
                log.debug("删除临时文件: {}", imagePath);
            } catch (IOException e) {
                log.warn("删除临时文件失败: {}", imagePath, e);
            }
        }
    }

    /**
     * 生成临时文件名
     */
    private String generateTempFileName(String url) {
        String fileName = url.substring(url.lastIndexOf('/') + 1);

        // 如果 URL 没有扩展名，尝试推断
        if (!fileName.contains(".")) {
            fileName += ".png";
        }

        // 添加随机前缀避免冲突
        return "temp_" + UUID.randomUUID().toString().substring(0, 8) + "_" + fileName;
    }

    /**
     * 图片信息
     */
    private static class ImageInfo {
        final String alt;
        final String url;

        ImageInfo(String alt, String url) {
            this.alt = alt;
            this.url = url;
        }
    }

    /**
     * 本地图片信息
     */
    private static class LocalImageInfo {
        final String markdownPath;  // Markdown 中的路径（如 images/xxx.jpg）
        final Path localPath;        // 实际本地文件路径
        final String fileName;       // 文件名

        LocalImageInfo(String markdownPath, Path localPath, String fileName) {
            this.markdownPath = markdownPath;
            this.localPath = localPath;
            this.fileName = fileName;
        }
    }

    /**
     * 处理后的 Markdown
     */
    public static class ProcessedMarkdown {
        public final String content;
        public final List<String> imageUrls;

        public ProcessedMarkdown(String content, List<String> imageUrls) {
            this.content = content;
            this.imageUrls = imageUrls;
        }
    }
}
