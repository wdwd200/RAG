# RAG 后端知识库

RAG 后端知识库是一个面向知识库管理、文档上传、文档解析、chunk 入库和后续检索增强生成能力的 Spring Boot 后端项目。

当前阶段：Phase 5 已完成。项目已支持文档处理、向量检索、一次性与 SSE RAG 问答、requestId 审计追踪、LLM 调用日志，以及可选的千问真实 embedding 和 LLM；默认仍使用 Mock Embedding 与 Mock LLM。

## 技术栈

- Java 17
- Spring Boot 3.x
- Maven
- Spring Web
- Spring JDBC
- Spring Validation
- Spring Boot Actuator
- Springdoc OpenAPI / Swagger UI
- Flyway
- MyBatis-Plus
- PostgreSQL
- Qdrant
- H2 Test Database
- Lombok
- JUnit 5 / Spring Boot Test

## 本地基础设施

本地开发使用 Docker Compose 启动 PostgreSQL 和 Qdrant：

```bash
docker compose up -d
```

查看容器状态：

```bash
docker compose ps
```

默认数据库配置来自 `.env.example`。示例账号密码只用于本地开发，不用于生产环境。实际本地运行时可以复制为 `.env` 后按需调整，`.env` 已被 `.gitignore` 忽略。

## 本地启动应用

确认本地已安装 Java 17 和 Maven，并且 PostgreSQL 容器已启动。

运行测试：

```bash
mvn test
```

推荐启动应用：

```bash
mvn clean package
java -jar target/rag-backend-0.0.1-SNAPSHOT.jar
```

如果确认当前路径不会触发 classpath 编码问题，也可以使用：

```bash
mvn spring-boot:run
```

默认服务端口为 `8080`，可通过环境变量 `SERVER_PORT` 或 `src/main/resources/application.yml` 中的 `server.port` 调整。

默认本地文件存储路径为 `storage/documents`，可通过环境变量 `APP_STORAGE_LOCAL_ROOT` 覆盖。`storage/` 已被 `.gitignore` 忽略，不应提交本地上传文件。

默认允许上传的文件后缀为 `txt,md,pdf,docx`，可通过 `APP_STORAGE_ALLOWED_EXTENSIONS` 覆盖。默认单文件最大大小为 10MB，可通过 `APP_STORAGE_MAX_FILE_SIZE_BYTES` 覆盖。

默认 chunk 切分配置：

```text
APP_CHUNK_SIZE=800
APP_CHUNK_OVERLAP=100
```

`APP_CHUNK_OVERLAP` 必须小于 `APP_CHUNK_SIZE`。

默认 embedding 配置：

```text
APP_EMBEDDING_PROVIDER=mock
APP_EMBEDDING_DIMENSION=384
```

测试和默认本地配置使用 Mock Embedding。Mock 向量由文本 hash 确定性生成，同一段文本多次生成结果稳定一致。将 `APP_EMBEDDING_PROVIDER` 设置为 `qwen` 后，`QwenEmbeddingClient` 会通过 DashScope OpenAI-compatible embeddings API 调用真实千问 embedding。

默认 Qdrant 配置：

```text
QDRANT_HOST=localhost
QDRANT_HTTP_PORT=6333
QDRANT_COLLECTION_NAME=rag_chunks
QDRANT_DISTANCE=COSINE
```

`app.qdrant.vector-size` 默认跟 `APP_EMBEDDING_DIMENSION` 保持一致。Mock embedding 默认是 384 维，千问 `text-embedding-v4` 建议使用 1024 维。如果 Qdrant collection 已按 384 维创建，切换到 1024 维前必须删除旧 collection 或使用新的 `QDRANT_COLLECTION_NAME`，否则索引和检索会因向量维度不一致失败。

`.env.qwen.example` 是启用千问 embedding 和千问 LLM 的配置模板。真实 `DASHSCOPE_API_KEY` 只允许填写在本地 `.env`、环境变量或 IDEA Run Configuration 中，不得提交到 Git。千问 embedding 配置：

```text
APP_EMBEDDING_PROVIDER=qwen
APP_EMBEDDING_DIMENSION=1024
QWEN_EMBEDDING_MODEL=text-embedding-v4
QWEN_EMBEDDING_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
DASHSCOPE_API_KEY=replace-with-your-api-key
```

默认 LLM 配置：

