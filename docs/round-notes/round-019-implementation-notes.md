# Round 019 Implementation Notes

## 1. 本轮定位

本轮执行 Phase 5.1：`LlmClient` 抽象、Mock LLM 与 `PromptBuilder`。

本轮目标是为后续 RAG 问答闭环建立清晰的 LLM 层和 prompt 构造边界。本轮不提供聊天 HTTP 接口，不创建 chat 表，也不调用真实 LLM。

## 2. 本轮完成

- 新增 `app.llm.provider` 和 `app.llm.model` 配置。
- 新增 `LlmProperties`。
- 新增 `LlmClient` 模型适配接口。
- 新增 `MockLlmClient`。
- 新增 `LlmRequest` 和 `LlmResponse`。
- 新增 `LlmService` 和 `LlmServiceImpl`。
- 新增 `PromptBuilder` 和 `RagPromptBuilder`。
- 新增 `PromptContextChunk` 和 `RagPromptRequest`。
- 新增 Mock LLM、LlmService 和 PromptBuilder 测试。
- 更新 README。

## 3. 新增或修改文件

```text
README.md
docs/round-notes/round-019-implementation-notes.md
src/main/resources/application.yml
src/test/resources/application-test.yml
src/main/java/com/example/ragbackend/RagBackendApplication.java
src/main/java/com/example/ragbackend/llm/config/LlmProperties.java
src/main/java/com/example/ragbackend/llm/client/LlmClient.java
src/main/java/com/example/ragbackend/llm/client/MockLlmClient.java
src/main/java/com/example/ragbackend/llm/model/LlmRequest.java
src/main/java/com/example/ragbackend/llm/model/LlmResponse.java
src/main/java/com/example/ragbackend/llm/service/LlmService.java
src/main/java/com/example/ragbackend/llm/service/impl/LlmServiceImpl.java
src/main/java/com/example/ragbackend/chat/prompt/PromptBuilder.java
src/main/java/com/example/ragbackend/chat/prompt/RagPromptBuilder.java
src/main/java/com/example/ragbackend/chat/prompt/PromptContextChunk.java
src/main/java/com/example/ragbackend/chat/prompt/RagPromptRequest.java
src/test/java/com/example/ragbackend/llm/MockLlmClientTest.java
src/test/java/com/example/ragbackend/llm/LlmServiceTest.java
src/test/java/com/example/ragbackend/chat/prompt/RagPromptBuilderTest.java
```

## 4. LLM 配置

默认配置：

```text
APP_LLM_PROVIDER=mock
APP_LLM_MODEL=mock-rag-assistant
```

`LlmProperties` 使用 `app.llm` 前缀，并在应用入口注册配置绑定。

LLM 配置独立于 embedding 配置。本轮不读取任何真实 LLM API key，也没有 Qwen 或 OpenAI LLM 配置。

测试配置明确使用：

```text
app.llm.provider=mock
app.llm.model=mock-rag-assistant
```

## 5. LLM 调用链

```text
未来 ChatService
  ↓
PromptBuilder
  ↓
LlmService
  ↓
LlmClient
  ↓
MockLlmClient
```

本轮还没有 `ChatService`。该调用链描述下一轮业务编排应依赖的边界。

## 6. 核心职责

`PromptBuilder`：

- 只负责根据 question 和 context chunks 构造 prompt。
- 不调用 LLM。
- 不查询数据库。
- 不访问 Qdrant。
- 不依赖 retrieval service。

`LlmService`：

- 是业务侧调用 LLM 的统一入口。
- 校验 request 和 prompt。
- 请求没有指定 model 时使用 `app.llm.model`。
- 调用 `LlmClient` 并返回 `LlmResponse`。

`LlmClient`：

- 只表达 `prompt -> answer` 模型适配接口。
- 不包含 chat、retrieval 或持久化逻辑。

`MockLlmClient`：

- 只在 `app.llm.provider=mock` 时启用。
- 返回稳定、可预测的 mock answer。
- 不访问网络，不需要 API key。
- 不依赖 document、chunk 或 retrieval 模块。

## 7. LLM 模型

`LlmRequest`：

```text
prompt
model
temperature
```

`LlmResponse`：

```text
content
model
success
errorMessage
```

`temperature` 当前仅作为可选请求字段透传，Mock 实现不使用该值。

## 8. RAG Prompt 结构

`RagPromptBuilder` 生成的 prompt 包含：

```text
回答约束
用户问题
检索到的上下文片段
引用要求
```

核心约束：

```text
你只能基于给定上下文回答。
如果上下文不足以回答，请说明“根据当前知识库内容无法确定”。
不要编造知识库外的信息。
回答时使用 [片段N] 标注引用来源。
```

每个上下文片段可以包含：

```text
chunkId
documentId
fileName
score
content
```

没有上下文片段时，prompt 会明确写入“未检索到可用上下文片段”，同时保留无法确定的回答约束。

## 9. 测试覆盖

新增测试覆盖：

- MockLlmClient 对相同请求返回稳定结果。
- MockLlmClient 保留 LlmService 传入的模型名称。
- LlmService 调用 LlmClient。
- LlmService 为缺失 model 的请求补充默认模型。
- 空 prompt 返回 `LLM_REQUEST_INVALID`。
- PromptBuilder 包含用户问题。
- PromptBuilder 包含 chunk content 和元数据。
- PromptBuilder 包含上下文不足时说明无法确定的约束。
- PromptBuilder 包含引用要求。
- 没有 chunks 时 prompt 结构仍然清楚。
- 空 question 返回 `PROMPT_REQUEST_INVALID`。

全部测试只使用 Mock LLM，不依赖真实 API key。

## 10. 当前代码主线

Phase 5.1 当前主线：

```text
RagPromptRequest
  ↓
PromptBuilder.build
  ↓
生成纯文本 prompt
  ↓
LlmService.complete
  ↓
LlmClient.complete
  ↓
MockLlmClient
  ↓
LlmResponse
```

本轮没有把 prompt 构造放进 Controller、RetrievalService 或 EmbeddingService。

## 11. 本轮刻意不做

- 不接真实 Qwen LLM。
- 不接 OpenAI LLM。
- 不读取真实 LLM API key。
- 不创建 `chat_session` 表。
- 不创建 `chat_message` 表。
- 不实现 `/api/chat/once`。
- 不实现 `/api/chat/stream`。
- 不做 SSE。
- 不做 retrieval / LLM 调用日志表。
- 不做多轮记忆、Agent、reranker。
- 不引入 Redis、Elasticsearch 或 RabbitMQ。

## 12. 下一轮建议

进入 Phase 5.2：chat 基础表与 `/api/chat/once` JSON 闭环。

下一轮建议由 `ChatService` 编排：

```text
RetrievalService
  ↓
PromptBuilder
  ↓
LlmService
  ↓
返回一次性 JSON 回答
```

真实 LLM 与 SSE 仍应继续留在后续轮次。
