# Phase 003 Summary

## 1. Phase 3 完成了什么

Phase 3 完成了文档解析与 chunk 入库主链路。

当前系统已经具备：

- `document_chunk` 表。
- txt / md 本地文件解析。
- 固定窗口文本切分。
- 文档处理接口 `POST /api/documents/{id}/process`。
- chunk 查询接口。
- document 处理状态机。
- `processingVersion` 递增规则。
- 重复处理时旧 chunk 软失效。
- 失败时记录 `FAILED`、`failedStage` 和 `errorMessage`。

文档处理成功后的最终状态是 `CHUNKED`，不是 `INDEXED`。

## 2. 当前目录结构

Parser 相关：

```text
src/main/java/com/example/ragbackend/parser
├── DocumentParser.java
├── ParsedDocument.java
├── DocumentParserRegistry.java
├── UnsupportedDocumentTypeException.java
├── AbstractTextDocumentParser.java
├── TxtDocumentParser.java
└── MarkdownDocumentParser.java
```

Splitter 相关：

```text
src/main/java/com/example/ragbackend/splitter
├── TextSplitter.java
├── SplitOptions.java
├── TextChunk.java
└── FixedWindowTextSplitter.java
```

Chunk 相关：

```text
src/main/java/com/example/ragbackend/chunk
├── config/ChunkProperties.java
├── controller/DocumentChunkController.java
├── dto/DocumentChunkResponse.java
├── entity/DocumentChunk.java
├── mapper/DocumentChunkMapper.java
└── service
    ├── DocumentChunkService.java
    └── impl/DocumentChunkServiceImpl.java
```

Processing 相关：

```text
src/main/java/com/example/ragbackend/document
├── controller/DocumentController.java
├── dto/DocumentProcessingResponse.java
├── service/DocumentProcessingService.java
└── service/impl/DocumentProcessingServiceImpl.java
```

## 3. 文档处理主链路

```text
POST /api/documents/{id}/process
  ↓
DocumentController
  ↓
DocumentProcessingService
  ↓
状态校验
  ↓
processingVersion 准备
  ↓
DocumentParserRegistry
  ↓
TxtDocumentParser / MarkdownDocumentParser
  ↓
TextSplitter
  ↓
旧 chunk 软失效
  ↓
新 chunk 入库
  ↓
document 状态变为 CHUNKED
```

## 4. 状态机规则

允许处理：

```text
UPLOADED
FAILED
PARSED
CHUNKED
```

不允许处理：

```text
PARSING
CHUNKING
EMBEDDING
INDEXING
INDEXED
```

不允许处理时返回 `DOCUMENT_PROCESS_NOT_ALLOWED`。

## 5. processingVersion 规则

```text
UPLOADED 首次处理：沿用 document 当前 processingVersion，通常是 1
FAILED / PARSED / CHUNKED 重新处理：processingVersion + 1
新 chunk 的 processingVersion 与 document.processingVersion 一致
```

## 6. 旧 chunk 软失效规则

重复处理同一 document 时：

```text
旧 active chunk -> is_active = false
新 chunk -> is_active = true
```

chunk 查询接口只返回 active chunk。

保留旧 chunk 是为了后续评测、版本追踪和问题排查。

## 7. 失败处理规则

失败时 document 会记录：

```text
status = FAILED
failedStage = PARSING / CHUNKING
errorMessage = 清晰错误信息
```

当前覆盖：

- 文件不存在：`FAILED + PARSING`
- 不支持文件类型：`FAILED + PARSING`
- splitter 参数错误或切分失败：`FAILED + CHUNKING`

失败后可以再次调用 process。重新处理会递增 `processingVersion`。

## 8. 核心文件职责

`DocumentController`：HTTP 入口，只调用 service，不承载处理细节。

`DocumentProcessingService`：文档处理流程编排，集中处理状态校验、版本准备、解析、切分、chunk 入库和失败状态。

`DocumentParserRegistry`：根据 `fileType` 选择 parser。

`TxtDocumentParser` / `MarkdownDocumentParser`：文件转文本。Markdown 当前按普通 UTF-8 文本读取。

`TextSplitter`：文本切分抽象。

`FixedWindowTextSplitter`：固定窗口切分实现，校验 `chunkSize` 和 `overlap`。

`DocumentChunkService`：chunk 数据访问和 active 状态处理。

`DocumentChunkController`：chunk 查询入口。

`DocumentMapper` / `DocumentChunkMapper`：数据库访问。

## 9. 如何启动项目

启动本地基础设施：

```bash
docker compose up -d
```

打包：

```bash
mvn clean package
```

启动 jar：

```bash
java -jar target/rag-backend-0.0.1-SNAPSHOT.jar
```

## 10. 如何验证接口

基础验证：

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/api/health/database
```

创建知识库：

```bash
curl -X POST http://localhost:8080/api/knowledge-bases \
  -H "Content-Type: application/json" \
  -d '{"name":"Phase 3 验证知识库","description":"本地验证","ownerId":1,"visibility":"PRIVATE"}'
```

上传文档：

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "knowledgeBaseId=1" \
  -F "createdBy=1" \
  -F "file=@/path/to/demo.txt"
```

处理文档：

```bash
curl -X POST http://localhost:8080/api/documents/1/process
```

查询 document：

```bash
curl http://localhost:8080/api/documents/1
```

查询 chunks：

```bash
curl http://localhost:8080/api/documents/1/chunks
curl http://localhost:8080/api/chunks/1
```

## 11. 当前还没有做什么

- 未实现 embedding。
- 未写入 Qdrant。
- 未实现向量入库。
- 未实现向量检索。
- 未实现 LLM。
- 未实现 SSE。
- 未实现 Redis / Elasticsearch / RabbitMQ。
- 未实现异步处理。
- 未实现复杂重试队列。
- 未实现 PDF / docx 解析。
- 未实现 Spring Security / JWT。

## 12. 下一阶段 Phase 4 要做什么

Phase 4 进入 Embedding + Qdrant 基础建设。

建议从 Phase 4.1 开始：

```text
EmbeddingClient 抽象
Mock Embedding
向量维度配置
```

当前版本还没有向量入库或向量检索能力。