```text
APP_LLM_PROVIDER=mock
APP_LLM_MODEL=mock-rag-assistant
APP_LLM_TEMPERATURE=0.2
APP_LLM_MAX_TOKENS=1000
```

默认 provider 是 `mock`，不需要 API key，`/api/chat/once` 使用 `MockLlmClient` 返回稳定测试答案。

本地切换到千问 LLM：

```text
APP_LLM_PROVIDER=qwen
APP_LLM_MODEL=qwen-plus
QWEN_LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
APP_LLM_TEMPERATURE=0.2
APP_LLM_MAX_TOKENS=1000
DASHSCOPE_API_KEY=replace-with-your-api-key
```

设置 provider 为 `qwen` 后，`QwenLlmClient` 会调用 DashScope OpenAI-compatible Chat Completions API。`DASHSCOPE_API_KEY` 可以同时用于千问 embedding 和千问 LLM；真实 key 不得提交。LLM provider 与 embedding provider 可以独立选择。

## 健康检查

应用健康检查：

```bash
curl http://localhost:8080/api/health
```

数据库连通性检查：

```bash
curl http://localhost:8080/api/health/database
```

Qdrant collection 初始化 / 连通性检查：

```bash
curl http://localhost:8080/api/health/qdrant
```

该接口会调用 `VectorStoreService.ensureCollection()`，如果 collection 不存在会尝试创建。Qdrant 未启动时返回的 `data.status` 会是 `DOWN`。

Actuator 健康检查：

```bash
curl http://localhost:8080/actuator/health
```

## Swagger

启动项目后访问：

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

当前 Swagger 页面能看到健康检查、Qdrant 健康检查、知识库 CRUD、文档、chunk、向量检索、一次性 / SSE RAG 问答，以及检索日志和 LLM 调用日志查询接口。

## 知识库 CRUD API

创建知识库：

```bash
curl -X POST http://localhost:8080/api/knowledge-bases \
  -H "Content-Type: application/json" \
  -d '{"name":"默认知识库","description":"用于本地验证","ownerId":1,"visibility":"PRIVATE"}'
```

查询知识库列表：

```bash
curl http://localhost:8080/api/knowledge-bases
```

查询单个知识库：

```bash
curl http://localhost:8080/api/knowledge-bases/1
```

更新知识库：

```bash
curl -X PUT http://localhost:8080/api/knowledge-bases/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"更新后的知识库","description":"已更新","visibility":"PUBLIC"}'
```

删除知识库：

```bash
curl -X DELETE http://localhost:8080/api/knowledge-bases/1
```

## 文档 API

创建文档元数据：

```bash
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{"knowledgeBaseId":1,"fileName":"demo.txt","fileType":"txt","fileSize":1024,"storagePath":"documents/demo.txt","createdBy":1}'
```

上传文件并创建文档记录：

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "knowledgeBaseId=1" \
  -F "createdBy=1" \
  -F "file=@/path/to/demo.txt"
```

处理已上传文档并生成 chunk：

```bash
curl -X POST http://localhost:8080/api/documents/1/process
```

处理成功后，返回示例：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "documentId": 1,
    "status": "CHUNKED",
    "chunkCount": 3,
    "processingVersion": 1
  }
}
```

将已生成 chunk 的文档写入向量库：

```bash
curl -X POST http://localhost:8080/api/documents/1/index
```

