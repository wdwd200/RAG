# Phase 001 Summary

## 1. Phase 1 完成了什么

Phase 1 已将空仓库推进为一个可启动、可测试、具备基础知识库管理能力的 Spring Boot 后端项目。

已完成内容：

- 初始化 Maven Spring Boot 项目。
- 建立统一响应 `ApiResponse`。
- 建立业务异常 `BusinessException` 和全局异常处理 `GlobalExceptionHandler`。
- 提供应用健康检查 `GET /api/health`。
- 提供数据库健康检查 `GET /api/health/database`。
- 使用 Docker Compose 准备 PostgreSQL 和 Qdrant 基础设施。
- 配置 PostgreSQL datasource、Hikari 和 Flyway。
- 新增 `knowledge_base` 表。
- 接入 MyBatis-Plus。
- 实现 `knowledge_base` 持久化访问。
- 实现知识库最小 CRUD API。
- 使用 H2 完成持久化测试和 MockMvc API 测试。
- 补充 Swagger、README 和每轮实现说明。

## 2. 当前项目目录结构

```text
src/main/java/com/example/ragbackend
├── RagBackendApplication.java
├── common
│   ├── exception
│   │   ├── BusinessException.java
│   │   └── GlobalExceptionHandler.java
│   └── response
│       └── ApiResponse.java
├── config
│   ├── MybatisPlusConfig.java
│   └── OpenApiConfig.java
├── health
│   ├── DatabaseHealthController.java
│   └── HealthController.java
├── infrastructure
│   └── database
│       ├── DatabaseHealthResponse.java
│       └── DatabaseHealthService.java
└── knowledge
    ├── controller
    │   └── KnowledgeBaseController.java
    ├── dto
    │   ├── KnowledgeBaseCreateRequest.java
    │   ├── KnowledgeBaseResponse.java
    │   └── KnowledgeBaseUpdateRequest.java
    ├── entity
    │   └── KnowledgeBase.java
    ├── mapper
    │   └── KnowledgeBaseMapper.java
    └── service
        ├── KnowledgeBaseService.java
        └── impl
            └── KnowledgeBaseServiceImpl.java
```

## 3. knowledge_base CRUD 的代码调用链

```text
HTTP Request
  ↓
KnowledgeBaseController
  ↓
KnowledgeBaseService
  ↓
KnowledgeBaseMapper
  ↓
knowledge_base 表
```

错误处理调用链：

```text
BusinessException
  ↓
GlobalExceptionHandler
  ↓
ApiResponse.error(...)
```

## 4. 每个核心文件的职责

`ApiResponse.java`：统一接口响应结构，保证成功和失败返回格式一致。

`BusinessException.java`：表达可预期的业务错误，例如知识库不存在。

`GlobalExceptionHandler.java`：统一处理参数校验异常、业务异常和兜底异常。

`MybatisPlusConfig.java`：配置 Mapper 扫描路径。

`HealthController.java`：提供应用健康检查。

`DatabaseHealthController.java`：提供数据库连通性检查。

`KnowledgeBase.java`：映射 `knowledge_base` 表。

`KnowledgeBaseMapper.java`：基于 MyBatis-Plus 访问 `knowledge_base` 表。

`KnowledgeBaseService.java`：定义知识库创建、查询、更新、删除能力。

`KnowledgeBaseServiceImpl.java`：实现知识库 CRUD，并处理不存在数据时的业务错误。

`KnowledgeBaseController.java`：暴露知识库 CRUD HTTP API。

`KnowledgeBaseCreateRequest.java`：创建知识库请求 DTO。

`KnowledgeBaseUpdateRequest.java`：更新知识库请求 DTO。

`KnowledgeBaseResponse.java`：知识库响应 DTO。

## 5. 如何启动项目

启动基础设施：

```bash
docker compose up -d
```

打包应用：

```bash
mvn clean package
```

启动应用：

```bash
java -jar target/rag-backend-0.0.1-SNAPSHOT.jar
```

## 6. 如何验证接口

应用健康检查：

```bash
curl http://localhost:8080/api/health
```

数据库健康检查：

```bash
curl http://localhost:8080/api/health/database
```

查询知识库列表：

```bash
curl http://localhost:8080/api/knowledge-bases
```

创建知识库：

```bash
curl -X POST http://localhost:8080/api/knowledge-bases \
  -H "Content-Type: application/json" \
  -d '{"name":"测试知识库","description":"Phase 1 验证","ownerId":1,"visibility":"PRIVATE"}'
```

查询单个知识库：

```bash
curl http://localhost:8080/api/knowledge-bases/1
```

## 7. 当前还没有做什么

- 未实现 `document` 表。
- 未实现文档上传。
- 未实现文件存储。
- 未实现文档解析。
- 未实现 chunk 切分。
- 未实现 embedding。
- 未接入 Qdrant Java 客户端。
- 未实现向量检索。
- 未接入 LLM。
- 未实现 SSE。
- 未接入 Redis / Elasticsearch / RabbitMQ。
- 未实现 Spring Security / JWT。

## 8. 下一阶段 Phase 2 要做什么

Phase 2 建议从文档元数据基础开始：

- 新增 `document` 表。
- 定义文档状态枚举。
- 建立文档元数据 Entity、Mapper、Service。
- 提供文档元数据最小 API。
- 后续再进入文件上传、文档解析和 chunk。
