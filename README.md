# RAG 后端知识库

RAG 后端知识库是一个面向知识库管理、文档上传、文档解析、chunk 入库和后续检索增强生成能力的 Spring Boot 后端项目。

当前阶段：Phase 4.1 已完成。项目已支持 txt/md 文档解析、固定窗口文本切分、`document_chunk` 入库、chunk 查询接口、处理状态机，以及 embedding 抽象、Mock Embedding 和向量维度配置。

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

当前只提供 Mock Embedding，用于验证后续向量入库流程的代码边界。Mock 向量由文本 hash 确定性生成，同一段文本多次生成结果稳定一致；当前不连接真实 embedding API，也不写入 Qdrant。

## 健康检查

应用健康检查：

```bash
curl http://localhost:8080/api/health
```

数据库连通性检查：

```bash
curl http://localhost:8080/api/health/database
```

Actuator 健康检查：

```bash
curl http://localhost:8080/actuator/health
```

## Swagger

启动项目后访问：

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

当前 Swagger 页面能看到健康检查接口、知识库 CRUD 接口、文档元数据接口、文件上传接口、文档处理接口和 chunk 查询接口。

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
MockEmbeddingClient
  ↓
生成固定维度向量
```

`EmbeddingService` 是业务侧入口，负责文本校验和向量维度校验。`EmbeddingClient` 只表达“文本到向量”的模型适配接口。`MockEmbeddingClient` 只负责在 `app.embedding.provider=mock` 时生成确定性的伪向量，维度来自 `app.embedding.dimension`。

当前没有开放 embedding HTTP 接口，也没有把 embedding 流程接入文档处理链路；文档处理成功后的状态仍然是 `CHUNKED`，不是 `INDEXED`。

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

## 本阶段刻意不做

- 不接真实 embedding API。
- 不写入 Qdrant。
- 不做向量入库和向量检索。
- 不修改 document 状态为 `INDEXED`。
- 不做 LLM。
- 不做 SSE。
- 不做 Redis / Elasticsearch / RabbitMQ。
- 不做异步处理。
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

## 下一步计划

进入 Phase 4.2：Qdrant collection 初始化与 VectorStoreService 抽象。下一轮才开始处理向量库侧的 collection 和向量存储边界；当前版本仍没有实现向量入库或向量检索。
