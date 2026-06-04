# Round 004 Implementation Notes

## 1. 本轮完成了什么

本轮执行 Phase 1.4：`knowledge_base` 最小 CRUD API。

已基于上一轮完成的 `knowledge_base` 表、Entity、Mapper 和 Service，新增面向 HTTP 的 REST API，使知识库可以通过接口创建、查询、更新和删除。

本轮没有实现文档上传、文档解析、chunk、embedding、向量检索、LLM、SSE 或鉴权。

## 2. 新增或修改文件

- `src/main/java/com/example/ragbackend/knowledge/controller/KnowledgeBaseController.java`
- `src/main/java/com/example/ragbackend/knowledge/dto/KnowledgeBaseUpdateRequest.java`
- `src/main/java/com/example/ragbackend/knowledge/service/KnowledgeBaseService.java`
- `src/main/java/com/example/ragbackend/knowledge/service/impl/KnowledgeBaseServiceImpl.java`
- `src/test/java/com/example/ragbackend/knowledge/KnowledgeBaseControllerTest.java`
- `README.md`
- `docs/round-notes/round-004-implementation-notes.md`

## 3. 核心文件职责

`KnowledgeBaseController.java`：提供知识库 REST CRUD API，负责接收 HTTP 请求、触发参数校验并返回统一 `ApiResponse`。

`KnowledgeBaseUpdateRequest.java`：承载更新知识库时的请求参数，包含 `name`、`description` 和 `visibility`。

`KnowledgeBaseService.java`：补齐 `getById`、`update` 和 `deleteById` 能力，作为 Controller 和持久化层之间的业务接口。

`KnowledgeBaseServiceImpl.java`：实现更新、删除和不存在数据时的业务异常处理。

`KnowledgeBaseControllerTest.java`：使用 MockMvc 覆盖知识库创建、列表查询、单条查询、更新、删除和查询不存在 ID 的错误响应。

## 4. 本轮功能入口

接口入口：

```text
POST   /api/knowledge-bases
GET    /api/knowledge-bases
GET    /api/knowledge-bases/{id}
PUT    /api/knowledge-bases/{id}
DELETE /api/knowledge-bases/{id}
```

Swagger UI：

```text
http://localhost:8080/swagger-ui/index.html
```

## 5. 请求进入系统后的核心调用链

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

当知识库不存在时，Service 抛出 `BusinessException`，再由 `GlobalExceptionHandler` 转换为统一 `ApiResponse` 错误结构。

## 6. 新增配置项

本轮没有新增配置项。

## 7. 为什么采用当前设计

Controller 保持薄，只负责 HTTP 参数接收、校验和统一响应包装。

业务判断放在 Service 中，尤其是“数据不存在不能静默成功”的规则，便于后续其他入口复用。

DTO 与 Entity 分离，避免 REST API 直接暴露数据库对象。

## 8. 如何运行和验证

运行测试：

```bash
mvn test
```

启动应用时，优先使用：

```bash
mvn clean package
java -jar target/rag-backend-0.0.1-SNAPSHOT.jar
```

创建知识库：

```bash
curl -X POST http://localhost:8080/api/knowledge-bases \
  -H "Content-Type: application/json" \
  -d '{"name":"默认知识库","description":"用于本地验证","ownerId":1,"visibility":"PRIVATE"}'
```

查询列表：

```bash
curl http://localhost:8080/api/knowledge-bases
```

## 9. 暂时不用深究的细节

- `visibility` 第一版仍是普通字符串，没有实现权限模型。
- `ownerId` 仍使用简单字段，没有接入用户体系。
- 删除接口当前是物理删除，没有做软删除。
- 错误码目前保持简单，后续可以统一整理错误码枚举。

## 10. 下一轮建议

进入 Phase 1.5：Phase 1 收尾、接口验证与代码导读整理。
