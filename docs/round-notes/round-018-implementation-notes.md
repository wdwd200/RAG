# Round 018 Implementation Notes

## 1. 本轮定位

本轮执行 Phase 4.5，是 Phase 4 的收尾轮次。

本轮没有新增大功能，主要完成：

- Mock Embedding + Qdrant 完整链路验证。
- Qwen Embedding + Qdrant 真实链路验证。
- 核心类逻辑可读性检查。
- Phase 4 总结文档。
- README 阶段状态和验证说明更新。

## 2. Mock 链路验证

验证配置：

```text
APP_EMBEDDING_PROVIDER=mock
APP_EMBEDDING_DIMENSION=384
QDRANT_COLLECTION_NAME=rag_chunks_mock_384
```

验证命令：

```bash
docker compose up -d
mvn clean package
java -jar target/rag-backend-0.0.1-SNAPSHOT.jar
```

验证接口：

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

验证结果：

- 知识库创建成功。
- 文档上传后状态为 `UPLOADED`。
- process 后状态为 `CHUNKED`。
- index 后状态为 `INDEXED`。
- 7 个 active chunk 全部写回 `vectorId`。
- 目标知识库检索返回 5 条结果。
- 所有结果的 `knowledgeBaseId` 均与请求一致。
- 隔离知识库检索返回 0 条结果。
- Qdrant 健康检查返回 `UP`。
- collection 为 `rag_chunks_mock_384`，vector size 为 384。

## 3. Qwen 真实链路验证

本地已提供 `DASHSCOPE_API_KEY`，因此本轮完成了真实千问验证。

验证配置：

```text
APP_EMBEDDING_PROVIDER=qwen
APP_EMBEDDING_DIMENSION=1024
QWEN_EMBEDDING_MODEL=text-embedding-v4
QWEN_EMBEDDING_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
QDRANT_COLLECTION_NAME=rag_chunks_qwen_1024
```

真实 key 从本地 `.env` 读取，未写入任何仓库文件。

验证流程：

```text
上传 Markdown 文档
  ↓
process
  ↓
3 个 active chunk
  ↓
index
  ↓
调用 text-embedding-v4
  ↓
写入 rag_chunks_qwen_1024
  ↓
retrieval/search
```

验证结果：

- 文档状态成功从 `UPLOADED` 流转到 `CHUNKED`，再流转到 `INDEXED`。
- 3 个 active chunk 全部生成真实千问 embedding。
- 3 个 chunk 全部写入 Qdrant 并写回 `vectorId`。
- 检索返回 3 条结果。
- 首条结果属于目标知识库。
- 隔离知识库返回 0 条结果。
- collection 实际 points count 为 3。
- collection 实际 vector size 为 1024。
- distance 为 Cosine。

## 4. 索引链路

```text
POST /api/documents/{id}/index
  ↓
DocumentIndexingService
  ↓
DocumentChunkService
  ↓
EmbeddingService
  ↓
EmbeddingClient：mock 或 qwen
  ↓
VectorStoreService
  ↓
Qdrant
  ↓
chunk.vectorId 写回
  ↓
document.status = INDEXED
```

`DocumentIndexingServiceImpl` 仍是索引主流程编排位置。Controller、Embedding Client 和 Qdrant 适配层均未承载文档状态流转。

## 5. 检索链路

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

Qdrant payload 不作为最终内容来源。最终 `content`、`documentId`、`knowledgeBaseId` 和 `chunkIndex` 均来自关系库 `document_chunk`。

## 6. 逻辑可读性检查

本轮检查：

```text
DocumentIndexingServiceImpl
RetrievalServiceImpl
EmbeddingServiceImpl
QwenEmbeddingClient
QdrantVectorStoreService
DocumentChunkServiceImpl
```

检查结论：

- `DocumentIndexingServiceImpl` 只负责编排索引主流程和状态。
- `RetrievalServiceImpl` 只负责编排 query embedding、向量检索和关系库回查。
- `EmbeddingServiceImpl` 仍是 embedding 业务入口。
- `QwenEmbeddingClient` 只负责千问 HTTP 适配。
- `QdrantVectorStoreService` 只负责 Qdrant HTTP 适配。
- `DocumentChunkServiceImpl` 继续提供关系库 chunk 事实源访问。
- 没有使用 Qdrant payload content 替代关系库 content。

现有方法和职责已经清楚，本轮没有为了格式或风格做无关重构。

## 7. 新增或修改文件

```text
README.md
docs/phase-notes/phase-004-summary.md
docs/round-notes/round-018-implementation-notes.md
```

本轮没有修改业务代码、数据库 migration 或测试逻辑。

## 8. 安全与配置

- `.env` 继续被 `.gitignore` 忽略。
- 真实 `DASHSCOPE_API_KEY` 未提交。
- README 和文档只保留配置项名称和占位说明。
- Mock 384 维与 Qwen 1024 维使用不同 collection。
- 自动化测试继续使用 Mock，不依赖 DashScope 或 Qdrant。

## 9. 本轮刻意不做

- 不实现 RAG 回答。
- 不实现 PromptBuilder。
- 不接 LLM。
- 不做 SSE。
- 不做 reranker。
- 不引入 Redis、Elasticsearch 或 RabbitMQ。
- 不做异步任务和权限系统。

## 10. 下一轮建议

进入 Phase 5.1：`LlmClient` 抽象、Mock LLM 与 `PromptBuilder`。
