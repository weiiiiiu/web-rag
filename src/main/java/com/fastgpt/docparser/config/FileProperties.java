package com.fastgpt.docparser.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 文件配置属性
 */
@Component
@ConfigurationProperties(prefix = "file")
public class FileProperties {

    /**
     * 临时文件目录(存储上传文件、下载图片、解压文件等)
     */
    private String tmpDir = ".tmp";

    /**
     * 允许的文件类型
     */
    private List<String> allowedTypes = Arrays.asList(
            "pdf", "doc", "docx",           // Word & PDF
            "ppt", "pptx",                  // PowerPoint
            "xls", "xlsx",                  // Excel
            "txt", "md"                    // 文本
    );

    /**
     * 文件大小限制（MB）
     */
    private int maxSize = 50;

    public String getTmpDir() {
        return tmpDir;
    }

    public void setTmpDir(String tmpDir) {
        this.tmpDir = tmpDir;
    }

    public List<String> getAllowedTypes() {
        return allowedTypes;
    }

    public void setAllowedTypes(List<String> allowedTypes) {
        this.allowedTypes = allowedTypes;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * 检查文件类型是否允许
     */
    public boolean isAllowedType(String extension) {
        if (extension == null) {
            return false;
        }
        String ext = extension.toLowerCase().replace(".", "");
        return allowedTypes.contains(ext);
    }

    /**
     * 获取允许的文件类型字符串
     */
    public String getAllowedTypesString() {
        return String.join(", ", allowedTypes);
    }
}
