# Round 003 Implementation Notes

## 1. 本轮目标

本轮执行 Phase 1.3：`knowledge_base` 表与基础数据访问。

目标是补齐知识库基础持久化层，包括数据库表、Entity、Mapper、Service、DTO 和测试，为下一轮 REST CRUD API 做准备。

本轮不实现 Controller，也不对外暴露知识库 CRUD 接口。

## 2. 本轮完成内容

- 新增 Flyway migration：`V2__create_knowledge_base_table.sql`。
- 创建 `knowledge_base` 表。
- 新增 `idx_knowledge_base_owner_id` 索引。
- 引入 MyBatis-Plus Spring Boot 3 starter。
- 新增 MyBatis-Plus Mapper 扫描配置。
- 新增 `KnowledgeBase` Entity。
- 新增 `KnowledgeBaseMapper`。
- 新增 `KnowledgeBaseService` 和 `KnowledgeBaseServiceImpl`。
- 新增 `KnowledgeBaseCreateRequest` 和 `KnowledgeBaseResponse` DTO。
- 新增知识库持久化测试。
- 更新 README 阶段说明。

## 3. 新增或修改文件

- `pom.xml`
- `README.md`
- `src/main/resources/application.yml`
- `src/main/resources/db/migration/V2__create_knowledge_base_table.sql`
- `src/main/java/com/example/ragbackend/config/MybatisPlusConfig.java`
- `src/main/java/com/example/ragbackend/knowledge/entity/KnowledgeBase.java`
- `src/main/java/com/example/ragbackend/knowledge/mapper/KnowledgeBaseMapper.java`
- `src/main/java/com/example/ragbackend/knowledge/service/KnowledgeBaseService.java`
- `src/main/java/com/example/ragbackend/knowledge/service/impl/KnowledgeBaseServiceImpl.java`
- `src/main/java/com/example/ragbackend/knowledge/dto/KnowledgeBaseCreateRequest.java`
- `src/main/java/com/example/ragbackend/knowledge/dto/KnowledgeBaseResponse.java`
- `src/test/java/com/example/ragbackend/knowledge/KnowledgeBasePersistenceTest.java`
- `docs/round-notes/round-003-implementation-notes.md`

## 4. 核心文件职责

`V2__create_knowledge_base_table.sql`：创建 `knowledge_base` 表和 owner_id 索引。

`MybatisPlusConfig.java`：配置 Mapper 扫描路径，让 Spring 能发现 `knowledge.mapper` 下的 Mapper。

`KnowledgeBase.java`：对应 `knowledge_base` 表的 Entity，字段与表字段保持清晰对应。

`KnowledgeBaseMapper.java`：继承 MyBatis-Plus `BaseMapper`，负责 `knowledge_base` 表的数据访问。

`KnowledgeBaseService.java`：定义知识库基础数据访问能力，包括 `create`、`findById`、`findAll` 和 `existsById`。

`KnowledgeBaseServiceImpl.java`：实现基础数据访问逻辑，保持简单，不写复杂业务规则。

`KnowledgeBaseCreateRequest.java`：创建知识库时的请求 DTO，下一轮 Controller 可以复用。

`KnowledgeBaseResponse.java`：知识库查询结果 DTO，用于隔离 Entity 和对外返回结构。

`KnowledgeBasePersistenceTest.java`：验证 Flyway 建表、Mapper 插入查询和 Service 基础方法。

## 5. 数据库结构

本轮新增表：

```text
knowledge_base
```

字段：

```text
id
name
description
owner_id
visibility
document_count
chunk_count
created_at
updated_at
```

索引：

```text
idx_knowledge_base_owner_id
```

本轮没有创建 `user`、`document`、`knowledge_base_member` 或其他业务表。

## 6. 调用链

本轮没有新增 REST Controller。当前持久化调用链是：

```text
Service
  ↓
Mapper
  ↓
knowledge_base 表
```

具体到创建知识库：

```text
KnowledgeBaseService.create()
  ↓
KnowledgeBaseServiceImpl
  ↓
KnowledgeBaseMapper.insert()
  ↓
knowledge_base 表
```

具体到查询知识库：

```text
KnowledgeBaseService.findById() / findAll() / existsById()
  ↓
KnowledgeBaseServiceImpl
  ↓
KnowledgeBaseMapper.selectById() / selectList()
  ↓
knowledge_base 表
```

下一轮 REST API 加入后，请求链路才会变成：

```text
Controller
  ↓
Service
  ↓
Mapper
  ↓
knowledge_base 表
```

## 7. 配置说明

`pom.xml` 新增 MyBatis-Plus Spring Boot 3 starter：

```text
com.baomidou:mybatis-plus-spring-boot3-starter
```

`application.yml` 新增 MyBatis-Plus 基础配置：

```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
```

该配置让 `owner_id`、`created_at` 等下划线字段能映射到 `ownerId`、`createdAt` 等 Java 字段。

`MybatisPlusConfig.java` 使用 `@MapperScan` 扫描：

```text
com.example.ragbackend.knowledge.mapper
```

## 8. 测试说明

测试继续使用 `application-test.yml` 中的 H2 内存数据库，不依赖 Docker 或本地 PostgreSQL。

`mvn test` 会执行 Flyway：

```text
V1__init_database.sql
V2__create_knowledge_base_table.sql
```

`KnowledgeBasePersistenceTest` 覆盖：

- Flyway 能创建 `knowledge_base` 表。
- Mapper 能插入和查询知识库。
- Service 能创建、查询、判断存在。

## 9. 如何运行和验证

运行测试：

```bash
mvn test
```

如需连接本地 PostgreSQL，可先启动基础设施：

```bash
docker compose up -d
```

再启动应用：

```bash
mvn spring-boot:run
```

如果 Windows 中文路径下 `mvn spring-boot:run` 遇到 classpath 编码问题，可使用：

```bash
mvn package
java -jar target/rag-backend-0.0.1-SNAPSHOT.jar
```

## 10. 本轮刻意不做的事项

- 不实现 `KnowledgeBaseController`。
- 不实现 `knowledge_base` REST CRUD API。
- 不实现文档上传。
- 不创建 `document` 表。
- 不实现 chunk 切分。
- 不实现 embedding。
- 不连接 Qdrant Java 客户端。
- 不实现向量检索。
- 不实现 LLM。
- 不实现 SSE。
- 不引入 Redis、Elasticsearch、RabbitMQ。
- 不实现 Spring Security 或 JWT。

## 11. 下一轮建议

下一轮进入 Phase 1.4：`knowledge_base` 最小 CRUD API。

建议下一轮在当前 Service 基础上新增 Controller，并只实现知识库最小 CRUD 接口，不要提前进入文档上传、chunk、embedding 或向量检索。
