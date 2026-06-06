# Phase 004 Summary

## 1. Phase 4 完成了什么

Phase 4 完成了从关系库 chunk 到向量索引和向量检索的主链路。

当前系统已经具备：

- embedding 配置与客户端抽象。
- 可重复、无需外部服务的 Mock Embedding。
- DashScope OpenAI-compatible 千问 Embedding Client。
- Qdrant collection 初始化、向量写入、检索和按文档删除能力。
- 文档索引服务与 `POST /api/documents/{id}/index`。
- 文档状态从 `CHUNKED` 到 `INDEXED` 的流转。
- `document_chunk.vector_id` 写回。
- 检索服务与 `POST /api/retrieval/search`。
- Qdrant `knowledgeBaseId` payload filter。
- 检索结果回查关系库 active chunk。
- Mock 和真实千问两条链路的手动验证。

## 2. 当前目录结构

Embedding：

```text
src/main/java/com/example/ragbackend/embedding
├── client
│   ├── EmbeddingClient.java
│   ├── MockEmbeddingClient.java
│   ├── QwenEmbeddingClient.java
│   └── QwenEmbeddingRequestFactory.java
├── config
│   ├── EmbeddingProperties.java
│   └── QwenEmbeddingProperties.java
└── service
    ├── EmbeddingService.java
    └── impl/EmbeddingServiceImpl.java
```

Vector：

```text
src/main/java/com/example/ragbackend/vector
├── config/QdrantProperties.java
├── model
│   ├── ChunkVector.java
│   ├── VectorSearchRequest.java
│   └── VectorSearchResult.java
└── service
    ├── VectorStoreService.java
    ├── QdrantHealthService.java
    └── impl
        ├── QdrantRequestFactory.java
        └── QdrantVectorStoreService.java
```

Retrieval：

```text
src/main/java/com/example/ragbackend/retrieval
├── controller/RetrievalController.java
├── dto
│   ├── RetrieveRequest.java
│   ├── RetrieveResponse.java
│   └── RetrievedChunk.java
└── service
    ├── RetrievalService.java
    └── impl/RetrievalServiceImpl.java
```

文档索引入口：

```text
src/main/java/com/example/ragbackend/document/service
├── DocumentIndexingService.java
└── impl/DocumentIndexingServiceImpl.java
```

## 3. Embedding 抽象设计

`EmbeddingClient` 表达“文本转换为固定维度向量”的模型适配能力。

`EmbeddingService` 是业务统一入口，负责：

- 空文本校验。
- 单文本和 batch 调用。
- 返回向量维度校验。

模型 HTTP 细节不会进入 `EmbeddingService`。业务服务也不直接依赖 Mock 或千问实现，而是依赖 `EmbeddingService`。

## 4. Mock 与 Qwen 的区别

`MockEmbeddingClient`：

- 在 `app.embedding.provider=mock` 时启用。
- 默认生成 384 维向量。
- 基于文本 hash 确定性生成结果。
- 不访问网络、不需要 API key。
- 用于自动化测试和本地流程验证。

`QwenEmbeddingClient`：

- 在 `app.embedding.provider=qwen` 时启用。
- 通过 DashScope OpenAI-compatible `/embeddings` API 调用真实模型。
- 支持单文本和 batch 请求。
- 支持 `text-embedding-v4` 和可配置维度。
- 校验 API key、HTTP 结果、响应结构和向量维度。

测试环境始终使用 Mock，不依赖真实 DashScope。

## 5. Qdrant collection 与维度规则

`APP_EMBEDDING_DIMENSION` 同时决定 embedding 维度和 Qdrant collection vector size。

当前推荐：

```text
Mock：384 维，collection = rag_chunks_mock_384
Qwen：1024 维，collection = rag_chunks_qwen_1024
```

已按 384 维创建的 collection 不能直接写入 1024 维向量。切换 provider 或维度时必须删除旧 collection，或者使用新的 `QDRANT_COLLECTION_NAME`。

## 6. 文档索引链路

```text
POST /api/documents/{id}/index
  ↓
DocumentIndexingService
  ↓
DocumentChunkService
  ↓
EmbeddingService
  ↓
EmbeddingClient
  ↓
VectorStoreService
  ↓
Qdrant
  ↓
chunk.vectorId 写回关系库
  ↓
document 状态变为 INDEXED
```

索引只允许从 `CHUNKED` 状态开始：

```text
CHUNKED
  ↓
EMBEDDING
  ↓
INDEXING
  ↓
INDEXED
```

