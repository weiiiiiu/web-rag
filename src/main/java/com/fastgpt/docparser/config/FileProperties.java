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
     * 临时文件目录
     */
    private String tmpDir = "web/tmp";

    /**
     * 结果文件目录
     */
    private String resultDir = "web/results";

    /**
     * 允许的文件类型
     */
    private List<String> allowedTypes = Arrays.asList("pdf", "doc", "docx");

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

    public String getResultDir() {
        return resultDir;
    }

    public void setResultDir(String resultDir) {
        this.resultDir = resultDir;
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
