# Round 015 Implementation Notes

## 1. 本轮是什么

本轮执行 Phase 4.2：Qdrant collection 初始化与 VectorStoreService 抽象。

本轮目标是建立向量库访问层，让后续业务可以通过统一接口创建 / 检查 Qdrant collection、写入 chunk vector、执行向量搜索，以及按 documentId 删除向量。

## 2. 本轮完成了什么

- 新增 Qdrant 配置 `app.qdrant.*`。
- 新增 `.env.qwen.example`，作为后续启用千问 embedding 的配置模板。
- 新增 `QdrantProperties`。
- 新增 `VectorStoreService` 抽象。
- 新增向量层模型：`ChunkVector`、`VectorSearchRequest`、`VectorSearchResult`。
- 新增 `QdrantVectorStoreService`，使用 Spring `RestClient` 适配 Qdrant HTTP API。
- 新增 `QdrantRequestFactory`，集中构造 Qdrant HTTP 请求体，便于单元测试。
- 新增 `QdrantHealthService` 和 `GET /api/health/qdrant`。
- 新增 Qdrant 配置、向量模型、请求构造、service 校验和健康检查相关测试。
- 更新 `.env.example`、`.gitignore`、`application.yml`、`application-test.yml` 和 `README.md`。

## 3. 新增或修改的文件

```text
.env.example
.env.qwen.example
.gitignore
README.md
docs/round-notes/round-015-implementation-notes.md
src/main/resources/application.yml
src/test/resources/application-test.yml
src/main/java/com/example/ragbackend/RagBackendApplication.java
src/main/java/com/example/ragbackend/health/QdrantHealthController.java
src/main/java/com/example/ragbackend/vector/config/QdrantProperties.java
src/main/java/com/example/ragbackend/vector/model/ChunkVector.java
src/main/java/com/example/ragbackend/vector/model/QdrantHealthResponse.java
src/main/java/com/example/ragbackend/vector/model/VectorSearchRequest.java
src/main/java/com/example/ragbackend/vector/model/VectorSearchResult.java
src/main/java/com/example/ragbackend/vector/service/QdrantHealthService.java
src/main/java/com/example/ragbackend/vector/service/VectorStoreService.java
src/main/java/com/example/ragbackend/vector/service/impl/QdrantRequestFactory.java
src/main/java/com/example/ragbackend/vector/service/impl/QdrantVectorStoreService.java
src/test/java/com/example/ragbackend/vector/config/QdrantPropertiesTest.java
src/test/java/com/example/ragbackend/vector/model/VectorStoreModelTest.java
src/test/java/com/example/ragbackend/vector/service/QdrantHealthServiceTest.java
src/test/java/com/example/ragbackend/vector/service/impl/QdrantRequestFactoryTest.java
src/test/java/com/example/ragbackend/vector/service/impl/QdrantVectorStoreServiceTest.java
```

## 4. 核心文件职责

`QdrantProperties`：读取 Qdrant host、httpPort、collectionName、vectorSize 和 distance。

`VectorStoreService`：业务侧向量库入口，后续 document/chunk 业务只应依赖这个接口。

`QdrantVectorStoreService`：Qdrant HTTP API 适配实现，负责 collection 初始化、upsert、search 和 delete。

`QdrantRequestFactory`：集中构造 Qdrant 请求体，避免请求结构散落在 service 逻辑中。

`ChunkVector`：表达一条 chunk 的向量及必要 payload 元数据。

`VectorSearchRequest`：表达一次向量搜索请求。

`VectorSearchResult`：表达 Qdrant 搜索结果中的 chunk 元数据和 score。

`QdrantHealthService`：调用 `VectorStoreService.ensureCollection()` 做 Qdrant collection 初始化 / 连通性检查。

`QdrantHealthController`：提供 `GET /api/health/qdrant` 验证入口。

## 5. VectorStore 调用链

```text
未来业务服务
  ↓
VectorStoreService
  ↓
QdrantVectorStoreService
  ↓
Qdrant collection rag_chunks
```

当前 document processing 尚未接入这条链路。本轮只准备向量库访问层。

## 6. Qdrant HTTP 操作

`ensureCollection()`：

```text
GET /collections/{collectionName}
404 时 PUT /collections/{collectionName}
```

`upsert(ChunkVector)`：

```text
PUT /collections/{collectionName}/points?wait=true
```

`search(VectorSearchRequest)`：

```text
POST /collections/{collectionName}/points/search
```

`deleteByDocumentId(Long documentId)`：

```text
POST /collections/{collectionName}/points/delete?wait=true
```

payload 至少包含：

```text
chunkId
documentId
knowledgeBaseId
chunkIndex
contentHash
processingVersion
```

## 7. 新增配置项

```yaml
app:
  qdrant:
    host: ${QDRANT_HOST:localhost}
    http-port: ${QDRANT_HTTP_PORT:6333}
    collection-name: ${QDRANT_COLLECTION_NAME:rag_chunks}
    vector-size: ${APP_EMBEDDING_DIMENSION:384}
    distance: ${QDRANT_DISTANCE:COSINE}
```

`.env.example` 新增：

```text
QDRANT_COLLECTION_NAME=rag_chunks
QDRANT_DISTANCE=COSINE
```

`.env.qwen.example` 只是模板，本轮不会读取，也不会提交真实 API key。

## 8. Qwen 配置说明

本轮不需要真实千问 / DashScope API key。

后续需要填写 API key 的阶段：

```text
Phase 4.3：如果要用真实 embedding 做 chunk 向量入库，可以启用
Phase 4.4：如果要验证真实语义检索，建议启用
```

模板文件：

```text
.env.qwen.example
```

真实 `DASHSCOPE_API_KEY` 不要提交到 Git。

## 9. 为什么采用当前设计

本轮继续保持分层清晰：业务侧只依赖 `VectorStoreService`，Qdrant 的 URL、请求结构和响应解析只留在 `QdrantVectorStoreService` 及 `QdrantRequestFactory` 中。

使用 Spring `RestClient` 是为了避免新增 Qdrant Java client 依赖，降低依赖风险，同时保持 HTTP 请求结构透明、易测。

`QdrantRequestFactory` 单独存在，是为了让 collection、upsert、search、delete 的请求体可以不依赖真实 Qdrant 进行单元测试。

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

启动应用后验证 Qdrant：

```bash
curl http://localhost:8080/api/health/qdrant
```

该接口会调用 `VectorStoreService.ensureCollection()`，如果 `rag_chunks` 不存在会尝试创建。

## 11. 本轮刻意不做

- 不接真实千问 embedding API。
- 不读取 `DASHSCOPE_API_KEY`。
- 不实现 `QwenEmbeddingClient`。
- 不做 chunk embedding 批量任务。
- 不把 chunk 写入 Qdrant 处理链路。
- 不修改 document 状态为 `INDEXED`。
- 不实现向量检索 API。
- 不做 LLM。
- 不做 SSE。
- 不做 reranker。
- 不做 Redis / Elasticsearch / RabbitMQ。

## 12. 下一轮建议

进入 Phase 4.3：chunk embedding、向量入库与 document 状态到 `INDEXED`。

下一轮建议把 `EmbeddingService` 和 `VectorStoreService` 接入文档处理链路，并明确失败回滚、重复处理和旧向量删除策略。
