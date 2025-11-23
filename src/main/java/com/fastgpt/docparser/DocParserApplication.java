package com.fastgpt.docparser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 文档解析服务
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
        System.out.println("  FastGPT 文档解析服务启动成功！");
        System.out.println("  访问地址: http://localhost:8080");
        System.out.println("========================================\n");
    }
}
