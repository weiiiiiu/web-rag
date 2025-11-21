# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.2.0 document parsing service that converts Word/PDF documents to Markdown using the MinerU AI service, with automatic image hosting on GitHub and RAG (Retrieval-Augmented Generation) capabilities powered by Aliyun Bailian knowledge base.

**Key Technologies:**
- Java 17
- Spring Boot 3.2.0
- MinerU API for document parsing
- GitHub API + jsDelivr CDN for image hosting
- Aliyun Bailian SDK for knowledge base and RAG chat
- OkHttp 4.12.0 for HTTP requests
- Thymeleaf for web UI

## Build and Run Commands

### Build
```bash
# Clean and build (skip tests)
mvn clean package -DskipTests

# Build with tests
mvn clean package
```

### Run
```bash
# Using Maven
mvn spring-boot:run

# Using start script (validates environment first)
./start.sh

# Using JAR directly
java -jar target/docparser-1.0.0.jar
```

Default server port: 8080
- Main UI: http://localhost:8080
- RAG UI: http://localhost:8080/rag

## Configuration Setup

The application requires `src/main/resources/application.yml` (not tracked in git). Copy from `application-example.yml`:

```bash
cp src/main/resources/application-example.yml src/main/resources/application.yml
```

**Required configurations:**
- `mineru.api-token`: MinerU API token from https://mineru.net
- `github.token`: GitHub Personal Access Token with `repo` permission
- `github.repo`: Format `username/repository`
- `aliyun.bailian.access-key-id/secret`: For RAG features
- `aliyun.bailian.workspace-id`: Bailian workspace ID

## Architecture

### Core Service Flow

**Document Parsing Pipeline (DocumentParseService):**
1. Validate uploaded file (type/size)
2. Save to `./tmp` directory
3. Submit to MinerU API → get batch_id
4. Poll MinerU for completion (returns ZIP URL)
5. Download and extract ZIP (contains Markdown + images)
6. Upload images to GitHub → replace Markdown image URLs with CDN links
7. Save final Markdown to `./results`
8. Save to history (HistoryService)
9. Clean up temporary files

**MinerU Integration (MinerUDocParserService):**
- `applyUploadUrl()`: Request upload URL with batch_id
- `uploadFile()`: PUT file to pre-signed URL
- `pollForResult()`: Poll `/extract/results/batch/{id}` until state="done"
- `downloadAndExtractZip()`: Download result ZIP and extract
- `uploadImagesAndReplaceLinks()`: Process Markdown images via regex

**RAG Knowledge Base (AliyunBailianService + KnowledgeBaseService):**
- Knowledge base creation and document upload
- Integration with Aliyun Bailian SDK
- Document indexing and retrieval
- RagChatService handles Q&A against knowledge base

### Key Service Responsibilities

- **DocumentParseService**: Orchestrates entire parsing workflow
- **MinerUDocParserService**: Handles MinerU API communication and polling
- **MarkdownProcessService**: Processes Markdown content and local images
- **GitHubImageService**: Uploads images to GitHub repository
- **AliyunBailianService**: Base client for Aliyun Bailian SDK
- **KnowledgeBaseService**: Knowledge base CRUD operations
- **RagChatService**: RAG-based conversational interface
- **HistoryService**: Maintains processing history

### Controllers

- **IndexController** (`/`): Main document upload UI
- **DocumentController** (`/api/document`): Document parsing API
- **RagPageController** (`/rag`): RAG chat UI
- **KnowledgeBaseController** (`/api/knowledge-base`): Knowledge base management
- **RagChatController** (`/api/rag-chat`): RAG chat API
- **DiagnosticController** (`/api/diagnostic`): System diagnostics

## Important Implementation Details

### Image Processing Pattern
When processing Markdown images, the service uses regex pattern:
```java
Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)")
```
This finds all `![alt](url)` patterns. Only local relative paths are uploaded to GitHub; existing HTTP/HTTPS URLs are preserved.

### File Storage Paths
All paths use relative directories (configurable in application.yml):
- `file.tmp-dir`: Default `./tmp` (temporary uploads)
- `file.result-dir`: Default `./results` (final Markdown files)

Paths are resolved to absolute at runtime via `Paths.get().toAbsolutePath()`.

### MinerU Polling Strategy
- Max attempts: `mineru.max-polling-attempts` (default varies)
- Polling interval: `mineru.polling-interval` milliseconds
- States: `running` → `done` (success) or `failed` (error)
- Progress available during `running` state

### GitHub CDN URL Format
Images uploaded to GitHub use jsDelivr CDN:
```
https://cdn.jsdelivr.net/gh/{owner}/{repo}@{branch}/{path-prefix}/{date}/{filename}
```

### Configuration Properties Classes
- **MinerUProperties**: MinerU API settings
- **GitHubProperties**: GitHub image hosting settings
- **AliyunBailianProperties**: Aliyun Bailian RAG settings
- **FileProperties**: File storage and validation settings

All use `@ConfigurationProperties` with `@ConfigurationPropertiesScan` on main application class.

## Error Handling

- **BusinessException**: Custom business logic exception
- **GlobalExceptionHandler**: Centralized exception handling for REST APIs
- All API responses use `ApiResponse<T>` wrapper with `code`, `message`, `data`

## Development Considerations

### When Adding New Document Formats
1. Update `file.allowed-types` in application.yml
2. MinerU handles parsing; no code changes needed if MinerU supports the format
3. Update validation in FileProperties

### When Modifying Image Processing
- Image extraction happens in MinerUDocParserService
- Image uploading is in GitHubImageService
- Link replacement uses regex in MarkdownProcessService
- All local images must be resolved relative to extractDir

### When Adding RAG Features
- Extend KnowledgeBaseService for knowledge base operations
- Extend RagChatService for chat features
- All Aliyun SDK calls go through AliyunBailianService client
- Workspace ID and App ID required for most operations

### Testing Local Changes
The service creates directories automatically on startup. For testing:
1. Ensure `application.yml` exists with valid credentials
2. Test files under 50MB (configurable via `file.max-size`)
3. Supported formats: PDF, DOC, DOCX (configurable)
4. Check logs for detailed MinerU polling status

## File Cleanup Strategy
- Temporary uploaded files deleted after processing
- MinerU extract directories (`./tmp/mineru_*`) cleaned recursively
- Result files in `./results` are permanent (manual cleanup)
- Cleanup happens in DocumentParseService after building ParseResult
