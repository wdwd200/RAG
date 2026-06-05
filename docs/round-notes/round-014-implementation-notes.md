# Round 014 Implementation Notes

## 1. 本轮是什么

本轮执行 Phase 4.1：EmbeddingClient 抽象、Mock Embedding 与向量维度配置。

本轮目标是先建立 embedding 层的稳定边界，让后续 Qdrant 入库和检索可以依赖统一接口，而不是直接依赖某个真实模型 SDK 或 HTTP Client。

## 2. 本轮完成了什么

- 新增 `app.embedding.provider` 配置，默认值为 `mock`。
- 新增 `app.embedding.dimension` 配置，默认值为 `384`。
- 新增 `EmbeddingProperties`，集中承载 embedding 配置。
- 新增 `EmbeddingClient` 抽象，只表达文本到向量。
- 新增 `MockEmbeddingClient`，在 mock provider 下生成确定性固定维度向量。
- 新增 `EmbeddingService` 和 `EmbeddingServiceImpl`，作为业务侧 embedding 调用入口。
- 新增 embedding 测试，覆盖维度、稳定性、差异性、空文本、batch 和 service 维度校验。
- 更新 `.env.example`、`application.yml`、`application-test.yml` 和 `README.md`。

## 3. 新增或修改的文件

```text
src/main/java/com/example/ragbackend/RagBackendApplication.java
src/main/java/com/example/ragbackend/embedding/config/EmbeddingProperties.java
src/main/java/com/example/ragbackend/embedding/client/EmbeddingClient.java
src/main/java/com/example/ragbackend/embedding/client/MockEmbeddingClient.java
src/main/java/com/example/ragbackend/embedding/service/EmbeddingService.java
src/main/java/com/example/ragbackend/embedding/service/impl/EmbeddingServiceImpl.java
src/test/java/com/example/ragbackend/embedding/EmbeddingServiceTest.java
src/main/resources/application.yml
src/test/resources/application-test.yml
.env.example
README.md
docs/round-notes/round-014-implementation-notes.md
```

## 4. 核心文件职责

`EmbeddingProperties`：读取 `app.embedding` 下的 provider 和 dimension 配置。

`EmbeddingClient`：模型适配层接口，只定义单文本 embedding 和 batch embedding。

`MockEmbeddingClient`：mock provider 的实现，基于文本和下标的 SHA-256 hash 生成确定性伪向量。

`EmbeddingService`：业务侧入口，后续业务模块应依赖它，而不是直接依赖 `MockEmbeddingClient`。

`EmbeddingServiceImpl`：校验文本、调用 `EmbeddingClient`、校验返回向量维度。

`EmbeddingServiceTest`：验证 mock 向量维度、稳定性、batch 数量和 service 维度校验。

## 5. 本轮功能入口在哪里

当前没有新增 HTTP 接口。

本轮 Java 侧入口是：

```text
EmbeddingService.embed(String text)
EmbeddingService.embedBatch(List<String> texts)
```

后续 document chunk 入向量库时，应从业务编排层调用 `EmbeddingService`，不要直接调用 `MockEmbeddingClient`。

## 6. Embedding 调用链

```text
EmbeddingService
  ↓
EmbeddingClient
  ↓
MockEmbeddingClient
  ↓
生成固定维度向量
```

当前调用链只到 mock 向量生成，不写入 Qdrant，不更新 document 状态。

## 7. 新增配置项

生产配置默认值：

```yaml
app:
  embedding:
    provider: ${APP_EMBEDDING_PROVIDER:mock}
    dimension: ${APP_EMBEDDING_DIMENSION:384}
```

测试配置使用较小维度，便于断言：

```yaml
app:
  embedding:
    provider: mock
    dimension: 8
```

`.env.example` 新增：

```text
APP_EMBEDDING_PROVIDER=mock
APP_EMBEDDING_DIMENSION=384
```

## 8. 为什么采用当前设计

本轮先抽象 `EmbeddingClient`，是为了把“模型调用”从业务流程中隔离出来。后续接 OpenAI-compatible、Qwen、DeepSeek 或本地模型时，只需要新增 client 实现，不需要改动 document、chunk 或 controller。

`EmbeddingService` 单独存在，是为了给业务侧一个稳定入口。它负责输入校验和向量维度校验，避免后续多个业务模块重复写校验逻辑。

Mock 实现使用 deterministic pseudo vector，是为了让测试和本地流程稳定可复现。它不追求语义效果，只保证维度正确、同文本结果一致、不同文本可以产生不同结果。

## 9. 如何运行和验证

本轮已执行：

```bash
mvn test
git diff --check
```

在当前机器上实际使用的是 F 盘 Maven 工具和 F 盘本地仓库：

```text
F:\RAG缓存专用目录\tools\apache-maven-3.9.16\bin\mvn.cmd
F:\RAG缓存专用目录\maven-repository
```

测试结果：

```text
Tests run: 53, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

`git diff --check` 通过。

## 10. 哪些细节暂时不用深究

- Mock 向量没有真实语义，只用于流程和测试。
- 当前没有真实 embedding API key，也没有真实模型 HTTP 调用。
- 当前没有 Qdrant collection 初始化。
- 当前没有向量入库、向量检索或相似度计算。
- 当前没有把 document 状态从 `CHUNKED` 推进到 `INDEXED`。
- 当前没有新增 embedding Controller。

## 11. 本轮刻意不做

- 不接真实 embedding API。
- 不写 OpenAI-compatible HTTP Client。
- 不连接 Qdrant。
- 不创建 Qdrant collection。
- 不写向量入库。
- 不写向量检索。
- 不修改 document 状态为 `INDEXED`。
- 不做 LLM。
- 不做 SSE。
- 不做 Redis / Elasticsearch / RabbitMQ。
- 不做 Spring Security / JWT。

## 12. 下一轮建议

进入 Phase 4.2：Qdrant collection 初始化与 VectorStoreService 抽象。

下一轮建议先定义向量库边界，再考虑 collection 初始化和后续 chunk 向量入库，避免把 Qdrant 细节散落进 document 或 chunk 业务代码。
