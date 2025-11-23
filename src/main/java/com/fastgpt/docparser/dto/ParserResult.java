package com.fastgpt.docparser.dto;

import java.nio.file.Path;

/**
 * 文档解析器统一返回结果
 * 
 * @author ZHONG WEI
 */
public class ParserResult {
    
    /** Markdown 内容 */
    private String markdownContent;
    
    /** 解压目录(可选,仅 MinerU 使用) */
    private Path extractDir;

    public ParserResult(String markdownContent, Path extractDir) {
        this.markdownContent = markdownContent;
        this.extractDir = extractDir;
    }

    /**
     * 创建阿里云解析结果(无需解压目录)
     */
    public static ParserResult ofAliyun(String markdownContent) {
        return new ParserResult(markdownContent, null);
    }

    /**
     * 创建 MinerU 解析结果(包含解压目录)
     */
    public static ParserResult ofMinerU(String markdownContent, Path extractDir) {
        return new ParserResult(markdownContent, extractDir);
    }

    public String getMarkdownContent() {
        return markdownContent;
    }

    public void setMarkdownContent(String markdownContent) {
        this.markdownContent = markdownContent;
    }

    public Path getExtractDir() {
        return extractDir;
    }

    public void setExtractDir(Path extractDir) {
        this.extractDir = extractDir;
    }
}