索引成功后，返回示例：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "documentId": 1,
    "status": "INDEXED",
    "chunkCount": 3,
    "processingVersion": 1
  }
}
```

查询单个文档元数据：

```bash
curl http://localhost:8080/api/documents/1
```

查询某个知识库下的文档元数据列表：

```bash
curl http://localhost:8080/api/knowledge-bases/1/documents
```

删除文档元数据：

```bash
curl -X DELETE http://localhost:8080/api/documents/1
```

## Chunk API

查询某个 document 下当前 active chunk：

```bash
curl http://localhost:8080/api/documents/1/chunks
```

查询单个 active chunk：

```bash
curl http://localhost:8080/api/chunks/1
```

chunk 列表按 `chunkIndex` 升序返回。不存在或非 active 的 chunk 会返回 `DOCUMENT_CHUNK_NOT_FOUND`。

## 文档处理调用链

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

当前 parser 只支持 txt/md，按 UTF-8 普通文本读取。Markdown 暂不做 AST 解析。

当前 splitter 使用固定窗口切分，支持 `chunkSize` 和 `overlap`。`tokenCount` 使用简单估算，不做精准 token 计算。

重复处理同一文档时，当前实现会先把该 document 下旧 active chunk 标记为 inactive，再插入新 chunk。chunk 查询接口只返回 active chunk。处理成功后的最终状态是 `CHUNKED`，不是 `INDEXED`。

## 文档处理状态机规则

允许调用 `POST /api/documents/{id}/process` 的状态：

```text
UPLOADED
FAILED
PARSED
CHUNKED
```

不允许调用 process 的状态：

```text
PARSING
CHUNKING
EMBEDDING
INDEXING
INDEXED
```

不允许处理时返回 `DOCUMENT_PROCESS_NOT_ALLOWED`，不会静默处理，也不会把 `INDEXED` 文档重新切分。

`processingVersion` 规则：

```text
UPLOADED 首次处理：沿用 document 当前 processingVersion，通常是 1
FAILED / PARSED / CHUNKED 重新处理：processingVersion + 1
新生成 chunk 的 processingVersion 与 document.processingVersion 一致
```

失败状态记录规则：

```text
文件不存在：FAILED + failedStage=PARSING
不支持文件类型：FAILED + failedStage=PARSING
splitter 参数错误或切分失败：FAILED + failedStage=CHUNKING
```

失败时 `errorMessage` 会记录清晰错误信息，不返回 Java 堆栈。

## Embedding 抽象

当前 embedding 调用链：

```text
EmbeddingService
  ↓
EmbeddingClient
  ↓
MockEmbeddingClient 或 QwenEmbeddingClient
  ↓
生成固定维度向量
```

`EmbeddingService` 是业务侧入口，负责文本校验和向量维度校验。`EmbeddingClient` 只表达“文本到向量”的模型适配接口。`MockEmbeddingClient` 在 `app.embedding.provider=mock` 时生成确定性伪向量；`QwenEmbeddingClient` 只在 provider 为 `qwen` 时负责千问 HTTP 适配、鉴权、响应解析和维度校验。

当前没有开放 embedding HTTP 接口。embedding 流程只通过 `POST /api/documents/{id}/index` 接入文档索引链路，不在 `/process` 阶段提前执行。

## VectorStore 抽象

当前 VectorStore 调用链：

```text
DocumentIndexingService / RetrievalService
  ↓
VectorStoreService
  ↓
QdrantVectorStoreService
  ↓
Qdrant collection rag_chunks
```

`VectorStoreService` 是业务侧向量库入口，提供 collection 初始化、单条 chunk vector upsert、向量 search、按 documentId 删除向量的能力。`QdrantVectorStoreService` 只负责把这些操作适配成 Qdrant HTTP API 请求。

`DocumentIndexingService` 通过 `VectorStoreService` 写入向量，`RetrievalService` 通过同一入口检索向量。文档处理完成后先停在 `CHUNKED`；只有显式调用 `POST /api/documents/{id}/index` 后，才会生成 embedding、写入 Qdrant，并在成功后进入 `INDEXED`。

## 文档索引调用链

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

索引状态流转：

```text
CHUNKED
  ↓
EMBEDDING
  ↓
INDEXING
  ↓
INDEXED
```

失败规则：

```text
embedding 失败：FAILED + failedStage=EMBEDDING
Qdrant 写入失败：FAILED + failedStage=INDEXING
```

当前关系库中的 `document` 和 `document_chunk` 是事实源，Qdrant 是检索索引。如果部分 chunk 已经写入 Qdrant 后失败，本轮暂不做补偿删除，后续需要补充重建索引或补偿删除策略。

## 向量检索 API

```bash
curl -X POST http://localhost:8080/api/retrieval/search \
  -H "Content-Type: application/json" \
  -d '{"knowledgeBaseId":1,"question":"员工年假可以累计多久？","topK":5}'
```

`knowledgeBaseId` 和 `question` 必填。`topK` 为空时默认是 5，允许范围是 1 到 20，超过上限返回参数校验错误。

检索调用链：

```text
POST /api/retrieval/search
  ↓
RetrievalController
  ↓
RetrievalService
  ↓
EmbeddingService
  ↓
EmbeddingClient（mock 或 qwen）
  ↓
VectorStoreService.search
  ↓
Qdrant knowledgeBaseId filter
  ↓
