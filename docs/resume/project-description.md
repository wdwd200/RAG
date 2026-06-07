# Resume Project Description

## 1. 一句话项目描述

基于 Spring Boot 构建的 RAG 知识库后端项目，覆盖文档上传、解析切分、Embedding 向量化、Qdrant 检索、RAG 问答、SSE 流式响应、审计日志和检索评测。

## 2. 简历 Bullet 版本

- 基于 Spring Boot 构建 RAG 知识库后端系统，支持文档上传、解析切分、Embedding 向量化、Qdrant 检索、SSE 流式问答、引用溯源与日志审计。
- 设计文档处理状态机，覆盖 `UPLOADED`、`CHUNKED`、`EMBEDDING`、`INDEXING`、`INDEXED`、`FAILED` 等状态，并记录失败阶段和错误摘要。
- 抽象 EmbeddingClient、VectorStoreService 和 LlmClient，支持默认 mock provider 与可选 Qwen / DashScope provider，保证自动测试不依赖真实模型 key。
- 通过 requestId 串联 chat_message、retrieval_log、llm_call_log，实现一次问答链路的检索、模型调用和回答记录可追踪。
- 设计 evaluation_dataset / evaluation_question / evaluation_report 模块，支持 Recall@K、HitRate@K、MRR 评测和 bad case 分析。
- 明确 PostgreSQL 与 Qdrant 职责边界：关系库保存事实源和业务状态，Qdrant 仅作为向量检索索引，检索结果回查 active chunk。
- 补齐 Swagger、README、阶段文档、demo guide 和固定示例评测集，使项目具备可演示、可复现和可讲解能力。

## 3. 技术栈版本

```text
Java 17
Spring Boot 3.x
Maven
Spring Web
Spring Validation
Spring JDBC
MyBatis-Plus
Flyway
PostgreSQL
Qdrant
Springdoc OpenAPI
JUnit 5
H2 Test Database
Docker Compose
DashScope OpenAI-compatible API
```

## 4. 可量化能力描述

- 覆盖知识库、文档、chunk、检索、问答、审计、评测等多个后端模块。
- 自动测试覆盖 100+ cases，当前 `mvn test` 运行 130 个测试。
- 评测模块支持问题级结果保存和 report 聚合指标。
- 支持一次性 JSON 问答和 SSE 事件流问答两种输出方式。
- demo 文档覆盖 3 份固定示例文档和 10+ 条评测问题模板。

## 5. 不夸大的边界说明

第一版项目不是生产级 SaaS，目前没有实现：

- 登录、JWT 或权限系统。
- Redis 缓存、限流或分布式锁。
- Elasticsearch、BM25 或 Hybrid Search。
- Reranker。
- 异步文档处理队列。
- 公开数据集自动导入。
- LLM-as-judge 或答案质量评测。
- 复杂多租户计费、监控告警或运维体系。

面试中可以把这些作为后续增强方向说明，不要说成已经完成。
