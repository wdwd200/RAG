# RAG 后端知识库

RAG 后端知识库是一个基于 Spring Boot 的后端项目，覆盖知识库管理、文档上传、文档解析切分、Embedding 向量化、Qdrant 检索、RAG 问答、SSE 流式响应、requestId 审计日志和检索评测。

当前状态：第一版 6 阶段已完成。项目已具备可演示、可复现、可讲解和可投简历的基础状态。

## 核心功能

- 知识库 CRUD。
- 文档上传、本地 storage 和文档状态机。
- Markdown / txt 文档解析与固定窗口 chunk 切分。
- Mock embedding 和 Qwen / DashScope embedding。
- Qdrant 向量索引与 `knowledgeBaseId` 过滤检索。
- 一次性 RAG 问答和 SSE 流式问答。
- requestId 串联 `chat_message`、`retrieval_log`、`llm_call_log`。
- Evaluation dataset / question / report / question result。
- Recall@K、HitRate@K、MRR 和 bad cases 分析。
- 固定示例文档、评测问题模板和 Phase 6 demo guide。

## 技术栈

- Java 17
- Spring Boot 3.x
- Maven
- Spring Web / Validation / Actuator
- Spring JDBC
- MyBatis-Plus
- Flyway
- PostgreSQL
- Qdrant
- H2 Test Database
- Springdoc OpenAPI / Swagger UI
- JUnit 5
- Docker Compose
- DashScope OpenAI-compatible API

## 快速启动

自动测试只需要 Java 17 和 Maven：

```bash
mvn test
```

自动测试使用 H2、mock provider 和固定测试数据，不依赖 Docker、Qdrant、真实 embedding 或真实 LLM。

真实链路运行需要 PostgreSQL 和 Qdrant：

```bash
docker compose up -d
mvn clean package
java -jar target/rag-backend-0.0.1-SNAPSHOT.jar
```

健康检查：

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/api/health/database
curl http://localhost:8080/api/health/qdrant
```

Swagger：

```text
http://localhost:8080/swagger-ui/index.html
```

## Mock 模式

默认配置使用 mock provider：

```text
APP_EMBEDDING_PROVIDER=mock
APP_EMBEDDING_DIMENSION=384
APP_LLM_PROVIDER=mock
```

Mock 模式不需要 API key，适合自动测试、本地流程验证和 demo 主链路演示。mock embedding 是确定性的，mock LLM 返回可预测答案，但它不代表真实语义检索或真实模型回答质量。

## Qwen 模式

切换到 Qwen / DashScope 时，真实 key 只允许放在本地 `.env`、环境变量或 IDEA Run Configuration 中，不提交到 Git。

示例配置见：

```text
.env.qwen.example
```

常用配置：

```text
APP_EMBEDDING_PROVIDER=qwen
APP_EMBEDDING_DIMENSION=1024
QWEN_EMBEDDING_MODEL=text-embedding-v4
APP_LLM_PROVIDER=qwen
APP_LLM_MODEL=qwen-plus
DASHSCOPE_API_KEY=replace-with-your-local-key
```

注意：Qdrant collection vector size 必须和 embedding dimension 一致。mock 384 维和 Qwen 1024 维建议使用不同 `QDRANT_COLLECTION_NAME`。

## 核心 API 入口

详细接口索引见 [docs/api-index.md](docs/api-index.md)。

常用主链路接口：

- `GET /api/health`
- `POST /api/knowledge-bases`
- `POST /api/documents/upload`
- `POST /api/documents/{id}/process`
- `POST /api/documents/{id}/index`
- `GET /api/documents/{documentId}/chunks`
- `POST /api/retrieval/search`
- `POST /api/chat/once`
- `POST /api/chat/stream`
- `GET /api/audit/retrieval-logs/{requestId}`
- `GET /api/audit/llm-call-logs/{requestId}`
- `POST /api/evaluation/datasets/{datasetId}/run`
- `GET /api/evaluation/reports/{reportId}/summary`
- `GET /api/evaluation/reports/{reportId}/bad-cases`

## Demo 入口

最终演示清单：

```text
docs/demo/final-demo-checklist.md
```

Phase 6 evaluation demo：

```text
docs/demo/phase-006-demo-guide.md
```

示例文档：

```text
docs/demo/sample-documents/
```

评测问题模板：

```text
docs/demo/sample-evaluation-questions.json
```

第一版评测集的 `relevantChunkIds` 需要人工根据 `GET /api/documents/{documentId}/chunks` 返回的 active chunks 选择。`chunkId` 是数据库运行时生成的，不同数据库和不同演示轮次可能不同。

可选辅助脚本：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/demo/phase6-demo.ps1
```

脚本会自动创建知识库、上传示例文档、process、index 并打印 chunks，然后停在人工标注阶段。

## 项目文档

- 项目总览：[docs/project-overview.md](docs/project-overview.md)
- API 索引：[docs/api-index.md](docs/api-index.md)
- Phase 6 总结：[docs/phase-notes/phase-006-summary.md](docs/phase-notes/phase-006-summary.md)
- 最终演示清单：[docs/demo/final-demo-checklist.md](docs/demo/final-demo-checklist.md)
- Evaluation demo guide：[docs/demo/phase-006-demo-guide.md](docs/demo/phase-006-demo-guide.md)
- 示例评测集说明：[docs/demo/sample-evaluation-dataset.md](docs/demo/sample-evaluation-dataset.md)
- 示例报告说明：[docs/demo/sample-evaluation-report.md](docs/demo/sample-evaluation-report.md)
- 简历项目描述：[docs/resume/project-description.md](docs/resume/project-description.md)
- 面试讲解要点：[docs/resume/interview-talking-points.md](docs/resume/interview-talking-points.md)
- 后续增强规划：[docs/roadmap/optional-enhancements.md](docs/roadmap/optional-enhancements.md)

## 阶段总结

- Phase 1：项目骨架、统一响应、异常处理、健康检查、Swagger。
- Phase 2：PostgreSQL、Flyway、MyBatis-Plus、知识库 CRUD、文档上传。
- Phase 3：文档解析、chunk 切分、处理状态机、active chunk。
- Phase 4：embedding 抽象、Qwen embedding、Qdrant、向量检索。
- Phase 5：RAG 问答、PromptBuilder、LLM 抽象、Qwen LLM、SSE、审计日志。
- Phase 6：检索评测、Recall@K / HitRate@K / MRR、bad cases、可复现 demo。

完整阶段文档位于：

```text
docs/phase-notes/
docs/round-notes/
```

## 后续增强方向

第一版未实现 Redis、Elasticsearch、Hybrid Search、reranker、公开数据集自动导入、LLM-as-judge 和权限系统。

后续可选增强见：

```text
docs/roadmap/optional-enhancements.md
```
