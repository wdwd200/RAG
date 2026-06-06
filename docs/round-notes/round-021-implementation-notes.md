# Round 021 Implementation Notes

## 1. 本轮定位

本轮执行 Phase 5.3：检索日志、requestId 链路追踪与问答可观测性。

目标是让 `/api/chat/once` 返回一次请求级 requestId，并能够通过该 requestId 查询本次问答实际召回的 chunk、分数和排序。

## 2. 本轮完成

- 新增 `retrieval_log` 表。
- 为 `request_id`、`session_id` 和 `knowledge_base_id` 建立索引。
- 给 `chat_message` 增加 `request_id` 字段和索引。
- 新增 RetrievalLog Entity、Mapper、Service 和 Response DTO。
- 新增检索日志查询 Controller。
- ChatService 在问答开始时生成 UUID requestId。
- USER message、retrieval_log、ASSISTANT message 和 ChatOnceResponse 使用同一个 requestId。
- ChatService 按检索结果顺序写入 retrieval_log。
- 新增 `GET /api/audit/retrieval-logs/{requestId}`。
- 新增 migration、requestId、日志排序和查询相关测试。
- 更新 README。

## 3. 主要文件

```text
src/main/resources/db/migration/V6__create_retrieval_log_table.sql
src/main/resources/db/migration/V7__add_request_id_to_chat_message.sql
src/main/java/com/example/ragbackend/audit/entity/RetrievalLog.java
src/main/java/com/example/ragbackend/audit/dto/RetrievalLogResponse.java
src/main/java/com/example/ragbackend/audit/mapper/RetrievalLogMapper.java
src/main/java/com/example/ragbackend/audit/service/RetrievalLogService.java
src/main/java/com/example/ragbackend/audit/service/impl/RetrievalLogServiceImpl.java
src/main/java/com/example/ragbackend/audit/controller/RetrievalLogController.java
src/main/java/com/example/ragbackend/chat/entity/ChatMessage.java
src/main/java/com/example/ragbackend/chat/dto/ChatOnceResponse.java
src/main/java/com/example/ragbackend/chat/service/ChatMessageService.java
src/main/java/com/example/ragbackend/chat/service/impl/ChatMessageServiceImpl.java
src/main/java/com/example/ragbackend/chat/service/impl/ChatServiceImpl.java
src/main/java/com/example/ragbackend/config/MybatisPlusConfig.java
src/test/java/com/example/ragbackend/chat/ChatControllerTest.java
src/test/java/com/example/ragbackend/chat/ChatPersistenceTest.java
src/test/java/com/example/ragbackend/chat/ChatServiceTest.java
README.md
```

## 4. requestId 链路

```text
POST /api/chat/once
  ↓
ChatService 校验请求
  ↓
生成 UUID requestId
  ↓
保存 USER chat_message
  ↓
RetrievalService 检索
  ↓
保存 retrieval_log 多条记录
  ↓
PromptBuilder
  ↓
LlmService / MockLlmClient
  ↓
保存 ASSISTANT chat_message
  ↓
ChatOnceResponse.requestId
```

同一次成功问答中：

- USER message 的 requestId 与响应一致。
- 每条 retrieval_log 的 requestId 与响应一致。
- ASSISTANT message 的 requestId 与响应一致。
- retrieval_log.messageId 指向 USER message。

## 5. retrieval_log 记录内容

每条检索结果对应一条日志：

```text
requestId
sessionId
messageId
knowledgeBaseId
question
retrieverType = VECTOR
topK
chunkId
documentId
rankPosition
score
createdAt
```

`rankPosition` 根据 RetrievalService 返回顺序生成，从 1 开始。

检索结果为空时，本轮不写占位日志。ChatService 仍会使用空上下文构造 prompt 并返回 Mock LLM answer；使用该 requestId 查询日志时返回空列表。

## 6. 查询接口

```text
GET /api/audit/retrieval-logs/{requestId}
```

示例：

```bash
curl http://localhost:8080/api/audit/retrieval-logs/3f5cb685-84c6-4bbf-a2c8-a8fa8b801404
```

响应使用统一 ApiResponse，日志按 `rankPosition` 升序、id 升序返回。

该接口只用于开发排查，本轮不做分页或权限系统。

## 7. 职责边界

`ChatService`：

- 仍是问答主流程编排位置。
- 生成 requestId。
- 组装并提交本次问答的 retrieval logs。

`RetrievalService`：

- 只负责 question embedding、向量检索和关系库 active chunk 回查。
- 不依赖 RetrievalLogService，不写日志。

`RetrievalLogService`：

- 接收一次检索结果对应的日志列表并持久化。
- 按 requestId 查询并排序。
- 不负责检索或问答编排。

`ChatMessageService`：

- 只负责 chat_message 数据访问。
- 接收并保存 ChatService 传入的 requestId。

`RetrievalLogController`：

- 只提供 HTTP 查询入口并返回统一 ApiResponse。
- 不组装或写入日志。

## 8. 事务规则

`ChatService.once` 保持事务边界。USER message、retrieval logs 和 ASSISTANT message 属于同一次成功问答事务。

如果后续 prompt 或 LLM 流程失败，本次事务会回滚，避免只留下部分问答数据。参数校验在 requestId 生成和数据写入前执行，空 question 不会产生 chat message 或 retrieval_log。

## 9. 测试覆盖

测试默认使用 Mock LLM 和 mock RetrievalService，不依赖真实 LLM、Qwen 或 Docker。

覆盖内容：

- V6/V7 migration 可执行。
- `retrieval_log` 表存在。
- `chat_message.request_id` 字段存在。
- `/api/chat/once` 响应包含 requestId。
- USER message 写入 requestId。
- ASSISTANT message 写入相同 requestId。
- 检索结果写入 retrieval_log。
- rankPosition 从 1 开始并按顺序递增。
- 通过 requestId 查询日志并按 rankPosition 升序返回。
- 日志关联 sessionId、USER messageId 和 knowledgeBaseId。
- question 为空时不产生脏日志。
- ChatService 调用 RetrievalService、PromptBuilder、LlmService 和 RetrievalLogService。

RetrievalService 未增加任何日志依赖，仍只负责检索。

## 10. 运行与验证

```bash
mvn test
docker compose up -d
mvn spring-boot:run
```

先调用：

```bash
curl -X POST http://localhost:8080/api/chat/once \
  -H "Content-Type: application/json" \
  -d '{"knowledgeBaseId":1,"question":"这份文档讲了什么？","topK":5}'
```

从响应中获取 requestId，再调用：

```bash
curl http://localhost:8080/api/audit/retrieval-logs/{requestId}
```

## 11. 本轮刻意不做

- 不接真实 LLM。
- 不做 SSE 或 `/api/chat/stream`。
- 不做 llm_call_log。
- 不做 token 统计。
- 不做复杂多轮记忆。
- 不做 Agent 工作流或 reranker。
- 不引入 Redis、Elasticsearch 或 RabbitMQ。
- 不做权限系统。

## 12. 下一轮建议

进入 Phase 5.4：真实 LLM Client 与模型配置模板。
