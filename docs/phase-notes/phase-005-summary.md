# Phase 5 Summary

## 1. Phase 5 完成内容

Phase 5 在 Phase 4 向量检索能力之上完成了 RAG 问答主链路：

- 新增 LLM Client 抽象和 Mock LLM。
- 新增 PromptBuilder 和受约束的 RAG prompt。
- 新增 chat_session、chat_message 及数据访问层。
- 新增一次性问答接口 `POST /api/chat/once`。
- 新增 requestId，并串联消息、检索日志和 LLM 调用日志。
- 新增 retrieval_log 和查询接口。
- 新增 QwenLlmClient，适配 DashScope OpenAI-compatible Chat Completions。
- 新增 llm_call_log 和查询接口。
- 新增 SSE 问答接口 `POST /api/chat/stream`。
- 完成 Mock 和 Qwen 两套端到端 RAG 问答验证。

Phase 5 完成后，系统可以从已索引知识库检索 active chunk，构造 RAG prompt，调用 Mock 或 Qwen LLM，并返回答案、引用和可查询的审计链路。

## 2. 当前目录结构

```text
chat
├── config
│   └── ChatStreamConfig
├── controller
│   └── ChatController
├── dto
│   ├── ChatOnceRequest
│   ├── ChatOnceResponse
│   ├── ChatReferenceResponse
│   └── ChatStreamEvent
├── entity
│   ├── ChatMessage
│   └── ChatSession
├── mapper
│   ├── ChatMessageMapper
│   └── ChatSessionMapper
├── prompt
│   ├── PromptBuilder
│   ├── PromptContextChunk
│   ├── RagPromptBuilder
│   └── RagPromptRequest
└── service
    ├── ChatMessageService
    ├── ChatProgressListener
    ├── ChatService
    ├── ChatSessionService
    ├── ChatStreamService
    └── impl

llm
├── client
│   ├── LlmClient
│   ├── MockLlmClient
│   └── QwenLlmClient
├── config
│   └── LlmProperties
├── model
│   ├── LlmRequest
│   └── LlmResponse
└── service
    ├── LlmService
    └── impl

audit
├── controller
│   ├── RetrievalLogController
│   └── LlmCallLogController
├── dto
├── entity
├── mapper
└── service

retrieval
├── controller
├── dto
└── service
```

## 3. /api/chat/once 主链路

```text
POST /api/chat/once
  ↓
ChatController
  ↓
ChatService
  ↓
创建或读取 ChatSession
  ↓
保存 USER ChatMessage
  ↓
RetrievalService
  ↓
EmbeddingService + VectorStoreService
  ↓
关系库回查 active chunk
  ↓
保存 retrieval_log
  ↓
PromptBuilder
  ↓
LlmService
  ↓
MockLlmClient / QwenLlmClient
  ↓
保存 llm_call_log
  ↓
保存 ASSISTANT ChatMessage
  ↓
返回 answer + references + requestId
```

ChatService 是一次性问答和共享问答工作流的编排位置。Controller 不包含检索、prompt、模型或日志细节。

## 4. /api/chat/stream 主链路