Embedding 失败记录 `FAILED + failedStage=EMBEDDING`，向量库写入失败记录 `FAILED + failedStage=INDEXING`。

## 7. 向量检索链路

```text
POST /api/retrieval/search
  ↓
RetrievalController
  ↓
RetrievalService
  ↓
EmbeddingService
  ↓
VectorStoreService.search
  ↓
Qdrant knowledgeBaseId filter
  ↓
DocumentChunkService 回查 active chunk
  ↓
返回 RetrievedChunk
```

`topK` 默认值为 5，允许范围为 1 到 20。

## 8. knowledgeBaseId 过滤规则

`knowledgeBaseId` 是检索必填参数。

Qdrant search 始终使用：

```text
knowledgeBaseId = 请求中的 knowledgeBaseId
```

`QdrantVectorStoreService` 拒绝没有 `knowledgeBaseId` 的检索请求。关系库回查后，`RetrievalService` 还会再次比较 chunk 的 `knowledgeBaseId`，防止错误索引数据进入响应。

Mock 和 Qwen 手动验证中，目标知识库均能返回结果，隔离知识库均返回 0 条。

## 9. 事实源规则

关系库中的 `document_chunk` 是内容事实源，Qdrant 是检索索引。

Qdrant payload 只用于召回候选 `chunkId` 和 score。最终响应中的 `documentId`、`knowledgeBaseId`、`chunkIndex` 和 `content` 来自关系库。

以下结果会被跳过：

- Qdrant 中存在但关系库中不存在的 chunk。
- 已经 inactive 的 chunk。
- 不属于请求知识库的 chunk。

## 10. 使用 Mock 验证

配置：

```text
APP_EMBEDDING_PROVIDER=mock
APP_EMBEDDING_DIMENSION=384
QDRANT_COLLECTION_NAME=rag_chunks_mock_384
```

启动并验证：

```bash
docker compose up -d
mvn clean package
java -jar target/rag-backend-0.0.1-SNAPSHOT.jar
```

接口顺序：

```text
POST /api/knowledge-bases
POST /api/documents/upload
POST /api/documents/{id}/process
POST /api/documents/{id}/index
POST /api/retrieval/search
GET  /api/documents/{id}
GET  /api/documents/{id}/chunks
GET  /api/health/qdrant
```

Phase 4.5 实测结果：

- 文档状态：`UPLOADED -> CHUNKED -> INDEXED`。
- 7 个 chunk 全部写回 vectorId。
- 目标知识库返回 5 条结果。
- 隔离知识库返回 0 条结果。
- Qdrant 状态为 `UP`，collection 为 `rag_chunks_mock_384`。

## 11. 使用千问 API key 验证

真实配置只放在本地 `.env`、环境变量或 IDEA Run Configuration：

```text
APP_EMBEDDING_PROVIDER=qwen
APP_EMBEDDING_DIMENSION=1024
QWEN_EMBEDDING_MODEL=text-embedding-v4
QWEN_EMBEDDING_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
DASHSCOPE_API_KEY=本地真实值
QDRANT_COLLECTION_NAME=rag_chunks_qwen_1024
```

验证接口顺序与 Mock 相同。

Phase 4.5 实测结果：

- `text-embedding-v4` 调用成功。
- 文档状态：`UPLOADED -> CHUNKED -> INDEXED`。
- 3 个 chunk 全部写入 1024 维 Qdrant collection 并写回 vectorId。
- 真实语义检索返回 3 条结果。
- 隔离知识库返回 0 条结果。
- collection 为 `rag_chunks_qwen_1024`，实际 vector size 为 1024，distance 为 Cosine。

真实 API key 没有写入代码或 Git。

## 12. 当前还没有做什么

- 没有 RAG 最终回答。
- 没有 PromptBuilder。
- 没有 LLM Client。
- 没有 SSE 流式输出。
- 没有 reranker。
- 没有 Redis、Elasticsearch 或 RabbitMQ。
- 没有异步索引任务。
- 没有权限系统。

当前 API 只返回相关 chunk，不生成回答。

## 13. 下一阶段

Phase 5 将实现 RAG 问答闭环。

下一轮 Phase 5.1 建议先完成：

- `LlmClient` 抽象。
- Mock LLM。
- `PromptBuilder`。
- 为后续 retrieval + prompt + LLM 编排建立清晰边界。

Phase 5.1 仍应保持默认测试不依赖真实模型 API。
