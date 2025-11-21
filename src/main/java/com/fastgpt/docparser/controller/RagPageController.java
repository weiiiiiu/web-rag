package com.fastgpt.docparser.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * RAG 页面控制器
 *
 * @author ZHONG WEI
 */
@Controller
public class RagPageController {

    /**
     * RAG 对话页面
     */
    @GetMapping("/rag")
    public String ragPage() {
        return "rag";
    }
}
