package com.fastgpt.docparser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 *  文档解析服务
 *
 * 功能：
 * 1. 支持 Word/PDF 文档上传
 * 2. 使用 MinerU 文档解析服务转换为 Markdown
 * 3. 自动提取图片并上传到 GitHub 图床
 * 4. 返回带 CDN 链接的 Markdown 内容
 *
 * @author ZHONG WEI
 * @version 1.0.0
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DocParserApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocParserApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("  RAG启动成功！");
        System.out.println("  访问地址: http://localhost:8080/rag");
        System.out.println("========================================\n");
    }
}
