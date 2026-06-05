# RAG 后端知识库

RAG 后端知识库是一个面向知识库管理和后续检索增强生成能力的 Spring Boot 后端项目。

当前阶段：Phase 2.1，`document` 表与文档元数据 API 已可用。

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

本地开发使用 Docker Compose 启动 PostgreSQL 和 Qdrant。

启动基础设施：

```bash
docker compose up -d
```

查看容器状态：

```bash
docker compose ps
```

默认数据库配置来自 `.env.example`，示例账号密码只用于本地开发，不用于生产环境。实际本地运行时可以复制为 `.env` 后按需调整，`.env` 已被 `.gitignore` 忽略。

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

## 健康检查

应用健康检查：

```bash
curl http://localhost:8080/api/health
```

数据库连通性检查：

```bash
curl http://localhost:8080/api/health/database
```

数据库健康检查会执行 `SELECT 1`，只返回数据库状态、数据库类型、校验 SQL 和检查时间，不会返回数据库密码、用户名或完整 JDBC URL。

Actuator 健康检查：

```bash
curl http://localhost:8080/actuator/health
```

## Swagger

启动项目后访问：

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

当前 Swagger 页面能看到健康检查接口、知识库 CRUD 接口和文档元数据接口。

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

本阶段的文档接口只创建和管理文档元数据，不接收 multipart 文件，也不做文档解析。

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
- 新增 `KnowledgeBase` Entity。
- 新增 `KnowledgeBaseMapper`。
- 新增 `KnowledgeBaseService` 和 `KnowledgeBaseServiceImpl`。
- 新增 `KnowledgeBaseCreateRequest`、`KnowledgeBaseUpdateRequest` 和 `KnowledgeBaseResponse` DTO。
- 新增知识库持久化测试，覆盖表创建、Mapper 插入查询和 Service 基础方法。
- 新增 `KnowledgeBaseController`。
- 新增 `knowledge_base` 最小 REST CRUD API。
- 新增知识库 CRUD API 测试。
- 新增 `document` 表 migration：`V3__create_document_table.sql`。
- 新增文档状态枚举 `DocumentStatus`。
- 新增 `Document` Entity、Mapper、Service 和 Controller。
- 新增文档元数据 API。
- 新增文档元数据接口测试。

## 本轮尚未实现

- 文档上传
- multipart 文件接收
- 本地文件 storage
- 文档解析
- chunk 切分
- embedding
- 向量检索
- Qdrant Java 客户端连接
- LLM
- SSE
- 评测
- Redis / Elasticsearch / RabbitMQ
- 登录、JWT、Spring Security 权限系统

## 下一步计划

进入 Phase 2.2：文件上传接口与本地 storage。
