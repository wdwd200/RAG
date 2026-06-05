# RAG 后端知识库

RAG 后端知识库是一个面向知识库管理、文档上传、后续文档解析与检索增强生成能力的 Spring Boot 后端项目。

当前阶段：Phase 3.1 已完成。本阶段新增了 `document_chunk` 表、chunk 基础数据访问代码、`DocumentParser` 抽象，以及 txt/md 本地文本解析器。本阶段不生成 chunk，也不提供文档处理 API。

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

Spring multipart 默认上限配置为 20MB request body 21MB，用于保证超过 10MB 的文件先进入业务校验并返回统一 `FILE_TOO_LARGE` 响应。

## 健康检查

应用健康检查：

```bash
curl http://localhost:8080/api/health
```

数据库连通性检查：

```bash
curl http://localhost:8080/api/health/database
```

数据库健康检查会执行 `SELECT 1`，只返回数据库状态、数据库类型、校验 SQL 和检查时间，不返回数据库密码、用户名或完整 JDBC URL。

Actuator 健康检查：

```bash
curl http://localhost:8080/actuator/health
```

## Swagger

启动项目后访问：

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

当前 Swagger 页面能看到健康检查接口、知识库 CRUD 接口、文档元数据接口和文件上传接口。Phase 3.1 没有新增 chunk REST Controller，因此不会出现 chunk 查询 API。

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

## 文档元数据 API

文档元数据接口可用，只创建和管理文档记录，不接收 multipart 文件，也不做文档解析。

创建文档元数据：

```bash
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{"knowledgeBaseId":1,"fileName":"demo.pdf","fileType":"pdf","fileSize":1024,"storagePath":"documents/demo.pdf","createdBy":1}'
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

删除 document 时，如果记录中存在 `storagePath`，系统会先尝试删除对应的本地文件，再删除数据库记录。本地文件已不存在时，删除 document 仍可成功。

## 文件上传 API

文件上传接口可用，支持 multipart 上传、本地 storage、文件类型限制、文件大小限制，以及删除 document 时同步删除本地文件。

上传文件并创建文档记录：

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "knowledgeBaseId=1" \
  -F "createdBy=1" \
  -F "file=@/path/to/demo.txt"
```

上传成功后，原始文件会保存到本地 storage，接口会返回 document 记录，状态为 `UPLOADED`。

当前允许的默认后缀为 `txt,md,pdf,docx`。默认单文件最大大小为 10MB。

## Phase 3.1 文档解析与 chunk 基础

本阶段新增 `document_chunk` 表，用于为后续 TextSplitter 和 embedding 链路保存 chunk 事实数据。当前只提供 Entity、Mapper、Service，不提供 REST Controller。

Chunk 数据访问链路：

```text
DocumentChunkService
  ↓
DocumentChunkMapper
  ↓
document_chunk 表
```

本阶段新增 `DocumentParser` 抽象和 `DocumentParserRegistry`，并实现 txt/md 解析器。txt 和 md 当前都按 UTF-8 普通文本读取，Markdown 暂不做 AST 解析。

Parser 调用链：

```text
DocumentParserRegistry
  ↓
TxtDocumentParser / MarkdownDocumentParser
  ↓
读取本地文件
  ↓
ParsedDocument
```

当前 parser 只负责读取本地文件并返回 `ParsedDocument`。parser 不切分 chunk，不更新 document 状态，不写数据库。

## 当前已完成

- 初始化 Maven Spring Boot 项目。
- 配置 Java 17、Spring Boot 3.x 和基础 Web 能力。
- 新增统一响应结构 `ApiResponse`。
- 新增业务异常 `BusinessException`。
- 新增全局异常处理 `GlobalExceptionHandler`。
- 保留统一风格健康检查接口 `GET /api/health`。
- 新增数据库健康检查接口 `GET /api/health/database`。
- 新增 Docker Compose，包含 PostgreSQL 和 Qdrant。
- 配置 PostgreSQL datasource、Hikari 和 Flyway。
- 新增 Flyway baseline migration：`V1__init_database.sql`。
- 新增 `knowledge_base` 表 migration：`V2__create_knowledge_base_table.sql`。
- 引入 MyBatis-Plus，并配置 Mapper 扫描。
- 新增知识库 Entity、Mapper、Service、DTO 和 REST CRUD API。
- 新增知识库持久化与 Controller 测试。
- 新增 `document` 表 migration：`V3__create_document_table.sql`。
- 新增文档状态枚举 `DocumentStatus`。
- 新增文档 Entity、Mapper、Service、Controller 和元数据 API。
- 新增本地 storage 配置、`FileStorageService` 和 `LocalFileStorageService`。
- 新增文件上传接口 `POST /api/documents/upload`。
- 上传成功后保存原始文件并创建 document 记录。
- 新增上传文件类型和大小限制。
- 删除 document 时同步删除本地文件。
- 新增 `document_chunk` 表 migration：`V4__create_document_chunk_table.sql`。
- 新增 `DocumentChunk` Entity、Mapper、Service 和 Response DTO。
- 新增 `DocumentParser` 抽象、`DocumentParserRegistry`、txt/md parser 和解析结果模型。
- 新增 parser 与 chunk 基础测试。

## 本阶段刻意不做

- 不做 TextSplitter。
- 不做 chunk 切分和 chunk 生成。
- 不做 chunk 查询 Controller 或 REST API。
- 不做 `/api/documents/{id}/process`。
- 不改变 document 状态流转。
- 不做 PDF / docx 解析。
- 不做 embedding。
- 不接入 Qdrant Java 客户端。
- 不做向量检索。
- 不做 LLM。
- 不做 SSE。
- 不做 Redis / Elasticsearch / RabbitMQ。
- 不做登录、JWT、Spring Security 权限系统。

## 阶段文档

- Phase 1 总结：`docs/phase-notes/phase-001-summary.md`
- Phase 2 总结：`docs/phase-notes/phase-002-summary.md`
- Phase 3.1 实现说明：`docs/round-notes/round-010-implementation-notes.md`

## 下一步计划

进入 Phase 3.2：TextSplitter、文档处理服务与 chunk 入库。
