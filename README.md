# RAG 后端知识库

RAG 后端知识库是一个面向知识库管理和后续检索增强生成能力的 Spring Boot 后端项目。

当前阶段：Phase 1.2：Docker Compose + 数据库连接 + migration 基础。

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
- PostgreSQL
- Qdrant
- H2 Test Database
- Lombok
- JUnit 5 / Spring Boot Test

## 本地基础设施

本轮提供本地开发用的 PostgreSQL 和 Qdrant 容器。

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

启动应用：

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

健康检查接口 `/api/health` 和 `/api/health/database` 可以在 Swagger 页面中看到。

## 当前已完成

- 初始化 Maven Spring Boot 项目。
- 配置 Java 17、Spring Boot 3.x 和基础 Web 能力。
- 新增统一响应结构 `ApiResponse`。
- 新增业务异常 `BusinessException`。
- 新增全局异常处理 `GlobalExceptionHandler`。
- 保留统一风格健康检查接口 `GET /api/health`。
- 新增数据库健康检查接口 `GET /api/health/database`。
- 新增 PostgreSQL JDBC、Spring JDBC、Flyway 和测试用 H2。
- 新增 Docker Compose，包含 PostgreSQL 和 Qdrant。
- 新增 Flyway baseline migration：`V1__init_database.sql`。
- 配置 Springdoc OpenAPI / Swagger UI。
- 配置 Actuator 暴露 `health` 和 `info`。

## 本轮尚未实现

- `knowledge_base` 表
- `knowledge_base` Entity / Mapper / Repository
- `knowledge_base` CRUD
- 文档上传
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

进入 Phase 1.3：`knowledge_base` 表与基础数据访问。
