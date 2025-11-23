# FastGPT 文档解析服务

一个基于 Spring Boot 的文档解析服务，支持将 Word/PDF 文档自动转换为 Markdown 格式，并将文档中的图片上传到图床。

## 功能特性

-  **文档解析**：支持 PDF、Word (doc/docx) 格式文档为MD
-  **自动图床**：自动提取文档图片并上传到图床
-  **可视化界面**：简洁美观的 Web 界面，支持拖拽上传
-  **实时预览**：Markdown 实时渲染预览



## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.8+

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
 mvn clean package -DskipTests
 
# 运行
./start.sh                   
```

### 5. 使用

1. 打开浏览器访问 http://localhost:8080
2. 上传 PDF 或 Word 文档（支持拖拽）
3. 等待解析完成（通常需要 1-3 分钟）
4. 查看 Markdown 预览或复制源码



## 开发计划

- [ ] 支持批量文档处理
- [x] 支持更多文档格式（PPT、Excel 等）
- [x] 支持其他图床（阿里云 OSS、七牛云等）
- [x] 添加文档处理历史记录
- [ ] 支持自定义 Markdown 样式

## 许可证

本项目采用 MIT 许可证。

## 贡献

欢迎提交 Issue 和 Pull Request！

## 联系方式

如有问题，请在 GitHub 上提交 Issue。
