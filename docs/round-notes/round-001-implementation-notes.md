# Round 001 Implementation Notes

## 1. 本轮完成了什么

本轮执行 Phase 1.1：项目初始化与通用基础设施骨架。

已将空仓库初始化为一个可启动的 Spring Boot Maven 项目，并补齐后续开发会复用的基础能力：统一响应、全局异常处理、参数校验异常处理、健康检查、Swagger/OpenAPI、Actuator 基础暴露、基础配置、README 和本轮实现说明。

本轮没有接入数据库，也没有实现知识库业务 CRUD。

## 2. 新增或修改了哪些文件

- `pom.xml`
- `.gitignore`
- `.env.example`
- `README.md`
- `src/main/resources/application.yml`
- `src/main/java/com/example/ragbackend/RagBackendApplication.java`
- `src/main/java/com/example/ragbackend/common/response/ApiResponse.java`
- `src/main/java/com/example/ragbackend/common/exception/BusinessException.java`
- `src/main/java/com/example/ragbackend/common/exception/GlobalExceptionHandler.java`
- `src/main/java/com/example/ragbackend/config/OpenApiConfig.java`
- `src/main/java/com/example/ragbackend/health/HealthController.java`
- `src/main/java/com/example/ragbackend/knowledge/.gitkeep`
- `src/main/java/com/example/ragbackend/infrastructure/.gitkeep`
- `src/test/java/com/example/ragbackend/RagBackendApplicationTests.java`
- `docs/round-notes/round-001-implementation-notes.md`

## 3. 每个核心文件的职责

`pom.xml`：定义 Maven 项目、Java 17、Spring Boot 3.x，以及本轮允许的基础依赖。

`application.yml`：定义应用名、端口、Swagger/OpenAPI 配置和 Actuator 暴露范围。

`RagBackendApplication.java`：Spring Boot 应用启动入口。

`ApiResponse.java`：统一接口响应结构，所有业务 Controller 应返回该结构。

`BusinessException.java`：业务异常基础类，用于后续业务模块主动抛出可预期错误。

`GlobalExceptionHandler.java`：统一处理业务异常、参数校验异常和兜底异常，避免直接向前端暴露 Java 堆栈。

`OpenApiConfig.java`：配置 OpenAPI 文档标题、描述和版本。

`HealthController.java`：提供统一 API 风格的健康检查入口 `GET /api/health`。

`RagBackendApplicationTests.java`：基础上下文启动测试，保证 Spring Boot 应用能被测试环境加载。

## 4. 本轮功能入口在哪里

本轮只有一个自定义业务接口：

```text
GET /api/health
```

启动后可访问：

```bash
curl http://localhost:8080/api/health
```

Swagger UI：

```text
http://localhost:8080/swagger-ui.html
```

Actuator health：

```text
http://localhost:8080/actuator/health
```

## 5. 请求进入系统后的核心调用链

`GET /api/health` 的调用链：

```text
HTTP Request
  -> Spring MVC DispatcherServlet
  -> HealthController.health()
  -> ApiResponse.success(data)
  -> JSON Response
```

异常情况下的调用链：

```text
Controller / Validation / Service
  -> 抛出异常
  -> GlobalExceptionHandler
  -> ApiResponse.error(code, message)
  -> JSON Response
```

## 6. 新增配置项

`spring.application.name`：应用名称，当前为 `rag-backend`。

`server.port`：服务端口，默认 `8080`。

`springdoc.api-docs.path`：OpenAPI JSON 地址，当前为 `/v3/api-docs`。

`springdoc.swagger-ui.path`：Swagger UI 地址，当前为 `/swagger-ui.html`。

`springdoc.packages-to-scan`：Swagger 扫描基础包，当前为 `com.example.ragbackend`。

`management.endpoints.web.exposure.include`：Actuator 暴露范围，当前只暴露 `health,info`。

`management.endpoint.health.show-details`：Actuator health 详情展示策略，当前为 `never`。

## 7. 为什么采用当前设计

包名采用 `com.example.ragbackend`，符合本轮建议的基础包名，并且能支撑后续按模块扩展。

`common` 下放统一响应和异常处理，是因为这些能力会被后续知识库、文档、检索、问答等模块复用。

`health` 单独成包，是为了让健康检查入口和后续业务模块保持边界清晰。

`knowledge` 和 `infrastructure` 本轮只创建目录，不提前实现业务和外部依赖，避免越过 Phase 1.1 的范围。

Swagger 使用 `springdoc-openapi-starter-webmvc-ui`，与 Spring Boot 3.x 和 Spring MVC 项目匹配。

## 8. 如何运行和验证

运行测试：

```bash
mvn test
```

启动服务：

```bash
mvn spring-boot:run
```

验证健康检查：

```bash
curl http://localhost:8080/api/health
```

验证 Swagger：

```text
http://localhost:8080/swagger-ui.html
```

验证 Actuator：

```bash
curl http://localhost:8080/actuator/health
```

## 9. 哪些细节暂时不用深究

- 数据库连接、连接池、migration、SQL 初始化都留到 Phase 1.2。
- `knowledge_base` 表、实体、Mapper、Repository 和 CRUD API 都留到 Phase 1.3 到 Phase 1.4。
- 文档上传、解析、chunk 切分、embedding、向量库、LLM、SSE、Redis、Elasticsearch 和登录鉴权都不是本轮范围。
- 当前 `BusinessException` 只保留最小字段，后续可以根据业务错误码体系再扩展。
- 当前 `ApiResponse` 只提供最小静态工厂方法，后续若有分页、链路追踪 ID 等需求再扩展。

## 10. 下一轮建议

下一轮进入 Phase 1.2：Docker Compose + 数据库连接 + migration 基础。

建议下一轮只补充数据库与本地基础设施能力，包括 Docker Compose、数据库连接配置、migration 工具和基础连通性验证，不要提前实现 `knowledge_base` CRUD。
