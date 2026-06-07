# Round 024 Implementation Notes

## 1. 本轮定位

本轮执行 Phase 5.6，是 Phase 5 的收尾轮次。

本轮没有新增业务接口或大功能，重点是实际验证完整 RAG 问答链路、检查代码职责边界，并补齐 Phase 5 总结和导读文档。

## 2. 本轮完成

- 启动并确认 PostgreSQL、Qdrant 正常运行。
- 执行 `mvn clean package` 并启动当前应用。
- 完成 Mock Embedding + Mock LLM 全链路验证。
- 完成 Qwen Embedding + Qwen LLM 全链路验证。
- 检查 once、stream、retrieval_log 和 llm_call_log 的 requestId 串联。
- 检查 Chat、Retrieval、Prompt、LLM、Audit 和 Controller 职责边界。
- 新增 `docs/phase-notes/phase-005-summary.md`。
- 更新 README。
- 新增本实现说明。

## 3. 验证接口

本轮实际调用：

```text
GET  /api/health
GET  /api/health/database
GET  /api/health/qdrant
POST /api/knowledge-bases
POST /api/documents/upload
POST /api/documents/{id}/process
POST /api/documents/{id}/index
POST /api/retrieval/search
POST /api/chat/once
POST /api/chat/stream
GET  /api/audit/retrieval-logs/{requestId}
GET  /api/audit/llm-call-logs/{requestId}
```

应用启动时 Flyway 将本地 PostgreSQL 从 V4 正常迁移到 V8。

## 4. Mock 链路结果

配置：

```text
Embedding provider: mock
Vector dimension: 384
Qdrant collection: rag_chunks_phase5_6_mock_384
LLM provider: mock
LLM model: mock-rag-assistant
```

结果：

- 三个健康检查均通过。
- 文档上传状态为 UPLOADED。
- process 后状态为 CHUNKED，生成 1 个 chunk。
- index 后状态为 INDEXED。
- retrieval/search 返回 1 个目标 chunk。
- chat/once 返回非空 answer、1 个 reference 和 requestId。
- chat/stream HTTP 200，Content-Type 为 text/event-stream。
- stream 包含 retrieval_start、retrieval_result、answer_delta、references、done。
- once 和 stream 均写入 1 条 retrieval_log。
- once 和 stream 均写入 1 条成功 llm_call_log。

## 5. Qwen 链路结果

配置：

```text
Embedding provider: qwen
Embedding model: text-embedding-v4
Vector dimension: 1024
Qdrant collection: rag_chunks_phase5_6_qwen_1024
LLM provider: qwen
LLM model: qwen-plus
```

结果：

- upload、process 和 index 通过。
- text-embedding-v4 成功生成并写入 1024 维向量。
- retrieval/search 返回 1 个目标 chunk。
- chat/once 返回非空答案、引用和 requestId。
- chat/stream 返回全部成功事件。
- once 与 stream 的 LLM 日志均为 provider=qwen、modelName=qwen-plus、success=true。
- 真实 key 只从本地 `.env` 注入，没有输出或写入仓库。

## 6. /api/chat/once

```text
ChatController
  ↓
ChatService
  ↓
创建或读取 session，保存 USER message
  ↓
RetrievalService
  ↓
保存 retrieval_log
  ↓
PromptBuilder
  ↓
LlmService
  ↓
MockLlmClient / QwenLlmClient
  ↓
保存 llm_call_log 和 ASSISTANT message
  ↓
返回 answer + references + requestId
```

## 7. /api/chat/stream

```text
ChatController
  ↓
ChatStreamService
  ↓
ChatService 共享问答工作流
  ↓
RetrievalService
  ↓
PromptBuilder
  ↓
LlmService
  ↓
保存 chat_message / retrieval_log / llm_call_log
  ↓
SseEmitter events
```

stream 没有复制第二套问答业务流程。ChatStreamService 负责异步执行和 SSE 发送，ChatService 仍负责编排实际 RAG 问答。

## 8. requestId 串联

同一个 requestId 串联：

```text
USER chat_message
retrieval_log
llm_call_log
ASSISTANT chat_message
JSON response 或 SSE events
```

本轮实际使用 once 和 stream 返回的 requestId 查询两类日志，均返回对应成功记录。

## 9. 逻辑可读性检查

检查结果：

- ChatServiceImpl 仍负责 once 和共享问答主流程。
- ChatStreamServiceImpl 只负责 stream 异步编排和 SSE 事件发送。
- RetrievalServiceImpl 只负责 query embedding、向量检索和关系库 chunk 回查。
- RagPromptBuilder 只负责 prompt 构造。
- LlmServiceImpl 只负责 LLM 请求校验、默认参数和 LlmClient 调用。
- QwenLlmClient 只负责 DashScope HTTP 适配。
- RetrievalLogServiceImpl 只负责 retrieval_log 数据访问。
- LlmCallLogServiceImpl 只负责 llm_call_log 数据访问和失败日志事务边界。
- ChatController、RetrievalController 和 Audit Controller 保持薄。

未发现需要轻量改名或重构的问题，本轮没有修改生产业务代码。

## 10. 测试

`mvn clean package` 已通过，自动测试仍使用 Mock LLM 和 H2，不依赖真实 Qwen key。

提交前还会再次执行：

```text
mvn test
git diff --check
```

## 11. 本轮刻意不做

- 不实现评测模块。
- 不实现真实 token streaming。
- 不实现 Agent 或 reranker。
- 不引入 Redis、Elasticsearch、RabbitMQ。
- 不增加新的真实模型供应商。

## 12. 下一轮

进入 Phase 6.1：`evaluation_dataset` / `evaluation_question` 表与评测集导入基础。
