# Round 012 Implementation Notes

## 1. 本轮完成了什么

本轮执行 Phase 3.3：处理失败状态、重新处理与文档状态机补强。

本轮没有新增 embedding、Qdrant、向量检索、LLM 或 SSE。重点是让已有文档处理链路在重复处理、失败重试、版本记录和 chunk active 状态上更稳。

## 2. 本轮补强的状态机规则

`POST /api/documents/{id}/process` 现在会先校验 document 当前状态。

允许 process 的状态：

```text
UPLOADED
FAILED
PARSED
CHUNKED
```

不允许 process 的状态：

```text
PARSING
CHUNKING
EMBEDDING
INDEXING
INDEXED
```

不允许处理时返回：

```text
DOCUMENT_PROCESS_NOT_ALLOWED
```

这样可以避免正在处理中的文档被重复处理，也避免未来已经 `INDEXED` 的文档被重新切分后与向量索引不一致。

## 3. processingVersion 规则

本轮明确版本规则：

```text
首次处理 UPLOADED：沿用 document 当前 processingVersion，通常是 1
从 FAILED 重新处理：processingVersion + 1
从 PARSED 重新处理：processingVersion + 1
从 CHUNKED 重新处理：processingVersion + 1
```

处理成功后：

```text
document.processingVersion = 新版本
document_chunk.processingVersion = 同一个新版本
```

本轮不做复杂版本迁移，不变更历史 document 版本表。

## 4. 旧 chunk 如何软失效

上一轮重复处理时使用物理删除旧 chunk。本轮改为软失效：

```text
将旧 active chunk 的 is_active 改为 false
插入新 chunk，is_active 为 true
```

查询接口：

```text
GET /api/documents/{documentId}/chunks
GET /api/chunks/{id}
```

只返回 active chunk。

这样旧 chunk 会保留在数据库中，便于后续评测、版本追踪和问题排查。

## 5. 失败状态如何记录

失败时 document 会记录：

```text
status = FAILED
failedStage = 失败阶段
errorMessage = 清晰错误信息
```

当前失败阶段：

```text
PARSING
CHUNKING
```

本轮至少覆盖：

```text
文件不存在 -> FAILED + PARSING
不支持文件类型 -> FAILED + PARSING
splitter 参数错误 -> FAILED + CHUNKING
```

`errorMessage` 使用业务异常消息或异常简短消息，不写入 Java 堆栈。

失败后的 document 允许再次调用 process。重新处理时会按 FAILED 规则递增 `processingVersion`。

## 6. 当前事务边界如何设计

文档处理成功主线使用 `TransactionTemplate` 包住：

```text
状态更新
processingVersion 更新
旧 chunk 软失效
新 chunk 入库
chunkCount 更新
CHUNKED 状态更新
```

如果处理主流程失败，事务会回滚，避免出现部分 chunk 入库或 document 状态半更新。

失败状态记录在主事务回滚后单独执行：

```text
FAILED
failedStage
errorMessage
```

这样做的原因是：如果把失败标记放在同一个回滚事务里，异常抛出后 `FAILED` 状态也会被回滚，前端和后续排查就看不到失败原因。

## 7. 处理链路

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
TextSplitter
  ↓
旧 chunk 软失效
  ↓
新 chunk 入库
  ↓
document 状态变为 CHUNKED
```

## 8. 新增或修改文件

修改文件：

- `src/main/java/com/example/ragbackend/document/service/impl/DocumentProcessingServiceImpl.java`
- `src/main/java/com/example/ragbackend/chunk/service/DocumentChunkService.java`
- `src/main/java/com/example/ragbackend/chunk/service/impl/DocumentChunkServiceImpl.java`
- `src/main/java/com/example/ragbackend/chunk/controller/DocumentChunkController.java`
- `src/test/java/com/example/ragbackend/document/DocumentProcessingControllerTest.java`
- `README.md`

新增文件：

- `src/test/java/com/example/ragbackend/document/DocumentProcessingInvalidChunkConfigTest.java`
- `docs/round-notes/round-012-implementation-notes.md`

## 9. 测试覆盖

本轮新增或更新测试覆盖：

- `UPLOADED` 文档处理成功，状态为 `CHUNKED`。
- `CHUNKED` 文档重新处理后 `processingVersion` 递增。
- 重新处理后旧 chunk 变为 inactive。
- chunk 列表只返回 active chunk。
- 文件不存在时处理失败，状态为 `FAILED`，`failedStage` 为 `PARSING`。
- 失败后恢复文件可以再次 process。
- 不支持类型处理失败，状态为 `FAILED`，`failedStage` 为 `PARSING`。
- 切分参数不合法时处理失败，状态为 `FAILED`，`failedStage` 为 `CHUNKING`。
- `INDEXED` 状态文档不允许重新处理。

## 10. 本轮不做

- 不做 embedding。
- 不写入 Qdrant。
- 不做向量入库。
- 不做向量检索。
- 不做 LLM。
- 不做 SSE。
- 不做 Redis / Elasticsearch / RabbitMQ。
- 不做异步处理。
- 不做复杂重试队列。
- 不做 PDF / docx 解析。
- 不做 Spring Security / JWT。

## 11. 下一轮建议

进入 Phase 3.4：Phase 3 收尾、处理链路接口验证与导读整理。
