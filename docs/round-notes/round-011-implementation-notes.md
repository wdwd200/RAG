# Round 011 Implementation Notes

## 1. 本轮完成了什么

本轮执行 Phase 3.2：TextSplitter、文档处理服务与 chunk 入库。

本轮把已上传的 txt/md 文档从原始本地文件处理成 `document_chunk` 数据。处理成功后，document 的最终状态为 `CHUNKED`。

本轮不做 embedding，不写入 Qdrant，不做向量检索、LLM 或 SSE。

## 2. 新增或修改了哪些文件

新增文件：

- `src/main/java/com/example/ragbackend/chunk/config/ChunkProperties.java`
- `src/main/java/com/example/ragbackend/chunk/controller/DocumentChunkController.java`
- `src/main/java/com/example/ragbackend/document/dto/DocumentProcessingResponse.java`
- `src/main/java/com/example/ragbackend/document/service/DocumentProcessingService.java`
- `src/main/java/com/example/ragbackend/document/service/impl/DocumentProcessingServiceImpl.java`
- `src/main/java/com/example/ragbackend/splitter/TextSplitter.java`
- `src/main/java/com/example/ragbackend/splitter/SplitOptions.java`
- `src/main/java/com/example/ragbackend/splitter/TextChunk.java`
- `src/main/java/com/example/ragbackend/splitter/FixedWindowTextSplitter.java`
- `src/test/java/com/example/ragbackend/document/DocumentProcessingControllerTest.java`
- `src/test/java/com/example/ragbackend/splitter/FixedWindowTextSplitterTest.java`
- `docs/round-notes/round-011-implementation-notes.md`

修改文件：

- `src/main/java/com/example/ragbackend/RagBackendApplication.java`
- `src/main/java/com/example/ragbackend/document/controller/DocumentController.java`
- `src/main/java/com/example/ragbackend/chunk/service/impl/DocumentChunkServiceImpl.java`
- `src/main/resources/application.yml`
- `src/test/resources/application-test.yml`
- `.env.example`
- `README.md`

## 3. 每个核心文件的职责

`TextSplitter`：文本切分抽象，只负责把文本切成 `TextChunk`。

`SplitOptions`：切分参数，包括 `chunkSize` 和 `overlap`。

`TextChunk`：切分后的内存模型，包括 chunk index、内容和简单 token 估算。

`FixedWindowTextSplitter`：第一版固定窗口切分实现，校验 `overlap < chunkSize`。

`ChunkProperties`：绑定 `app.chunk.size` 和 `app.chunk.overlap`。

`DocumentProcessingService` / `DocumentProcessingServiceImpl`：文档处理主业务编排位置，负责状态流转、文件定位、parser 调用、splitter 调用、chunk 入库和失败状态记录。

`DocumentProcessingResponse`：`POST /api/documents/{id}/process` 的响应 DTO。

`DocumentChunkController`：chunk 查询 HTTP 入口，只调用 `DocumentChunkService`。

`DocumentChunkServiceImpl`：继续负责 chunk 数据访问；本轮补充只返回 active chunk 的查询条件。

## 4. 新增接口

```text
POST /api/documents/{id}/process
GET /api/documents/{documentId}/chunks
GET /api/chunks/{id}
```

`POST /api/documents/{id}/process` 成功后返回：

```text
documentId
status
chunkCount
processingVersion
```

成功状态必须是 `CHUNKED`，不是 `INDEXED`。

## 5. 文档处理调用链

```text
POST /api/documents/{id}/process
  ↓
DocumentController
  ↓
DocumentProcessingService
  ↓
DocumentParserRegistry
  ↓
TxtDocumentParser / MarkdownDocumentParser
  ↓
TextSplitter
  ↓
DocumentChunkService
  ↓
DocumentChunkMapper
  ↓
document_chunk 表
```

## 6. 状态流转

成功主线：

```text
UPLOADED
  ↓
PARSING
  ↓
PARSED
  ↓
CHUNKING
  ↓
CHUNKED
```

失败时：

```text
FAILED
```

失败时会记录：

- `failedStage`
- `errorMessage`

本轮失败记录是基础版本，下一轮可以继续补强状态机和重新处理策略。

## 7. chunk 入库规则

写入 `document_chunk` 时：

- `knowledge_base_id` 来自 document。
- `document_id` 来自 document。
- `chunk_index` 从 0 开始。
- `content` 来自 `TextChunk.content`。
- `content_hash` 使用 SHA-256。
- `processing_version` 使用 document 当前 `processingVersion`。
- `is_active` 为 true。
- `token_count` 使用简单估算。
- `vector_id` 为空。
- `page_number` 为空。
- `metadata_json` 为空。

重复处理同一文档时，当前实现选择先物理删除该 document 下旧 chunk，再插入新 chunk。这是当前阶段最简单清楚的实现。后续如需要保留历史版本，可以改为把旧 chunk 标记为 inactive。

## 8. 新增配置项

`application.yml`：

```yaml
app:
  chunk:
    size: ${APP_CHUNK_SIZE:800}
    overlap: ${APP_CHUNK_OVERLAP:100}
```

`.env.example` 同步新增：

```text
APP_CHUNK_SIZE=800
APP_CHUNK_OVERLAP=100
```

`APP_CHUNK_OVERLAP` 必须小于 `APP_CHUNK_SIZE`，否则 splitter 会返回 `INVALID_CHUNK_OPTIONS`。

## 9. 如何运行和验证

运行自动测试：

```bash
mvn test
```

检查 diff 空白问题：

```bash
git diff --check
```

本轮测试覆盖：

- `FixedWindowTextSplitter` 按 `chunkSize / overlap` 切分文本。
- overlap 不合法时返回 `INVALID_CHUNK_OPTIONS`。
- txt 文档处理后状态变为 `CHUNKED`。
- txt 文档处理后生成 chunks。
- md 文档处理后生成 chunks。
- `GET /api/documents/{id}/chunks` 可以查到 chunk 列表。
- `GET /api/chunks/{id}` 可以查到单个 chunk。
- 不存在 document 处理失败。
- 不支持文件类型处理失败，并记录 `FAILED` 状态。

## 10. 本轮边界

本轮只做到：

```text
文件解析
  ↓
文本切分
  ↓
chunk 入库
```

本轮刻意不做：

- embedding
- Qdrant Java 客户端
- 向量入库
- 向量检索
- LLM
- SSE
- Redis / Elasticsearch / RabbitMQ
- 异步处理
- 复杂重试
- PDF / docx 解析
- Spring Security / JWT

## 11. 下一轮建议

进入 Phase 3.3：处理失败状态、重新处理与文档状态机补强。

下一轮建议重点处理：

- 重复处理时是否增加 `processingVersion`。
- 是否把旧 chunk 标记为 inactive 而不是物理删除。
- 失败状态和失败阶段枚举化。
- 文件不存在、unsupported type、parser 失败、chunk 入库失败的状态机细节。
