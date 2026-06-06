# Round 023 Implementation Notes

## 1. 本轮定位

本轮执行 Phase 5.5：SSE 流式输出与 `llm_call_log`。

目标是在保持 `/api/chat/once` 可用的前提下，新增应用层 SSE 输出，并让同步和流式问答都记录 LLM provider、model、耗时、成功状态和错误摘要。

## 2. 本轮完成

- 新增 `llm_call_log` 表和 requestId、sessionId 索引。
- 新增 LlmCallLog Entity、Mapper、Service、DTO 和查询 Controller。
- 新增 `GET /api/audit/llm-call-logs/{requestId}`。
- `/api/chat/once` 成功和失败时都记录 LLM 调用日志。
- 新增 `POST /api/chat/stream`。
- 新增 `retrieval_start`、`retrieval_result`、`answer_delta`、`references`、`done` 和 `error` SSE 事件。
- once 与 stream 复用同一套事务化问答工作流。
- stream 成功时保存 USER / ASSISTANT message、retrieval_log 和 llm_call_log。
- 默认测试继续使用 Mock LLM，不依赖真实 API key。

## 3. 主要文件

```text
src/main/resources/db/migration/V8__create_llm_call_log_table.sql
src/main/java/com/example/ragbackend/audit/entity/LlmCallLog.java
src/main/java/com/example/ragbackend/audit/mapper/LlmCallLogMapper.java
src/main/java/com/example/ragbackend/audit/service/LlmCallLogService.java
src/main/java/com/example/ragbackend/audit/service/impl/LlmCallLogServiceImpl.java
src/main/java/com/example/ragbackend/audit/dto/LlmCallLogResponse.java
src/main/java/com/example/ragbackend/audit/controller/LlmCallLogController.java
src/main/java/com/example/ragbackend/chat/config/ChatStreamConfig.java
src/main/java/com/example/ragbackend/chat/dto/ChatStreamEvent.java
src/main/java/com/example/ragbackend/chat/service/ChatProgressListener.java
src/main/java/com/example/ragbackend/chat/service/ChatStreamService.java
src/main/java/com/example/ragbackend/chat/service/impl/ChatStreamServiceImpl.java
src/main/java/com/example/ragbackend/chat/service/ChatService.java
src/main/java/com/example/ragbackend/chat/service/impl/ChatServiceImpl.java
src/main/java/com/example/ragbackend/chat/controller/ChatController.java
src/test/java/com/example/ragbackend/chat/ChatControllerTest.java
src/test/java/com/example/ragbackend/chat/ChatLlmFailureTest.java
src/test/java/com/example/ragbackend/chat/ChatPersistenceTest.java
src/test/java/com/example/ragbackend/chat/ChatServiceTest.java
README.md
```

## 4. SSE 调用链

```text
POST /api/chat/stream
  ↓
ChatController
  ↓
ChatStreamService
  ↓
ChatService 共享问答工作流
  ↓
保存 USER chat_message
  ↓
RetrievalService
  ↓
保存 retrieval_log
  ↓
PromptBuilder
  ↓
LlmService
  ↓
保存 llm_call_log
  ↓
保存 ASSISTANT chat_message
  ↓
SseEmitter events
```

`ChatStreamService` 使用专用线程池执行问答，Controller 可以立即返回 `SseEmitter`。SSE 发送逻辑没有进入 RetrievalService、PromptBuilder 或 LlmClient。

## 5. 共享问答工作流

`ChatService.once(request)` 使用无操作进度监听器执行同步问答。

`ChatStreamService` 调用 `ChatService.execute(request, progressListener)`，只在两个业务节点接收进度：

```text
onRetrievalStart
onRetrievalResult
```

检索后的 prompt、LLM 调用、日志和消息持久化仍由 ChatService 统一编排。这样 once 与 stream 不会复制两套容易分叉的 RAG 主流程。

## 6. SSE 事件

每个事件的数据结构：

```text
requestId
eventType
data
```

成功事件顺序：