DocumentChunkService 回查关系库 active chunk
  ↓
返回 RetrievedChunk
```

Qdrant search 始终使用 `knowledgeBaseId` payload filter，不能跨知识库检索。Qdrant 返回的 `chunkId` 会回查关系库；不存在、已 inactive 或不属于请求知识库的 chunk 会被跳过，最终 `content` 始终来自 `document_chunk`。

## Phase 4 手动验证结果

本地 Docker、PostgreSQL 和 Qdrant 环境已完成以下两条链路验证：

```text
上传文档
  ↓
POST /api/documents/{id}/process
  ↓
document.status = CHUNKED
  ↓
POST /api/documents/{id}/index
  ↓
document.status = INDEXED
  ↓
POST /api/retrieval/search
  ↓
返回当前 knowledgeBaseId 下的 active chunk
```

Mock 验证使用 `rag_chunks_mock_384`：

- 文档成功从 `UPLOADED` 流转到 `CHUNKED`，再流转到 `INDEXED`。
- 7 个 active chunk 全部写回 `vectorId`。
- 目标知识库返回 5 条检索结果。
- 隔离知识库返回 0 条结果。

千问验证使用 `rag_chunks_qwen_1024`：

- `text-embedding-v4` 成功生成 1024 维向量。
- 3 个 active chunk 全部写入 Qdrant 并写回 `vectorId`。
- 真实语义检索返回 3 条结果。
- 隔离知识库返回 0 条结果。

真实 API key 仅从本地 `.env` 读取，没有写入代码、README 或 Git。Mock 384 维和 Qwen 1024 维使用不同 collection。

## LLM 抽象与 PromptBuilder

当前 RAG 问答调用链：

```text
POST /api/chat/once
  ↓
ChatController
  ↓
ChatService
  ↓
ChatSessionService / ChatMessageService
  ↓
RetrievalService
  ↓
PromptBuilder
  ↓
LlmService
  ↓
LlmClient
  ↓
MockLlmClient 或 QwenLlmClient
  ↓
写入 llm_call_log
  ↓
保存 assistant message
  ↓
返回 answer + references
```

`PromptBuilder` 只根据用户问题和上下文片段构造 prompt，不调用模型、数据库或 Qdrant。生成的 RAG prompt 包含：

- 用户问题。
- 检索到的上下文片段。
- 只能基于上下文回答的约束。
- 上下文不足时说明“根据当前知识库内容无法确定”的约束。
- 使用 `[片段N]` 标注引用来源的要求。

`LlmService` 是业务侧 LLM 入口，负责校验 prompt、补充默认模型和 temperature，并调用 `LlmClient`。`MockLlmClient` 返回稳定、可预测的 mock answer；`QwenLlmClient` 只负责千问 Chat Completions HTTP 鉴权、请求和响应解析。

## 一次性 RAG 问答 API

```bash
curl -X POST http://localhost:8080/api/chat/once \
  -H "Content-Type: application/json" \
  -d '{"knowledgeBaseId":1,"sessionId":null,"question":"这份文档讲了什么？","topK":5}'
```

请求规则：

- `knowledgeBaseId` 必填。
- `question` 必填且不能为空。
- `topK` 为空时默认使用 5，允许范围为 1 到 20。
- `sessionId` 为空时创建新会话。
- `sessionId` 不为空时必须存在，且必须属于请求中的知识库。

响应包含：

```text
requestId
sessionId
userMessageId
assistantMessageId
answer
references
```

每次成功调用会生成一个 UUID requestId，并保存一条 `USER` 消息和一条 `ASSISTANT` 消息。两条消息使用同一个 requestId；assistant 消息的 `references_json` 保存本次回答引用的 chunk 信息。响应中的 references 和持久化引用都来自 RetrievalService 返回的关系库 active chunk。

该接口根据 `APP_LLM_PROVIDER` 使用 Mock LLM 或 Qwen LLM，并返回普通 JSON 响应。

## SSE RAG 问答 API

```bash
curl -N -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"knowledgeBaseId":1,"sessionId":null,"question":"这份文档讲了什么？","topK":5}'
```

请求参数规则与 `/api/chat/once` 相同。接口响应类型是 `text/event-stream`，事件顺序如下：

```text
retrieval_start
retrieval_result
answer_delta
references
done
```

发生异常时发送 `error` 事件并结束连接。每个事件的数据结构都包含：

```text
requestId
eventType
data
```

事件含义：

- `retrieval_start`：检索开始，并返回本次问答的 requestId。
- `retrieval_result`：返回检索到的关系库 active chunk 引用。
- `answer_delta`：返回一段回答文本，可能发送多次。
- `references`：返回本次回答的完整引用列表。
- `done`：明确表示完成，并返回 sessionId、userMessageId 和 assistantMessageId。
- `error`：返回不包含 Java 堆栈的清晰错误信息。

当前 SSE 是应用层流式输出：`LlmService.complete()` 先获取完整答案，再拆成多个 `answer_delta` 事件发送，尚不是真实模型 token streaming。`/api/chat/once` 与 `/api/chat/stream` 复用同一套事务化问答工作流，因此两者都会保存 USER / ASSISTANT message、retrieval_log 和 llm_call_log。

## 问答审计日志与 requestId

一次问答的追踪链路：

```text
POST /api/chat/once 或 POST /api/chat/stream
  ↓