```text
POST /api/chat/stream
  ↓
ChatController
  ↓
ChatStreamService
  ↓
专用线程池执行 ChatService 共享问答工作流
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

ChatProgressListener 只暴露检索开始和检索完成两个进度点。完整答案返回后，ChatStreamService 将其拆成多个 `answer_delta` 事件。

当前 SSE 是应用层流式输出，不是真实模型 token streaming。真实 token streaming 需要后续扩展 LlmClient 和 LlmService 的流式接口。

## 5. requestId 审计链路

每次问答生成一个 UUID requestId。同一次成功问答中的以下记录使用同一个 requestId：

```text
USER chat_message
retrieval_log
llm_call_log
ASSISTANT chat_message
ChatOnceResponse 或 SSE events
```

查询接口：

```text
GET /api/audit/retrieval-logs/{requestId}
GET /api/audit/llm-call-logs/{requestId}
```

retrieval_log.messageId 和 llm_call_log.messageId 指向本次 USER message。LLM 失败日志使用独立事务保存，主问答事务回滚后仍可通过 requestId 排查。

## 6. Mock LLM 与 Qwen LLM

Mock LLM：

- provider 为 `mock`。
- 不需要 API key 或网络。
- 返回确定性测试答案。
- 适合自动化测试和本地流程验证。

Qwen LLM：

- provider 为 `qwen`。
- 使用 `QwenLlmClient` 调用 DashScope Chat Completions。
- 默认模型为 `qwen-plus`。
- 需要本地 `DASHSCOPE_API_KEY`。
- key 不进入代码、响应、审计日志或 Git。

两种实现都通过 LlmService 和 LlmClient 边界接入。ChatService、PromptBuilder 和 Controller 不感知具体 HTTP 供应商。

## 7. SSE 事件

成功事件顺序：

```text
retrieval_start
retrieval_result
answer_delta
references
done
```

异常事件：

```text
error
```

每个事件包含 requestId、eventType 和 data。`done` 明确表示结束；`error` 返回清晰错误信息，不返回 Java 堆栈。

## 8. retrieval_log

每条召回结果对应一条 retrieval_log，主要记录：

```text
requestId
sessionId
messageId
knowledgeBaseId
question
retrieverType
topK
chunkId
documentId
rankPosition
score
createdAt
```

rankPosition 从 1 开始。检索为空时不写占位记录。

## 9. llm_call_log

每次 LLM 调用记录：

```text
requestId
sessionId
messageId
knowledgeBaseId
provider
modelName
promptTokens
completionTokens
latencyMs
success
errorMessage
createdAt
```

当前 LlmResponse 未提供真实 usage，因此 promptTokens 和 completionTokens 保持为空。成功日志跟随主事务提交；失败日志使用新事务保存并继续抛出原异常。

## 10. Mock 验证

配置：

```text
APP_EMBEDDING_PROVIDER=mock
APP_EMBEDDING_DIMENSION=384
APP_LLM_PROVIDER=mock
APP_LLM_MODEL=mock-rag-assistant
QDRANT_COLLECTION_NAME=rag_chunks_phase5_6_mock_384
```

2026-06-06 实际验证结果：

- 健康检查、PostgreSQL 和 Qdrant 均为 UP。
- 文档从 UPLOADED 流转到 CHUNKED，再流转到 INDEXED。
- 1 个 active chunk 写入 Qdrant。
- retrieval/search 返回 1 个关系库 chunk。
- chat/once 返回 answer、1 个 reference 和 requestId。
- chat/stream 返回全部成功 SSE 事件。
- once 和 stream 均可查询到 retrieval_log 和成功 llm_call_log。

## 11. Qwen 验证

配置：

```text
APP_EMBEDDING_PROVIDER=qwen
APP_EMBEDDING_DIMENSION=1024
QWEN_EMBEDDING_MODEL=text-embedding-v4
APP_LLM_PROVIDER=qwen
APP_LLM_MODEL=qwen-plus
QDRANT_COLLECTION_NAME=rag_chunks_phase5_6_qwen_1024
```

2026-06-06 实际验证结果：

- text-embedding-v4 成功生成 1024 维向量。
- 文档成功处理并索引到独立 Qdrant collection。
- retrieval/search 返回 1 个目标 chunk。
- chat/once 返回非空答案、引用和 requestId。
- chat/stream 返回 retrieval_start、retrieval_result、answer_delta、references 和 done。
- once 和 stream 的 llm_call_log 均记录 provider=qwen、modelName=qwen-plus、success=true。
- 真实 API key 仅从本地 `.env` 读取，未写入文档或 Git。

## 12. 当前尚未实现

- 真实模型 token streaming。
- 评测数据集和自动评测任务。
- Agent 工作流。
- reranker。
- 复杂多轮记忆。
- Redis、Elasticsearch、RabbitMQ。
- 权限系统。
- 新的真实模型供应商。

## 13. Phase 6

下一阶段进入评测模块。Phase 6.1 建议先实现：

```text
evaluation_dataset
evaluation_question
评测集导入基础
```

Phase 6 的重点是建立可重复的 RAG 质量评测数据和执行基础，不是继续增加模型供应商或模型能力。
