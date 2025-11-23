#  文档解析服务

一个基于 Spring Boot 的文档解析服务，支持将 Word/PDF 文档自动转换为 Markdown 格式，并将文档中的图片上传到 GitHub 图床。集成 MinerU 文档解析、阿里云百炼知识库管理和 RAG 对话功能。

## 功能特性

- **文档解析**：支持 PDF、Word (doc/docx) 格式文档
- **智能转换**：使用 MinerU AI 将文档转换为高质量 Markdown
- **自动图床**：自动提取文档图片并上传到 GitHub 仓库
- **CDN 加速**：使用 jsDelivr CDN 加速图片访问
- **知识库管理**：集成阿里云百炼知识库，支持文档上传
- **RAG 对话**：基于知识库的智能问答功能
- **可视化界面**：简洁美观的 Web 界面，支持拖拽上传
- **实时预览**：Markdown 实时渲染预览


## 技术栈

- **后端**：Spring Boot 3.2.0、Java 17
- **文档解析**：MinerU AI 文档解析服务
- **知识库**：阿里云百炼知识库 SDK
- **图床**：GitHub API + jsDelivr CDN
- **前端**：HTML5 + Thymeleaf + Marked.js
- **构建工具**：Maven
- **HTTP 客户端**：OkHttp 4.12.0

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.8+
- MinerU API Token（在 https://mineru.net 申请）
- 阿里云账号（可选，用于知识库和 RAG 功能）
- GitHub 账号和 Personal Access Token

### 2. 配置

复制 `src/main/resources/application-example.yml` 为 `application.yml`，并填入配置信息：

```yaml
# MinerU 文档解析配置
mineru:
  api-token: your-mineru-api-token   # 在 https://mineru.net 申请
  api-base-url: https://mineru.net/api/v4
  model-version: vlm                 # pipeline 或 vlm
  enable-formula: true               # 是否开启公式识别
  enable-table: true                 # 是否开启表格识别

# GitHub 图床配置
github:
  token: your-github-token           # GitHub Personal Access Token
  repo: username/repo                # 仓库路径（格式：用户名/仓库名）
  branch: main                       # 分支名
  path-prefix: img/                  # 图片存储路径前缀
  cdn: cdn.jsdelivr.net              # CDN 域名

# 阿里云百炼配置（可选，用于知识库和 RAG 功能）
aliyun:
  bailian:
    access-key-id: your-access-key-id
    access-key-secret: your-access-key-secret
    api-key: your-dashscope-api-key  # 通义千问 API Key
    workspace-id: your-workspace-id
```

### 3. 获取配置信息

#### MinerU 配置

1. 访问 [MinerU 官网](https://mineru.net)
2. 注册账号并登录
3. 在控制台获取 API Token

#### GitHub 配置

1. 登录 GitHub，进入 Settings → Developer settings → Personal access tokens
2. 生成新 Token，勾选 `repo` 权限
3. 创建或选择一个现有仓库用于存储图片

### 4. 运行

```bash
# 编译项目
mvn clean package

# 运行服务
java -jar target/docparser-1.0.0.jar

# 或直接运行
mvn spring-boot:run
```

服务启动后，访问 http://localhost:8080

### 5. 使用

1. 打开浏览器访问 http://localhost:8080
2. 上传 PDF 或 Word 文档（支持拖拽）
3. 等待解析完成（通常需要 1-3 分钟）
4. 查看 Markdown 预览或复制源码



## 开发计划

- [x] 支持 MinerU 文档解析
- [x] 集成阿里云百炼知识库
- [x] 实现 RAG 对话功能
- [x] 添加文档处理历史记录
- [ ] 支持批量文档处理
- [ ] 支持更多文档格式（PPT、Excel 等）
- [ ] 支持其他图床（阿里云 OSS、七牛云等）
- [ ] 支持自定义 Markdown 样式
- [ ] 优化解析速度和准确率

## 许可证

本项目采用 MIT 许可证。

## 贡献

欢迎提交 Issue 和 Pull Request！

## 联系方式

如有问题，请在 GitHub 上提交 Issue。
