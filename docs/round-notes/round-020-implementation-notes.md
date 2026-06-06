# Round 020 Implementation Notes

## 1. 本轮定位

本轮执行 Phase 5.2：chat 基础表与 `/api/chat/once` JSON 闭环。

目标是完成一次普通 RAG 问答：保存用户问题，检索知识库 chunk，构造 RAG prompt，调用 Mock LLM，保存助手回答及引用，并返回一次性 JSON 响应。

## 2. 本轮完成

- 新增 `chat_session` 表和 `knowledge_base_id` 索引。
- 新增 `chat_message` 表、角色约束和 `session_id` 索引。
- 新增 ChatSession、ChatMessage、Mapper 和基础数据访问 Service。
- 新增 ChatService 作为 RAG 问答主流程编排入口。
- 新增 `POST /api/chat/once`。
- 支持新建会话或复用已有会话。
- 保存 USER 和 ASSISTANT 两条消息。
- 将回答引用序列化到 assistant message 的 `references_json`。
- 返回 answer 和 references。
- 新增 migration、持久化、Service 编排和 Controller 闭环测试。
- 更新 README。

## 3. 主要文件

```text
src/main/resources/db/migration/V5__create_chat_tables.sql
src/main/java/com/example/ragbackend/config/MybatisPlusConfig.java
src/main/java/com/example/ragbackend/chat/controller/ChatController.java
src/main/java/com/example/ragbackend/chat/dto/ChatOnceRequest.java
src/main/java/com/example/ragbackend/chat/dto/ChatOnceResponse.java
src/main/java/com/example/ragbackend/chat/dto/ChatReferenceResponse.java
src/main/java/com/example/ragbackend/chat/entity/ChatSession.java
src/main/java/com/example/ragbackend/chat/entity/ChatMessage.java
src/main/java/com/example/ragbackend/chat/enums/ChatMessageRole.java
src/main/java/com/example/ragbackend/chat/mapper/ChatSessionMapper.java
src/main/java/com/example/ragbackend/chat/mapper/ChatMessageMapper.java
src/main/java/com/example/ragbackend/chat/service/ChatSessionService.java
src/main/java/com/example/ragbackend/chat/service/ChatMessageService.java
src/main/java/com/example/ragbackend/chat/service/ChatService.java
src/main/java/com/example/ragbackend/chat/service/impl/ChatSessionServiceImpl.java
src/main/java/com/example/ragbackend/chat/service/impl/ChatMessageServiceImpl.java
src/main/java/com/example/ragbackend/chat/service/impl/ChatServiceImpl.java
src/test/java/com/example/ragbackend/chat/ChatPersistenceTest.java
src/test/java/com/example/ragbackend/chat/ChatServiceTest.java
src/test/java/com/example/ragbackend/chat/ChatControllerTest.java
README.md
```

## 4. 数据模型

`chat_session` 保存：

```text
knowledgeBaseId
userId
title
createdAt
updatedAt
```

`chat_message` 保存：

```text
sessionId
role
content
referencesJson
createdAt
```

role 只允许 `USER`、`ASSISTANT`、`SYSTEM`。本轮没有 user 表，默认 `userId` 为 1。

## 5. 接口

```text
POST /api/chat/once
```

请求示例：

```json
{
  "knowledgeBaseId": 1,
  "sessionId": null,
  "question": "这份文档讲了什么？",
  "topK": 5
}
```

响应 data 至少包含：

```text
sessionId
userMessageId
assistantMessageId
answer
references
```

`topK` 为空时默认使用 5，允许范围为 1 到 20。

## 6. 调用链

```text
POST /api/chat/once
  ↓
ChatController
  ↓
ChatService
  ↓
ChatSessionService / ChatMessageService
  ↓
RetrievalService
  ↓
PromptBuilder
  ↓
LlmService
  ↓
MockLlmClient
  ↓
保存 assistant message
  ↓
返回 answer + references
```

## 7. 核心职责

`ChatController`：

- 负责 HTTP 请求、Bean Validation 和统一 ApiResponse。
- 不包含检索、prompt、LLM 或持久化细节。

`ChatService`：

- 校验业务请求。
- 创建或读取 session。
- 校验已有 session 的 knowledgeBaseId 归属。
- 保存 USER message。
- 调用 RetrievalService、PromptBuilder 和 LlmService。
- 保存带 `references_json` 的 ASSISTANT message。
- 组装 ChatOnceResponse。

`ChatSessionService` / `ChatMessageService`：

- 只负责 chat 表的基础数据访问。
- 不负责 RAG 编排。

`RetrievalService`、`PromptBuilder`、`LlmService`：

- 保持原有职责边界，分别负责检索、prompt 构造和 LLM 调用。

## 8. 会话与引用规则

- `sessionId` 为空时创建新 session，标题取用户问题前 200 个字符。
- `sessionId` 不为空时必须存在。
- 已有 session 必须属于请求中的 knowledgeBaseId，否则返回 `CHAT_SESSION_KNOWLEDGE_BASE_MISMATCH`。
- 用户问题保存为 `USER` message。
- LLM answer 保存为 `ASSISTANT` message。
- references 使用 Jackson 序列化为 JSON 字符串并保存到 `references_json`。
- 返回引用包含 chunkId、documentId、knowledgeBaseId、chunkIndex、score 和 content。

## 9. 测试

测试默认使用：

```text
APP_EMBEDDING_PROVIDER=mock
APP_LLM_PROVIDER=mock
```

Controller 闭环测试只 mock RetrievalService，实际使用 ChatController、ChatService、PromptBuilder、LlmService、MockLlmClient、Flyway 和 H2，覆盖：

- 无 sessionId 时创建 session。
- 保存 USER message。
- 保存 ASSISTANT message。
- 返回稳定 mock answer。
- 返回 references。
- 保存 `references_json`。
- topK 为空时默认使用 5。
- question 为空时返回参数校验错误。
- sessionId 不存在时返回 `CHAT_SESSION_NOT_FOUND`。

ChatService 单元测试验证会调用 RetrievalService、PromptBuilder 和 LlmService。

## 10. 运行与验证

```bash
mvn test
docker compose up -d
mvn spring-boot:run
```

应用启动后调用：

```bash
curl -X POST http://localhost:8080/api/chat/once \
  -H "Content-Type: application/json" \
  -d '{"knowledgeBaseId":1,"question":"这份文档讲了什么？","topK":5}'
```

真实运行仍需要 PostgreSQL；要获得检索结果，还需要 Qdrant 中已有对应知识库的 chunk 向量。

## 11. 本轮刻意不做

- 不接真实 Qwen 或 OpenAI LLM。
- 不读取真实 LLM API key。
- 不做 SSE 或 `/api/chat/stream`。
- 不做 retrieval_log。
- 不做 llm_call_log。
- 不做 request_id 链路追踪。
- 不做复杂多轮记忆。
- 不做 Agent 工作流或 reranker。
- 不引入 Redis、Elasticsearch 或 RabbitMQ。

## 12. 下一轮建议

进入 Phase 5.3：检索日志、request_id 链路追踪与问答可观测性。