生成 requestId
  ↓
USER chat_message
  ↓
retrieval_log 多条记录
  ↓
llm_call_log 一条记录
  ↓
ASSISTANT chat_message
  ↓
JSON 响应或 SSE events
```

使用问答响应中的 requestId 查询检索日志：

```bash
curl http://localhost:8080/api/audit/retrieval-logs/{requestId}
```

查询 LLM 调用日志：

```bash
curl http://localhost:8080/api/audit/llm-call-logs/{requestId}
```

日志按 `rankPosition` 升序返回，每条记录包含：

```text
requestId
sessionId
messageId
knowledgeBaseId
question
retrieverType
topK
chunkId
documentId
rankPosition
score
createdAt
```

`messageId` 指向本次用户问题消息，`rankPosition` 从 1 开始。检索结果为空时不写占位日志，查询该 requestId 会返回空列表，问答仍会使用空上下文 prompt 正常返回 Mock LLM 响应。

每次 LLM 调用会记录 provider、modelName、latencyMs、success 和 errorMessage。当前 LLM Client 没有暴露真实 usage，因此 `promptTokens` 和 `completionTokens` 保持为空。成功日志跟随问答主事务提交；LLM 失败日志使用独立事务保存，随后继续抛出原异常，主问答事务会回滚。

## Phase 5 完整链路验证

Phase 5 收尾时已实际启动 PostgreSQL、Qdrant 和应用，完成以下闭环：

```text
创建知识库
  ↓
上传 txt 文档
  ↓
process 生成 chunk
  ↓
index 写入 Qdrant
  ↓
retrieval/search 返回关系库 active chunk
  ↓
chat/once 返回 answer、references、requestId
  ↓
chat/stream 返回完整 SSE 事件
  ↓
