# FastGPT 文档解析服务

一个基于 Spring Boot 的文档解析服务，支持将 Word/PDF 文档自动转换为 Markdown 格式，并将文档中的图片上传到 GitHub 图床。

## 功能特性

- 📄 **文档解析**：支持 PDF、Word (doc/docx) 格式文档
- 🤖 **智能转换**：使用阿里云文档解析（大模型版）将文档转换为 Markdown
- 🖼️ **自动图床**：自动提取文档图片并上传到 GitHub 仓库
- 🔗 **CDN 加速**：使用 jsDelivr CDN 加速图片访问
- 🎨 **可视化界面**：简洁美观的 Web 界面，支持拖拽上传
- 📋 **实时预览**：Markdown 实时渲染预览

## 技术栈

- **后端**：Spring Boot 3.2.0、Java 17
- **文档解析**：阿里云文档解析 SDK
- **图床**：GitHub API + jsDelivr CDN
- **前端**：HTML5 + Thymeleaf + Marked.js
- **构建工具**：Maven

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.8+
- 阿里云账号（开通文档解析服务）
- GitHub 账号和 Personal Access Token

### 2. 配置

复制 `src/main/resources/application-example.yml` 为 `application.yml`，并填入配置信息：

```yaml
# 阿里云配置
aliyun:
  access-key-id: your-aliyun-access-key-id
  access-key-secret: your-aliyun-access-key-secret

# GitHub 图床配置
github:
  token: your-github-token          # GitHub Personal Access Token
  repo: username/repo                # 仓库路径（格式：用户名/仓库名）
  branch: main                       # 分支名
  path-prefix: images/               # 图片存储路径前缀
  cdn: cdn.jsdelivr.net             # CDN 域名
```

### 3. 获取配置信息

#### 阿里云配置

1. 登录[阿里云控制台](https://home.console.aliyun.com/)
2. 进入 AccessKey 管理页面，创建 AccessKey
3. 开通[文档解析服务](https://www.aliyun.com/product/docmind-api)

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

## 项目结构

```
web/
├── tmp/                              # 临时文件存储
├── results/                          # Markdown 结果存储
├── src/
│   ├── main/
│   │   ├── java/com/fastgpt/docparser/
│   │   │   ├── config/               # 配置类
│   │   │   │   ├── AliyunProperties.java
│   │   │   │   ├── GitHubProperties.java
│   │   │   │   └── FileProperties.java
│   │   │   ├── controller/           # 控制器
│   │   │   │   ├── IndexController.java
│   │   │   │   └── DocumentController.java
│   │   │   ├── service/              # 业务逻辑
│   │   │   │   ├── AliyunDocParserService.java
│   │   │   │   ├── GitHubImageService.java
│   │   │   │   ├── MarkdownProcessService.java
│   │   │   │   └── DocumentParseService.java
│   │   │   ├── dto/                  # 数据传输对象
│   │   │   │   ├── ApiResponse.java
│   │   │   │   └── ParseResult.java
│   │   │   ├── exception/            # 异常处理
│   │   │   │   ├── BusinessException.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   └── DocParserApplication.java
│   │   └── resources/
│   │       ├── application.yml       # 配置文件
│   │       ├── templates/            # 前端模板
│   │       │   └── index.html
│   │       └── static/               # 静态资源
├── pom.xml                           # Maven 配置
└── README.md                         # 项目说明
```

## API 接口

### 解析文档

**请求**

```http
POST /api/document/parse
Content-Type: multipart/form-data

file: <文件>
```

**响应**

```json
{
  "code": 200,
  "message": "解析成功",
  "data": {
    "originalFilename": "example.pdf",
    "markdownContent": "# 标题\n\n内容...\n\n![img](https://cdn.jsdelivr.net/...)",
    "imageCount": 3,
    "imageUrls": [
      "https://cdn.jsdelivr.net/gh/user/repo@main/images/20231121/abc_image1.png",
      "https://cdn.jsdelivr.net/gh/user/repo@main/images/20231121/def_image2.png"
    ],
    "resultFilePath": "web/results/example_20231121_143052.md",
    "processingTime": 125340
  }
}
```

### 健康检查

```http
GET /api/document/health
```

## 工作流程

```
用户上传文档
    ↓
保存到 web/tmp
    ↓
调用阿里云文档解析 API
    ↓
获取 Markdown 和图片 URL
    ↓
下载图片到临时目录
    ↓
上传图片到 GitHub 仓库
    ↓
替换 Markdown 中的图片链接为 CDN 链接
    ↓
保存最终结果到 web/results
    ↓
返回结果给用户
```

## 注意事项

1. **文件大小限制**：默认最大支持 50MB 文件
2. **支持格式**：PDF、DOC、DOCX
3. **图片格式**：支持常见图片格式（PNG、JPG、GIF 等）
4. **网络要求**：需要能够访问阿里云 API 和 GitHub API
5. **费用**：阿里云文档解析服务按量计费，请注意成本控制

## 配置说明

### 修改端口

在 `application.yml` 中修改：

```yaml
server:
  port: 8080  # 修改为其他端口
```

### 修改文件大小限制

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB      # 单个文件大小
      max-request-size: 50MB   # 请求总大小
```

### 修改文件存储路径

```yaml
file:
  tmp-dir: web/tmp           # 临时目录
  result-dir: web/results    # 结果目录
```

## 常见问题

### 1. 阿里云 API 调用失败

- 检查 AccessKey 是否正确
- 确认已开通文档解析服务
- 检查网络连接

### 2. GitHub 图片上传失败

- 检查 Token 是否有 `repo` 权限
- 确认仓库路径格式正确（username/repo）
- 检查网络是否能访问 GitHub API

### 3. 图片显示不出来

- GitHub 仓库必须是公开的
- 检查 CDN 链接是否正确
- 尝试刷新 jsDelivr 缓存

## 开发计划

- [ ] 支持批量文档处理
- [ ] 支持更多文档格式（PPT、Excel 等）
- [ ] 支持其他图床（阿里云 OSS、七牛云等）
- [ ] 添加文档处理历史记录
- [ ] 支持自定义 Markdown 样式

## 许可证

本项目采用 MIT 许可证。

## 贡献

欢迎提交 Issue 和 Pull Request！

## 联系方式

如有问题，请在 GitHub 上提交 Issue。
