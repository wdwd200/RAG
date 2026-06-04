# Round 002 Implementation Notes

## 1. 本轮目标

本轮执行 Phase 1.2：Docker Compose + 数据库连接 + migration 基础。

目标是让项目具备本地 PostgreSQL、Qdrant、Spring Boot 数据库连接、Flyway migration baseline 和数据库连通性检查能力，为后续 `knowledge_base` 表和 CRUD 做准备。

## 2. 本轮完成内容

- 新增 PostgreSQL、Spring JDBC、Flyway 和测试用 H2 依赖。
- 新增 `docker-compose.yml`，包含 PostgreSQL 和 Qdrant。
- 更新 `.env.example`，补充 PostgreSQL、Qdrant 和服务端口配置。
- 更新 `application.yml`，增加 datasource、Hikari 和 Flyway 配置。
- 新增 Flyway baseline migration：`V1__init_database.sql`。
- 新增数据库连通性检查 Service。
- 新增数据库健康检查接口：`GET /api/health/database`。
- 新增测试 profile 配置，测试环境使用 H2，不依赖 Docker。
- 新增数据库健康检查 Service 测试。
- 更新 README 的启动和验证说明。

## 3. 新增或修改文件

- `pom.xml`
- `.env.example`
- `docker-compose.yml`
- `README.md`
- `src/main/resources/application.yml`
- `src/main/resources/db/migration/V1__init_database.sql`
- `src/main/java/com/example/ragbackend/health/DatabaseHealthController.java`
- `src/main/java/com/example/ragbackend/infrastructure/database/DatabaseHealthService.java`
- `src/main/java/com/example/ragbackend/infrastructure/database/DatabaseHealthResponse.java`
- `src/test/resources/application-test.yml`
- `src/test/java/com/example/ragbackend/RagBackendApplicationTests.java`
- `src/test/java/com/example/ragbackend/infrastructure/database/DatabaseHealthServiceTest.java`
- `docs/round-notes/round-002-implementation-notes.md`

## 4. 核心文件职责

`docker-compose.yml`：定义本地开发用 PostgreSQL 和 Qdrant 容器，以及对应数据卷和端口映射。

`.env.example`：提供本地开发环境变量示例。示例密码只用于本地开发，不用于生产环境。

`application.yml`：集中维护应用端口、数据库连接、Hikari 连接池、Flyway、Swagger 和 Actuator 配置。

`V1__init_database.sql`：Flyway baseline migration，只执行 `SELECT 1` 验证 migration 链路，不创建业务表。

`DatabaseHealthResponse.java`：数据库健康检查响应对象，只包含安全的状态信息。

`DatabaseHealthService.java`：使用 `JdbcTemplate` 执行 `SELECT 1`，并返回数据库连通性状态。

`DatabaseHealthController.java`：暴露 `GET /api/health/database`，只调用 Service 并包装统一响应。

`application-test.yml`：测试环境数据源配置，使用 H2 PostgreSQL 兼容模式。

`DatabaseHealthServiceTest.java`：验证数据库健康检查逻辑在测试环境可用。

## 5. 接口入口

保留原有接口：

```text
GET /api/health
```

本轮新增接口：

```text
GET /api/health/database
```

Swagger UI：

```text
http://localhost:8080/swagger-ui/index.html
```

## 6. 请求调用链

数据库健康检查调用链：

```text
GET /api/health/database
  ↓
DatabaseHealthController
  ↓
DatabaseHealthService
  ↓
JdbcTemplate
  ↓
PostgreSQL / H2
```

正常请求时，`DatabaseHealthService` 执行 `SELECT 1`，查询成功后返回 `DatabaseHealthResponse`，Controller 再包装为 `ApiResponse.success(data)`。

异常请求时，Service 抛出 `BusinessException`，`GlobalExceptionHandler` 将异常转换为统一的 `ApiResponse.error(code, message)`，不会向前端暴露 Java 堆栈、数据库密码或完整 JDBC URL。

## 7. 配置说明

`server.port`：服务端口，默认读取 `${SERVER_PORT:8080}`。

`spring.datasource.url`：PostgreSQL JDBC 地址，默认连接 `localhost:5432/rag_backend`。

`spring.datasource.username`：数据库用户名，默认 `${POSTGRES_USER:rag_user}`。

`spring.datasource.password`：数据库密码，默认 `${POSTGRES_PASSWORD:rag_password}`。

`spring.datasource.hikari.maximum-pool-size`：Hikari 最大连接数，当前为 `10`。

`spring.datasource.hikari.minimum-idle`：Hikari 最小空闲连接数，当前为 `2`。

`spring.datasource.hikari.connection-timeout`：连接超时时间，当前为 `30000` 毫秒。

`spring.flyway.enabled`：启用 Flyway migration。

`spring.flyway.locations`：migration 文件目录，当前为 `classpath:db/migration`。

测试环境使用 `application-test.yml`，数据源切换为 H2 内存数据库，避免 `mvn test` 依赖 Docker 或本地 PostgreSQL。

## 8. Docker Compose 说明

PostgreSQL 用于后续保存知识库、文档元数据、检索记录等关系型数据。本轮只建立连接和 migration 基础，不创建业务表。

Qdrant 用于后续向量存储和向量检索。本轮只提供容器，不引入 Qdrant Java 客户端，也不在代码中连接 Qdrant。

Compose 中的默认数据库名、用户名和密码支持环境变量覆盖。默认值只是本地开发示例，不应在生产环境中使用。

## 9. Flyway migration 说明

Flyway 用于管理数据库结构版本。每个 migration 文件只执行一次，执行记录会写入 Flyway 的 schema history 表。

本轮新增的 `V1__init_database.sql` 是 baseline：

```sql
SELECT 1;
```

它只验证 Flyway 链路可用，不创建 `knowledge_base`、`document` 或任何业务表。业务表留到后续阶段。

## 10. 如何运行和验证

启动本地基础设施：

```bash
docker compose up -d
```

查看容器状态：

```bash
docker compose ps
```

运行测试：

```bash
mvn test
```

启动应用：

```bash
mvn spring-boot:run
```

验证应用健康检查：

```bash
curl http://localhost:8080/api/health
```

验证数据库健康检查：

```bash
curl http://localhost:8080/api/health/database
```

打开 Swagger：

```text
http://localhost:8080/swagger-ui/index.html
```

## 11. 本轮刻意不做的事项

- 不创建 `knowledge_base` 表。
- 不创建 `knowledge_base` Entity。
- 不创建 `knowledge_base` Mapper / Repository。
- 不实现 `knowledge_base` CRUD。
- 不实现文档上传或文档解析。
- 不实现 chunk 切分。
- 不实现 embedding。
- 不连接 Qdrant Java 客户端。
- 不实现向量检索。
- 不实现 LLM。
- 不实现 SSE。
- 不引入 Redis、Elasticsearch、RabbitMQ。
- 不实现登录、JWT、Spring Security 或复杂权限系统。

## 12. 下一轮建议

下一轮进入 Phase 1.3：`knowledge_base` 表与基础数据访问。

建议下一轮在 Flyway 中新增 `knowledge_base` 表 migration，并补充最小数据访问层，但仍不要提前实现完整 CRUD 或文档处理流程。