```text
retrieval_start
retrieval_result
answer_delta（一个或多个）
references
done
```

含义：

- `retrieval_start`：开始检索并返回 requestId。
- `retrieval_result`：返回检索得到的 active chunk 引用。
- `answer_delta`：返回回答文本片段。
- `references`：返回完整引用列表。
- `done`：返回完成标记及 sessionId、userMessageId、assistantMessageId。
- `error`：异常时返回清晰消息，不返回 Java 堆栈。

本轮 SSE 是应用层流式输出。`LlmService.complete()` 先返回完整答案，之后由 ChatStreamService 拆成多个 `answer_delta`。后续如需真实 token streaming，应扩展 LlmClient / LlmService 流式接口，不应把 Qwen HTTP 细节写入 ChatStreamService。

## 7. llm_call_log

字段：

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

`messageId` 指向本次 USER message。provider 和默认 model 来自 `LlmProperties`；成功响应存在 model 时优先记录响应 model。

当前 LlmResponse 没有 usage 字段，因此 `promptTokens` 和 `completionTokens` 保持为空，不做不准确估算。

成功日志使用问答主事务。LLM 调用或响应校验失败时，失败日志使用 `REQUIRES_NEW` 独立事务保存，再继续抛出原异常。这样主问答事务可以回滚，失败调用仍可通过 requestId 排查。

## 8. requestId 串联

一次成功问答使用同一个 UUID requestId 串联：

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

失败日志也保留 requestId。由于主问答事务会回滚，失败记录中的 sessionId / messageId 是本次尝试生成的标识，失败排查应以 requestId 为主。

## 9. 职责边界

- `ChatController`：HTTP / SSE 入口和参数校验。
- `ChatStreamService`：异步执行共享问答工作流并发送 SSE。
- `ChatService`：once 与 stream 共用的 RAG 主流程、日志组装和事务边界。
- `RetrievalService`：只负责检索，不发送 SSE、不写审计日志。
- `PromptBuilder`：只负责构造 prompt。
- `LlmService`：LLM 业务调用入口。
- `LlmCallLogService`：只负责 llm_call_log 保存和查询。
- `RetrievalLogService`：只负责 retrieval_log 保存和查询。

## 10. 测试覆盖

默认测试配置：

```text
app.llm.provider=mock
app.llm.api-key=
```

覆盖内容：

- V8 migration 可执行，`llm_call_log` 表存在。
- `/api/chat/once` 成功时写入成功日志。
- 成功日志可通过 requestId 查询。
- LLM 调用失败时写入失败日志并继续返回原业务错误。
- 主问答事务回滚后失败日志仍保留。
- `/api/chat/stream` 返回 `text/event-stream`。
- SSE 包含 retrieval_start、retrieval_result、answer_delta、references 和 done。
- SSE 事件顺序正确且每个事件包含 requestId。
- LLM 失败时 stream 发送 error 事件且不暴露 Java 堆栈。
- stream 保存两条 chat_message。
- stream 写入 retrieval_log 和 llm_call_log。
- stream 参数校验继续使用统一错误响应。

## 11. 运行与验证

```bash
mvn test
```

启动本地 PostgreSQL、Qdrant 和应用后：

```bash
curl -N -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"knowledgeBaseId":1,"question":"这份文档讲了什么？","topK":5}'
```

从事件中取得 requestId 后：

```bash
curl http://localhost:8080/api/audit/retrieval-logs/{requestId}
curl http://localhost:8080/api/audit/llm-call-logs/{requestId}
```

## 12. 本轮刻意不做

- 不实现真实模型 token streaming。
- 不实现 OpenAI-compatible GPT-5.5 通用 Client。
- 不做 Agent 工作流或 reranker。
- 不引入 Redis、Elasticsearch、RabbitMQ。
- 不做复杂多轮记忆。
- 不做权限系统。
- 不提交真实 API key。

## 13. 下一轮建议

进入 Phase 5.6：Phase 5 收尾、问答链路验证与导读整理。