按 requestId 查询 retrieval_log 和 llm_call_log
```

Mock 链路使用 `rag_chunks_phase5_6_mock_384` 和 384 维向量，文档成功流转到 `INDEXED`，once 与 stream 均写入消息和审计日志。

Qwen 真实链路使用：

```text
Embedding provider: qwen
Embedding model: text-embedding-v4
Vector dimension: 1024
Qdrant collection: rag_chunks_phase5_6_qwen_1024
LLM provider: qwen
LLM model: qwen-plus
```

真实链路的 upload、process、index、retrieval/search、chat/once 和 chat/stream 均验证通过。真实 API key 只从本地 `.env` 读取，没有输出到日志、文档或 Git。

## 当前已完成

- 初始化 Maven Spring Boot 项目。
- 配置 Java 17、Spring Boot 3.x 和基础 Web 能力。
- 新增统一响应结构 `ApiResponse` 和全局异常处理。
- 新增健康检查和数据库健康检查接口。
- 新增 Docker Compose，包含 PostgreSQL 和 Qdrant。
- 配置 PostgreSQL datasource、Hikari、Flyway 和 MyBatis-Plus。
- 新增 `knowledge_base` 表、Entity、Mapper、Service、DTO 和 REST CRUD API。
- 新增 `document` 表、Entity、Mapper、Service、Controller、元数据 API 和上传 API。
- 新增本地 storage、上传文件类型限制、大小限制和删除文件一致性处理。
- 新增 `document_chunk` 表、Entity、Mapper、Service 和 Response DTO。
- 新增 `DocumentParser` 抽象、`DocumentParserRegistry`、txt/md parser 和解析结果模型。
- 新增 `TextSplitter` 抽象、`FixedWindowTextSplitter`、`SplitOptions` 和 `TextChunk`。
- 新增 chunk 配置 `app.chunk.size` 和 `app.chunk.overlap`。
- 新增 `DocumentProcessingService`，集中编排文档解析、切分和 chunk 入库。
- 新增 `POST /api/documents/{id}/process`。
- 新增 `GET /api/documents/{documentId}/chunks` 和 `GET /api/chunks/{id}`。
- 新增 parser、splitter、chunk 和文档处理相关测试。
- 补强 process 可处理状态规则，禁止处理 `PARSING`、`CHUNKING`、`EMBEDDING`、`INDEXING`、`INDEXED`。
- 补强 `processingVersion` 递增规则，重新处理时版本递增。
- 重复处理时旧 chunk 改为软失效，新 chunk 保持 active。
- 失败时记录 `FAILED`、`failedStage` 和 `errorMessage`。
- 文档处理成功主线使用事务边界，失败状态在回滚后单独落库。
- Phase 3 收尾：已完成接口验证、处理链路导读和阶段总结文档。
- 新增 embedding 配置 `app.embedding.provider` 和 `app.embedding.dimension`。
- 新增 `EmbeddingProperties`。
- 新增 `EmbeddingClient` 抽象。
- 新增 `MockEmbeddingClient`，支持确定性固定维度 mock 向量。
- 新增 `EmbeddingService`，作为业务侧 embedding 调用入口。
- 新增 embedding 相关测试，覆盖维度、稳定性、空文本、batch 和 service 维度校验。
- 新增 Qdrant 配置 `app.qdrant.*`。
- 新增 `.env.qwen.example` 千问 embedding 配置模板。
- 新增 `VectorStoreService` 抽象。
- 新增 `QdrantVectorStoreService`，使用 Spring `RestClient` 适配 Qdrant HTTP API。
- 新增向量层模型 `ChunkVector`、`VectorSearchRequest`、`VectorSearchResult`。
- 新增 `GET /api/health/qdrant`，用于 Qdrant collection 初始化 / 连通性检查。
- 新增 Qdrant 配置、向量模型、请求构造和健康检查相关测试。
- 新增 `DocumentIndexingService`，编排 chunk embedding 和向量入库。
- 新增 `POST /api/documents/{id}/index`。
- active chunks 生成 embedding 后写入 `VectorStoreService`。
- 索引成功后写回 `document_chunk.vector_id`。
- 索引成功后 document 状态流转到 `INDEXED`。
- embedding / Qdrant 写入失败时记录 `FAILED`、`failedStage` 和 `errorMessage`。
- 新增文档索引相关服务测试和接口测试。
- 新增 `QwenEmbeddingClient`，支持 DashScope OpenAI-compatible batch embeddings API。
- 新增 `RetrievalService`，集中编排 query embedding、向量检索和关系库 chunk 回查。
- 新增 `POST /api/retrieval/search`。
- Qdrant search 强制使用 `knowledgeBaseId` payload filter。
- 检索结果只返回关系库中存在且 active 的 chunk，关系库 content 是事实源。
- 新增千问客户端、检索服务、参数校验和知识库过滤相关测试。
- 新增 LLM 配置 `app.llm.provider` 和 `app.llm.model`。
- 新增 `LlmClient`、`MockLlmClient`、`LlmService` 和 LLM 请求响应模型。
- 新增 `PromptBuilder`、`RagPromptBuilder` 和 prompt 上下文模型。
- 新增 Mock LLM、LlmService 和 PromptBuilder 单元测试。
- 新增 `chat_session` 和 `chat_message` 表及索引。
- 新增 ChatSession、ChatMessage、Mapper 和基础数据访问 Service。
- 新增 `ChatService`，编排 session、消息持久化、retrieval、prompt 和 LLM 调用。
- 新增 `POST /api/chat/once`。
- 一次性问答返回 answer 和 references，并将 references 序列化到 assistant message。
- 新增 chat migration、持久化、业务编排、参数校验和 HTTP 闭环测试。
- 新增 `retrieval_log` 表及 requestId、sessionId、knowledgeBaseId 索引。
- `chat_message` 新增 `request_id` 字段和索引。
- `/api/chat/once` 生成并返回 requestId，两条 chat message 使用同一 requestId。
- ChatService 按检索顺序写入 retrieval_log，rankPosition 从 1 开始。
- 新增 `GET /api/audit/retrieval-logs/{requestId}` 开发排查接口。
- 新增 requestId、日志持久化、排序查询和空请求防脏数据测试。
- 扩展 `LlmProperties`，支持 baseUrl、apiKey、temperature 和 maxTokens。
- 新增 `QwenLlmClient`，适配 DashScope OpenAI-compatible Chat Completions API。
- 补充 `.env.qwen.example` 千问 LLM 配置模板。
- 默认 provider 继续使用 MockLlmClient，不依赖真实 API key。
- 新增 Qwen LLM 鉴权、请求体、响应解析和错误处理测试。
- 使用本地 key 和 `qwen-plus` 完成真实 Chat Completions 最小验证。
- 新增 `llm_call_log` 表及 requestId、sessionId 索引。
- 新增 LlmCallLog Entity、Mapper、Service、DTO 和查询 Controller。
- 新增 `GET /api/audit/llm-call-logs/{requestId}`。
- `/api/chat/once` 会记录 LLM provider、model、耗时、成功状态和错误摘要。
- 新增 `POST /api/chat/stream`，通过 SSE 返回检索、回答片段、引用、完成和错误事件。
- `/api/chat/once` 与 `/api/chat/stream` 复用同一套问答工作流。
- stream 流程同样保存 chat_message、retrieval_log 和 llm_call_log。
- 新增 LLM 成功 / 失败日志、SSE 顺序、参数校验和持久化测试。
- 完成 Mock Embedding + Mock LLM 的 RAG 问答完整链路验证。
- 完成 `text-embedding-v4` + `qwen-plus` 的真实 RAG 问答完整链路验证。
- 完成 Phase 5 问答、SSE 和 requestId 审计链路导读。

## 本阶段刻意不做

- 不接 OpenAI LLM API。
- 不做真实模型 token streaming。
- 不做 token 统计。
- 不做复杂多轮记忆。
- 不做 reranker。
- 不做 Redis / Elasticsearch / RabbitMQ。
- 不做异步任务系统或消息队列。
- 不做分布式事务。
- 不做复杂重试。
- 不做 PDF / docx 解析。
- 不做登录、JWT、Spring Security 权限系统。

## 阶段文档

- Phase 1 总结：`docs/phase-notes/phase-001-summary.md`
- Phase 2 总结：`docs/phase-notes/phase-002-summary.md`
- Phase 3 总结：`docs/phase-notes/phase-003-summary.md`
- Phase 3.1 实现说明：`docs/round-notes/round-010-implementation-notes.md`
- Phase 3.2 实现说明：`docs/round-notes/round-011-implementation-notes.md`
- Phase 3.3 实现说明：`docs/round-notes/round-012-implementation-notes.md`
- Phase 3.4 实现说明：`docs/round-notes/round-013-implementation-notes.md`
- Phase 4.1 实现说明：`docs/round-notes/round-014-implementation-notes.md`
- Phase 4.2 实现说明：`docs/round-notes/round-015-implementation-notes.md`
- Phase 4.3 实现说明：`docs/round-notes/round-016-implementation-notes.md`
- Phase 4.4 实现说明：`docs/round-notes/round-017-implementation-notes.md`
- Phase 4 总结：`docs/phase-notes/phase-004-summary.md`
- Phase 4.5 实现说明：`docs/round-notes/round-018-implementation-notes.md`
- Phase 5.1 实现说明：`docs/round-notes/round-019-implementation-notes.md`
- Phase 5.2 实现说明：`docs/round-notes/round-020-implementation-notes.md`
- Phase 5.3 实现说明：`docs/round-notes/round-021-implementation-notes.md`
- Phase 5.4 实现说明：`docs/round-notes/round-022-implementation-notes.md`
- Phase 5.5 实现说明：`docs/round-notes/round-023-implementation-notes.md`
- Phase 5 总结：`docs/phase-notes/phase-005-summary.md`
- Phase 5.6 实现说明：`docs/round-notes/round-024-implementation-notes.md`

## 下一步计划

进入 Phase 6.1：`evaluation_dataset` / `evaluation_question` 表与评测集导入基础。下一阶段重点是评测模块，不是继续增加模型供应商或模型能力。
