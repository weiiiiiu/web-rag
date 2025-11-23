-- 文档解析服务数据库初始化脚本

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS docparser 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 2. 使用数据库
USE docparser;

-- 3. 删除旧表（如果存在）
DROP TABLE IF EXISTS document_history;

-- 4. 创建文档历史表
CREATE TABLE document_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    original_filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    document_id VARCHAR(100) NOT NULL COMMENT '文档ID',
    knowledge_base_id VARCHAR(100) NOT NULL COMMENT '知识库ID',
    markdown_content LONGTEXT COMMENT 'Markdown内容',
    image_count INT NOT NULL DEFAULT 0 COMMENT '图片数量',
    processing_time BIGINT NOT NULL DEFAULT 0 COMMENT '处理时间（毫秒）',
    image_storage VARCHAR(20) NOT NULL DEFAULT 'github' COMMENT '图片存储策略: oss 或 github',
    parser_type VARCHAR(20) NOT NULL DEFAULT 'mineru' COMMENT '解析器类型: aliyun 或 mineru',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_document_id (document_id),
    INDEX idx_knowledge_base_id (knowledge_base_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档解析历史表';

-- 5. 查看表结构
DESCRIBE document_history;

-- 6. 查询历史记录
-- SELECT id, original_filename, document_id, parser_type, image_storage, image_count, processing_time, created_at 
-- FROM document_history 
-- ORDER BY created_at DESC 
-- LIMIT 10;
