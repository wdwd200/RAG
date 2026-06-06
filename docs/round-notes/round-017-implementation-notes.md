# Round 017 Implementation Notes

## 1. 本轮目标

本轮执行 Phase 4.4：新增可选的千问 Embedding Client、向量检索服务和 HTTP API，并确保检索始终按 `knowledgeBaseId` 隔离。

本轮只实现“问题向量化 -> Qdrant 召回 -> 关系库 chunk 回查”，不生成 RAG 回答。

## 2. 本轮完成

- 新增 `QwenEmbeddingClient`，支持 DashScope OpenAI-compatible embeddings API。
- 支持单文本和 batch embedding。
- API key 缺失、HTTP 调用失败、响应无效和维度不一致时返回清晰业务错误。
- 新增 `RetrievalService`，集中编排检索主流程。
- 新增 `POST /api/retrieval/search`。
- `topK` 默认值为 5，允许范围为 1 到 20；超过上限返回参数校验错误。
- `VectorSearchRequest` 明确包含 `knowledgeBaseId`、`queryVector` 和 `topK`。
- Qdrant search 强制添加 `knowledgeBaseId` payload filter。
- 根据 Qdrant 返回的 `chunkId` 回查关系库，只返回 active chunk。
- Qdrant 中不存在对应关系库记录、记录已 inactive 或知识库不匹配时跳过。
- 更新千问配置模板、README 和自动化测试。

## 3. 主要文件

```text
.env.qwen.example
.gitignore
README.md
src/main/resources/application.yml
src/main/java/com/example/ragbackend/embedding/client/QwenEmbeddingClient.java
src/main/java/com/example/ragbackend/embedding/client/QwenEmbeddingRequestFactory.java
src/main/java/com/example/ragbackend/embedding/config/QwenEmbeddingProperties.java
src/main/java/com/example/ragbackend/retrieval/controller/RetrievalController.java
src/main/java/com/example/ragbackend/retrieval/dto/RetrieveRequest.java
src/main/java/com/example/ragbackend/retrieval/dto/RetrieveResponse.java
src/main/java/com/example/ragbackend/retrieval/dto/RetrievedChunk.java
src/main/java/com/example/ragbackend/retrieval/service/RetrievalService.java
src/main/java/com/example/ragbackend/retrieval/service/impl/RetrievalServiceImpl.java
src/main/java/com/example/ragbackend/vector/model/VectorSearchRequest.java
src/main/java/com/example/ragbackend/vector/service/impl/QdrantRequestFactory.java
src/main/java/com/example/ragbackend/vector/service/impl/QdrantVectorStoreService.java
```

## 4. 核心职责

`EmbeddingClient`：模型适配接口。

`QwenEmbeddingClient`：只负责千问 HTTP 请求、鉴权、响应解析和向量维度检查，只在 `app.embedding.provider=qwen` 时启用。

`EmbeddingService`：继续作为业务侧 embedding 统一入口。

`VectorStoreService`：继续作为业务侧向量库统一入口。

`RetrievalService`：检索主流程编排位置，负责问题向量化、向量检索、关系库回查和结果组装。

`RetrievalController`：只负责 HTTP 入参校验和统一 `ApiResponse` 输出。

`DocumentChunkService`：提供 active chunk 回查能力，保证关系库是内容事实源。

## 5. 检索调用链

```text
POST /api/retrieval/search
  -> RetrievalController
  -> RetrievalService
  -> EmbeddingService
  -> EmbeddingClient：mock 或 qwen
  -> VectorStoreService.search
  -> Qdrant knowledgeBaseId filter
  -> DocumentChunkService / Mapper 回查关系库
  -> 返回 RetrievedChunk
```

Qdrant payload 只用于定位候选 `chunkId` 和提供 score。响应中的 `documentId`、`knowledgeBaseId`、`chunkIndex` 和 `content` 均以关系库 `document_chunk` 为准。

## 6. knowledgeBaseId 过滤规则

`knowledgeBaseId` 是检索必填参数。

`QdrantRequestFactory.searchBody()` 始终生成以下 payload filter：

```text
knowledgeBaseId = 请求中的 knowledgeBaseId
```

`QdrantVectorStoreService` 拒绝没有 `knowledgeBaseId` 的检索请求。关系库回查后还会再次比较 chunk 的 `knowledgeBaseId`，避免错误索引数据进入最终结果。

## 7. topK 规则

- 请求未传 `topK`：使用 5。
- 最小值：1。
- 最大值：20。
- 小于 1 或大于 20：返回 `VALIDATION_ERROR`；服务层直接调用时返回 `RETRIEVAL_TOP_K_INVALID`。

## 8. 千问配置

默认测试和默认应用配置仍使用：

```text
APP_EMBEDDING_PROVIDER=mock
APP_EMBEDDING_DIMENSION=384
```

真实千问配置参考 `.env.qwen.example`：

```text
APP_EMBEDDING_PROVIDER=qwen
APP_EMBEDDING_DIMENSION=1024
QWEN_EMBEDDING_MODEL=text-embedding-v4
QWEN_EMBEDDING_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
DASHSCOPE_API_KEY=replace-with-your-api-key
```

真实 API key 只允许保存在本地 `.env`、环境变量或 IDEA Run Configuration 中，不提交到 Git。仓库只保留占位符模板。

## 9. Qdrant 维度注意事项

Mock embedding 默认是 384 维，千问 `text-embedding-v4` 建议使用 1024 维。`APP_EMBEDDING_DIMENSION` 必须和 Qdrant collection vector size 一致。

如果 collection 已按 384 维创建，切换到 1024 维时需要删除旧 collection 或改用新的 `QDRANT_COLLECTION_NAME`。

## 10. 测试覆盖

- Qwen API key 缺失时返回清晰错误。
- Qwen 请求包含 `model`、`input`、`dimensions` 和 `encoding_format`。
- batch 响应按 index 还原输入顺序。
- RetrievalService 调用 EmbeddingService。
- RetrievalService 将 `knowledgeBaseId` 和默认 `topK` 传给 VectorStoreService。
- 检索结果按 `chunkId` 回查关系库。
- inactive、不存在和跨知识库 chunk 不进入响应。
- Controller 校验空 question、空 knowledgeBaseId 和超限 topK。
- Qdrant search body 包含 `knowledgeBaseId` filter。

自动化测试默认不访问真实 DashScope 或 Qdrant。

## 11. 运行与验证

自动化验证：

```bash
mvn test
git diff --check
```

本地 Docker 和千问 API key 可用时，可按以下顺序验证真实链路：

```text
POST /api/knowledge-bases
POST /api/documents/upload
POST /api/documents/{id}/process
POST /api/documents/{id}/index
POST /api/retrieval/search
```

## 12. 本轮刻意不做

- 不实现 RAG 回答。
- 不实现 PromptBuilder。
- 不接 LLM。
- 不做 SSE。
- 不做 reranker。
- 不引入 Redis、Elasticsearch 或 RabbitMQ。
- 不做异步任务和权限系统。

## 13. 下一轮建议

进入 Phase 4.5：完成 Phase 4 收尾，在本地基础设施可用时验证真实千问 embedding + Qdrant 检索链路，并整理阶段导读。
