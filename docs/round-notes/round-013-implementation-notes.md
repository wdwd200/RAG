# Round 013 Implementation Notes

## 1. 本轮是什么

本轮执行 Phase 3.4：Phase 3 收尾、处理链路接口验证与导读整理。

本轮没有新增大功能，也没有进入 embedding、Qdrant、向量检索、LLM 或 SSE。

## 2. 本轮完成了什么

本轮完成：

- 新增 `docs/phase-notes/phase-003-summary.md`。
- 更新 `README.md`，标记 Phase 3 已完成。
- 新增 `docs/round-notes/round-013-implementation-notes.md`。
- 手动验证 Phase 3 文档处理和 chunk 查询接口。
- 复查处理链路、状态机和失败处理规则。

## 3. 本轮验证了哪些接口

已通过 jar 启动后的手动验证：

```text
GET  /api/health
GET  /api/health/database
POST /api/knowledge-bases
POST /api/documents/upload
POST /api/documents/{id}/process
GET  /api/documents/{id}
GET  /api/documents/{documentId}/chunks
GET  /api/chunks/{id}
```

验证结果：

- txt 上传后 process 成功，document 状态为 `CHUNKED`。
- md 上传后 process 成功，document 状态为 `CHUNKED`。
- process 后能查到 chunk 列表。
- 能查询单个 chunk。
- 重复 process 后 `processingVersion` 从 1 递增到 2。
- 重复 process 后旧 chunk 变为 inactive。
- chunk 查询接口只返回 active chunk。
- 不支持类型时，document 状态变为 `FAILED`，`failedStage` 为 `PARSING`，`errorMessage` 不为空。
- 文件不存在时，document 状态变为 `FAILED`，`failedStage` 为 `PARSING`，`errorMessage` 不为空。

## 4. 当前文档处理链路

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
DocumentChunkService
  ↓
DocumentChunkMapper
  ↓
document_chunk 表
```

`DocumentController` 仍然只作为 HTTP 入口，不承载处理流程。

`DocumentProcessingService` 是主业务编排位置。

`DocumentParserRegistry` 根据 `fileType` 选择 parser。

`TxtDocumentParser` / `MarkdownDocumentParser` 只负责文件转文本。

`TextSplitter` 只负责文本切分。

`DocumentChunkService` 只负责 chunk 数据访问和 active 状态处理。

## 5. 当前状态机规则

允许 process：

```text
UPLOADED
FAILED
PARSED
CHUNKED
```

不允许 process：

```text
PARSING
CHUNKING
EMBEDDING
INDEXING
INDEXED
```

处理成功后：

```text
status = CHUNKED
```

处理失败后：

```text
status = FAILED
failedStage = PARSING / CHUNKING
errorMessage = 清晰错误信息
```

## 6. 当前代码主线是否清楚

当前主线清楚：

- Controller 保持薄。
- ProcessingService 承担处理编排。
- Parser 不负责切分。
- Splitter 不写数据库。
- ChunkService 不解析文件。
- Mapper 只做数据库访问。

本轮只做了导读和验证，没有进行大重构。

## 7. 本轮验证环境

本轮使用 Docker Compose 启动 PostgreSQL 和 Qdrant。

由于当前 shell 的 PATH 中没有 `docker`，验证时使用了 Docker Desktop 自带的完整路径：

```text
C:\Program Files\Docker\Docker\resources\bin\docker.exe
```

应用启动方式：

```bash
mvn clean package
java -jar target/rag-backend-0.0.1-SNAPSHOT.jar
```

验证完成后已停止本地 jar 进程。

## 8. 本轮没有做什么

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

## 9. 下一轮建议

进入 Phase 4.1：EmbeddingClient 抽象、Mock Embedding 与向量维度配置。
