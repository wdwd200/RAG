# Round 016 Implementation Notes

## 1. 本轮是什么

本轮执行 Phase 4.3：chunk embedding、向量入库与 document 状态到 `INDEXED`。

本轮目标是把已经 `CHUNKED` 的文档 active chunks 生成 embedding，并写入 `VectorStoreService`。全部 chunk 写入成功后，document 状态从 `CHUNKED` 流转到 `INDEXED`。

## 2. 本轮完成了什么

- 新增 `DocumentIndexingService`。
- 新增 `DocumentIndexingServiceImpl`。
- 新增 `POST /api/documents/{id}/index`。
- `DocumentChunkService` 新增 `updateVectorId(chunkId, vectorId)`。
- active chunks 按 `chunkIndex` 升序生成 embedding。
- 调用 `VectorStoreService.ensureCollection()` 初始化 / 检查 collection。
- 调用 `VectorStoreService.upsert()` 写入 chunk vector。
- upsert 成功后写回 `document_chunk.vector_id`。
- 全部 active chunks 成功后更新 document 状态为 `INDEXED`。
- embedding 失败时记录 `FAILED + failedStage=EMBEDDING`。
- VectorStore / Qdrant 写入失败时记录 `FAILED + failedStage=INDEXING`。
- 新增文档索引服务测试和 `/index` 接口测试。
- 更新 `README.md` 和本轮实现说明。

## 3. 新增或修改的文件

```text
README.md
docs/round-notes/round-016-implementation-notes.md
src/main/java/com/example/ragbackend/chunk/service/DocumentChunkService.java
src/main/java/com/example/ragbackend/chunk/service/impl/DocumentChunkServiceImpl.java
src/main/java/com/example/ragbackend/document/controller/DocumentController.java
src/main/java/com/example/ragbackend/document/service/DocumentIndexingService.java
src/main/java/com/example/ragbackend/document/service/impl/DocumentIndexingServiceImpl.java
src/test/java/com/example/ragbackend/document/DocumentIndexingServiceTest.java
src/test/java/com/example/ragbackend/document/DocumentProcessingControllerTest.java
```

## 4. 核心文件职责

`DocumentIndexingService`：文档索引流程入口。

`DocumentIndexingServiceImpl`：本轮主业务编排位置，负责状态校验、active chunk 查询、embedding 调用、VectorStore 写入、vectorId 写回和 document 状态流转。

`DocumentController`：只新增 HTTP 入口 `POST /api/documents/{id}/index`，不承载索引细节。

`DocumentChunkService`：提供 active chunk 查询和 `vectorId` 写回能力。

`EmbeddingService`：继续作为文本转向量入口。

`VectorStoreService`：继续作为向量库业务入口。

`QdrantVectorStoreService`：继续只负责 Qdrant 适配，本轮没有把 Qdrant 细节写进 document/chunk/controller。

## 5. 索引调用链

```text
POST /api/documents/{id}/index
  ↓
DocumentController
  ↓
DocumentIndexingService
  ↓
DocumentChunkService
  ↓
EmbeddingService
  ↓
VectorStoreService.ensureCollection
  ↓
VectorStoreService.upsert
  ↓
Qdrant collection
  ↓
document 状态变为 INDEXED
```

## 6. 状态流转

成功流转：

```text
CHUNKED
  ↓
EMBEDDING
  ↓
INDEXING
  ↓
INDEXED
```

失败流转：

```text
embedding 失败：FAILED + failedStage=EMBEDDING
VectorStore / Qdrant 写入失败：FAILED + failedStage=INDEXING
```

`INDEXED` 只会在全部 active chunks 成功生成 embedding 并成功写入 VectorStore 后设置。

## 7. vectorId 规则

本轮 `document_chunk.vector_id` 使用 chunk id 字符串：

```text
vectorId = String.valueOf(chunkId)
```

选择这个方案的原因：

- 简单稳定。
- 当前 `QdrantVectorStoreService` 使用 `chunkId` 作为 Qdrant point id。
- reprocess 会生成新的 chunk 记录，新 chunk id 天然区分新旧版本。

## 8. 事务和一致性说明

关系库是事实源，Qdrant 是检索索引。

本轮没有引入分布式事务。索引流程采用简单顺序策略：

```text
更新 document 状态为 EMBEDDING
为每个 active chunk 生成 embedding
更新 document 状态为 INDEXING
确保 Qdrant collection 存在
逐个 upsert chunk vector
每个 upsert 成功后写回 chunk.vectorId
全部成功后更新 document 状态为 INDEXED
失败时更新 document 状态为 FAILED
```

如果部分 chunk 已经写入 Qdrant 后失败，本轮暂不实现补偿删除。后续应补充重建索引或按 documentId 删除旧向量的补偿策略。

## 9. Embedding 配置说明

本轮默认使用 `MockEmbeddingClient`。

```text
APP_EMBEDDING_PROVIDER=mock
APP_EMBEDDING_DIMENSION=384
```

本轮不需要真实千问 API key，不读取 `DASHSCOPE_API_KEY`，也不实现 `QwenEmbeddingClient`。

如果本地 `.env` 或 IDEA Run Configuration 设置了 `APP_EMBEDDING_PROVIDER=qwen`，当前版本不会有可用的 `EmbeddingClient` Bean。真实千问 embedding 建议在 Phase 4.4 或单独一轮 `QwenEmbeddingClient` 中接入。

## 10. 如何运行和验证

自动化验证：

```bash
mvn test
git diff --check
```

测试不依赖 Docker，也不访问真实 Qdrant。

如果本地 Docker 可用，可以手动启动基础设施：

```bash
docker compose up -d
```

然后启动应用，按顺序验证：

```text
POST /api/knowledge-bases
POST /api/documents/upload
POST /api/documents/{id}/process
POST /api/documents/{id}/index
GET  /api/documents/{id}
GET  /api/documents/{id}/chunks
GET  /api/health/qdrant
```

索引成功后：

```text
document.status = INDEXED
document_chunk.vector_id 不为空
```

## 11. 本轮刻意不做

- 不接真实千问 embedding API。
- 不读取 `DASHSCOPE_API_KEY`。
- 不实现 `QwenEmbeddingClient`。
- 不实现向量检索 API。
- 不做 RAG 问答。
- 不做 LLM。
- 不做 SSE。
- 不做 reranker。
- 不做 Redis / Elasticsearch / RabbitMQ。
- 不做异步任务。
- 不做分布式事务。
- 不做 Qdrant 补偿删除。

## 12. 下一轮建议

进入 Phase 4.4：向量检索 API 与 `knowledgeBaseId` 过滤。

下一轮建议基于 `VectorStoreService.search()` 新增检索服务和 API，并明确 query embedding、knowledgeBaseId 过滤、topK、scoreThreshold 和返回 chunk 信息的边界。
