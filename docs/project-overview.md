# Project Overview

## 1. 项目定位

RAG 后端知识库是一个 Spring Boot 后端项目，用于演示从知识库管理、文档上传、文档处理、向量检索到 RAG 问答和检索评测的完整主链路。

项目第一版定位是可演示、可复现、可讲解的工程样例，不是生产级 SaaS。

## 2. 核心能力

- 知识库 CRUD。
- 文档上传与本地 storage。
- 文档状态机：上传、处理、索引、失败记录。
- Markdown / txt 文档解析。
- 固定窗口 chunk 切分。
- Mock / Qwen embedding provider。
- Qdrant 向量索引和 knowledgeBaseId 过滤检索。
- 一次性 RAG 问答和 SSE 事件流问答。
- requestId 贯穿 chat_message、retrieval_log、llm_call_log。
- 检索评测集、评测问题、评测报告和 bad cases 分析。
- 可复现 Phase 6 demo 文档和操作指南。

## 3. 六阶段完成情况

```text
Phase 1: Spring Boot 项目骨架、统一响应、异常处理、健康检查、Swagger。
Phase 2: PostgreSQL、Flyway、MyBatis-Plus、知识库 CRUD、文档上传。
Phase 3: 文档解析、chunk 切分、处理状态机、active chunk 管理。
Phase 4: embedding 抽象、Qwen embedding、Qdrant、向量检索 API。
Phase 5: RAG 问答、PromptBuilder、LLM 抽象、Qwen LLM、SSE、审计日志。
Phase 6: evaluation dataset、Recall@K / HitRate@K / MRR、bad cases、demo 文档。
```

## 4. 总体架构

```text
Controller
  ↓
Service 编排层
  ↓
Mapper / Client / VectorStore
  ↓
PostgreSQL / Qdrant / DashScope-compatible API
```

主要边界：

- Controller 只做 HTTP 入口和参数接收。
- Service 负责业务编排。
- Mapper 负责关系库访问。
- EmbeddingClient / LlmClient 负责模型适配。
- VectorStoreService 负责向量库入口。
- Qdrant 只做向量索引，不作为最终事实源。

## 5. 核心调用链

```text
知识库
  ↓
上传文档
  ↓
process 生成 chunk
  ↓
index 写入 Qdrant
  ↓
retrieval/search 检索
  ↓
chat/once 或 chat/stream 问答
  ↓
requestId 查询 retrieval_log / llm_call_log
  ↓
evaluation 计算 Recall@K / HitRate@K / MRR
```

文档处理：

```text
POST /api/documents/{id}/process
  ↓
DocumentProcessingService
  ↓
DocumentParserRegistry
  ↓
TextSplitter
  ↓
DocumentChunkService
  ↓
document_chunk
```

向量检索：

```text
POST /api/retrieval/search
  ↓
RetrievalService
  ↓
EmbeddingService
  ↓
VectorStoreService.search
  ↓
Qdrant knowledgeBaseId filter
  ↓
DocumentChunkService 回查 active chunk
```

RAG 问答：

```text
POST /api/chat/once 或 POST /api/chat/stream
  ↓
ChatService
  ↓
RetrievalService
  ↓
PromptBuilder
  ↓
LlmService
  ↓
chat_message + retrieval_log + llm_call_log
```

评测：

```text
POST /api/evaluation/datasets/{datasetId}/run
  ↓
EvaluationRunService
  ↓
EvaluationQuestionService
  ↓
RetrievalService
  ↓
RetrievalMetricsCalculator
  ↓
EvaluationReportService
```

## 6. 核心数据表

- `knowledge_base`：知识库元数据。
- `document`：文档元数据、状态、处理版本、失败信息。
- `document_chunk`：文档片段，关系库事实源。
- `chat_session`：问答会话。
- `chat_message`：用户和助手消息，包含 requestId 和 references。
- `retrieval_log`：一次问答的检索命中日志。
- `llm_call_log`：一次问答的 LLM 调用日志。
- `evaluation_dataset`：评测集。
- `evaluation_question`：人工标注的评测问题。
- `evaluation_report`：一次评测运行报告。
- `evaluation_question_result`：问题级评测结果。

## 7. 关系库与 Qdrant 的职责边界

PostgreSQL 是事实源：

- 文档元数据。
- active chunk 内容。
- 处理状态。
- 聊天记录。
- 审计日志。
- 评测数据。

Qdrant 是检索索引：

- 存储 chunk 向量。
- 根据 query vector 搜索近邻。
- 使用 `knowledgeBaseId` payload filter 防止跨知识库检索。

检索结果必须根据 `chunkId` 回查关系库，最终返回的 `content` 来自 `document_chunk`，不是 Qdrant payload。

## 8. Mock Provider 与 Qwen Provider

Mock provider：

- 默认启用。
- 不需要 API key。
- 适合自动测试和本地流程验证。
- embedding 和 LLM 响应都是确定性或可预测的。

Qwen provider：

- 通过 DashScope OpenAI-compatible API 调用真实模型。
- 需要本地 `DASHSCOPE_API_KEY`。
- 真实 key 不提交到 Git。
- Qwen embedding 默认建议 1024 维，Qdrant collection vector size 必须匹配。

## 9. 如何本地运行

自动测试：

```bash
mvn test
```

自动测试使用 H2、mock provider 和固定测试数据，不依赖 Docker、Qdrant、真实 embedding 或真实 LLM。

真实链路：

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

## 10. 如何演示完整链路

推荐入口：

```text
docs/demo/final-demo-checklist.md
docs/demo/phase-006-demo-guide.md
```

最短演示路径：

```text
启动 Docker
  ↓
启动应用
  ↓
运行健康检查
  ↓
上传 demo 文档
  ↓
process + index
  ↓
retrieval/search
  ↓
chat/once 或 chat/stream
  ↓
查看 requestId 审计日志
  ↓
运行 evaluation demo
```
