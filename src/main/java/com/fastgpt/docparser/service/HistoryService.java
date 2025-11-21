package com.fastgpt.docparser.service;

import com.fastgpt.docparser.config.FileProperties;
import com.fastgpt.docparser.dto.HistoryRecord;
import com.fastgpt.docparser.dto.ParseResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 历史记录服务
 *
 * @author ZHONG WEI
 */
@Service
public class HistoryService {

    private static final Logger log = LoggerFactory.getLogger(HistoryService.class);
    private static final String HISTORY_FILE = "history.json";

    private final FileProperties fileProperties;
    private final Gson gson;
    private final Path historyFilePath;

    public HistoryService(FileProperties fileProperties) {
        this.fileProperties = fileProperties;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.historyFilePath = Paths.get(fileProperties.getResultDir()).toAbsolutePath().resolve(HISTORY_FILE);

        // 确保历史文件存在
        initHistoryFile();
    }

    /**
     * 初始化历史文件
     */
    private void initHistoryFile() {
        try {
            if (!Files.exists(historyFilePath)) {
                Files.writeString(historyFilePath, "[]");
                log.info("创建历史记录文件: {}", historyFilePath);
            }
        } catch (IOException e) {
            log.error("创建历史记录文件失败", e);
        }
    }

    /**
     * 添加历史记录
     */
    public HistoryRecord addHistory(ParseResult parseResult) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        long createdAt = System.currentTimeMillis();

        HistoryRecord record = HistoryRecord.builder()
                .id(id)
                .originalFilename(parseResult.getOriginalFilename())
                .markdownContent(parseResult.getMarkdownContent())
                .imageCount(parseResult.getImageCount())
                .filePath(parseResult.getResultFilePath())
                .processingTime(parseResult.getProcessingTime())
                .createdAt(createdAt)
                .build();

        List<HistoryRecord> records = loadHistory();
        records.add(0, record); // 添加到列表开头

        // 只保留最近 100 条记录
        if (records.size() > 100) {
            records = records.subList(0, 100);
        }

        saveHistory(records);
        log.info("添加历史记录: {}", record.getOriginalFilename());

        return record;
    }

    /**
     * 获取历史记录列表
     */
    public List<HistoryRecord> getHistoryList() {
        List<HistoryRecord> records = loadHistory();

        // 只返回列表信息，不包含 markdown 内容（减少传输大小）
        return records.stream()
                .map(r -> {
                    HistoryRecord brief = new HistoryRecord();
                    brief.setId(r.getId());
                    brief.setOriginalFilename(r.getOriginalFilename());
                    brief.setImageCount(r.getImageCount());
                    brief.setFilePath(r.getFilePath());
                    brief.setProcessingTime(r.getProcessingTime());
                    brief.setCreatedAt(r.getCreatedAt());
                    // 不设置 markdownContent
                    return brief;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取单个历史记录详情
     */
    public HistoryRecord getHistory(String id) {
        List<HistoryRecord> records = loadHistory();
        return records.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * 删除历史记录
     */
    public boolean deleteHistory(String id) {
        List<HistoryRecord> records = loadHistory();
        boolean removed = records.removeIf(r -> r.getId().equals(id));

        if (removed) {
            saveHistory(records);
            log.info("删除历史记录: {}", id);
        }

        return removed;
    }

    /**
     * 清空历史记录
     */
    public void clearHistory() {
        saveHistory(new ArrayList<>());
        log.info("清空所有历史记录");
    }

    /**
     * 从文件加载历史记录
     */
    private List<HistoryRecord> loadHistory() {
        try {
            String json = Files.readString(historyFilePath);
            List<HistoryRecord> records = gson.fromJson(json, new TypeToken<List<HistoryRecord>>(){}.getType());
            return records != null ? records : new ArrayList<>();
        } catch (Exception e) {
            log.error("加载历史记录失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 保存历史记录到文件
     */
    private void saveHistory(List<HistoryRecord> records) {
        try {
            String json = gson.toJson(records);
            Files.writeString(historyFilePath, json);
        } catch (IOException e) {
            log.error("保存历史记录失败", e);
        }
    }
}
