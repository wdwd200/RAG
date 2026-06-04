# RAG 后端知识库

RAG 后端知识库是一个面向知识库管理和后续检索增强生成能力的 Spring Boot 后端项目。

当前阶段：Phase 1.1 项目初始化与通用基础设施骨架。

## 技术栈

- Java 17
- Spring Boot 3.x
- Maven
- Spring Web
- Spring Validation
- Spring Boot Actuator
- Springdoc OpenAPI / Swagger UI
- Lombok
- JUnit 5 / Spring Boot Test

## 本地启动

确认本地已安装 Java 17 和 Maven。

```bash
mvn test
mvn spring-boot:run
```

默认服务端口为 `8080`，可通过 `src/main/resources/application.yml` 中的 `server.port` 调整。

## 健康检查

统一 API 风格健康检查：

```bash
curl http://localhost:8080/api/health
```

返回结构示例：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "status": "UP",
    "service": "rag-backend"
  },
  "timestamp": "2026-06-04T12:00:00"
}
```

Actuator 健康检查：

```bash
curl http://localhost:8080/actuator/health
```

## Swagger

启动项目后访问：

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

健康检查接口 `/api/health` 可以在 Swagger 页面中看到。

## 当前已完成

- 初始化 Maven Spring Boot 项目。
- 配置 Java 17、Spring Boot 3.x 和本轮基础依赖。
- 新增统一响应结构 `ApiResponse`。
- 新增业务异常 `BusinessException`。
- 新增全局异常处理 `GlobalExceptionHandler`。
- 新增统一风格健康检查接口 `GET /api/health`。
- 配置 Springdoc OpenAPI / Swagger UI。
- 配置 Actuator 暴露 `health` 和 `info`。
- 建立 `common`、`config`、`health`、`knowledge`、`infrastructure` 基础包结构。

## 下一步计划

进入 Phase 1.2：补充 Docker Compose、数据库连接和 migration 基础。

本阶段尚未接入数据库、ORM、向量库、缓存、搜索引擎、LLM、登录鉴权或业务 CRUD。
