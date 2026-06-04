# Round 005 Implementation Notes

## 1. 本轮定位

本轮执行 Phase 1.5：Phase 1 收尾、接口验证与代码导读整理。

本轮没有新增业务功能，只做知识库模块格式整理、Phase 1 总结文档、README 校准和接口验证。

## 2. 代码格式整理

本轮检查了以下文件：

- `KnowledgeBaseController.java`
- `KnowledgeBaseService.java`
- `KnowledgeBaseServiceImpl.java`
- `KnowledgeBaseCreateRequest.java`
- `KnowledgeBaseUpdateRequest.java`
- `KnowledgeBaseResponse.java`
- `KnowledgeBaseControllerTest.java`

实际整理内容：

- 收紧 `KnowledgeBaseCreateRequest` 字段之间的多余空行。
- 收紧 `KnowledgeBaseUpdateRequest` 字段之间的多余空行。
- 保持 Controller、Service 和测试的现有业务逻辑不变。

## 3. 接口验证

本轮已完成验证：

- `GET /api/health`：通过，返回 `success=true` 和服务状态 `UP`。
- `GET /api/health/database`：通过，返回 PostgreSQL 连通状态 `UP`。
- `GET /api/knowledge-bases`：通过，创建前返回空列表，创建后返回已创建知识库。
- `POST /api/knowledge-bases`：通过，成功创建 id 为 `1` 的知识库。
- `GET /api/knowledge-bases/{id}`：通过，能按 id 查询刚创建的知识库。

验证依赖本地 Docker、PostgreSQL 和打包后的应用：

```bash
docker compose up -d
mvn clean package
java -jar target/rag-backend-0.0.1-SNAPSHOT.jar
```

PowerShell 下执行 POST 验证时，使用 JSON 文件作为请求体，避免命令行引号转义破坏 JSON：

```bash
curl -X POST http://localhost:8080/api/knowledge-bases \
  -H "Content-Type: application/json" \
  --data-binary @target/phase15-create-request.json
```

## 4. Phase 1 当前可讲解的主线

Phase 1 的主线是从一个空后端项目，逐步形成可运行的知识库基础模块：

```text
项目骨架
  ↓
统一响应与异常处理
  ↓
数据库基础设施与 migration
  ↓
knowledge_base 表与持久化访问
  ↓
knowledge_base REST CRUD API
  ↓
Phase 1 收尾文档与验证
```

知识库 CRUD 调用链：

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

## 5. 本轮刻意不做

- 不做 `document` 表。
- 不做文档上传。
- 不做文件存储。
- 不做 chunk。
- 不做 embedding。
- 不做 Qdrant Java 客户端。
- 不做向量检索。
- 不做 LLM。
- 不做 SSE。
- 不做 Redis / Elasticsearch / RabbitMQ。
- 不做 Spring Security / JWT。

## 6. 下一轮建议

进入 Phase 2.1：`document` 表、文档状态枚举与文档元数据基础。
