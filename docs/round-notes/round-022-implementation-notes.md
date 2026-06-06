# Round 022 Implementation Notes

## 1. 本轮定位

本轮执行 Phase 5.4：真实 LLM Client 与模型配置模板。

目标是在保留 Mock LLM 默认行为的前提下，新增 DashScope OpenAI-compatible Chat Completions 适配，使 `/api/chat/once` 可以通过配置切换到千问 LLM。

## 2. 本轮完成

- 扩展 `LlmProperties`。
- 新增 `QwenLlmClient`。
- 补充 application.yml 的千问 LLM 配置。
- 更新 `.env.qwen.example`。
- 更新 `.env.example` 的默认 Mock LLM 配置。
- LlmService 补充默认 temperature。
- 保持 MockLlmClient 为默认实现。
- 新增 Qwen LLM HTTP 契约与错误处理测试。
- 更新 README。
- 使用 `qwen-plus` 完成真实 DashScope Chat Completions 最小验证。

## 3. 主要文件

```text
.env.example
.env.qwen.example
README.md
docs/round-notes/round-022-implementation-notes.md
src/main/resources/application.yml
src/test/resources/application-test.yml
src/main/java/com/example/ragbackend/llm/config/LlmProperties.java
src/main/java/com/example/ragbackend/llm/client/QwenLlmClient.java
src/main/java/com/example/ragbackend/llm/service/impl/LlmServiceImpl.java
src/test/java/com/example/ragbackend/llm/client/QwenLlmClientTest.java
src/test/java/com/example/ragbackend/llm/LlmServiceTest.java
```

## 4. LLM 配置

默认配置：

```text
APP_LLM_PROVIDER=mock
APP_LLM_MODEL=mock-rag-assistant
APP_LLM_TEMPERATURE=0.2
APP_LLM_MAX_TOKENS=1000
```

默认 provider 不需要 API key，项目和测试继续使用 MockLlmClient。

千问配置：

```text
APP_LLM_PROVIDER=qwen
APP_LLM_MODEL=qwen-plus
QWEN_LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
APP_LLM_TEMPERATURE=0.2
APP_LLM_MAX_TOKENS=1000
DASHSCOPE_API_KEY=replace-with-your-api-key
```

真实 key 只允许放在本地 `.env`、环境变量或 IDEA Run Configuration 中，不提交到 Git。

`DASHSCOPE_API_KEY` 可以同时用于 QwenEmbeddingClient 和 QwenLlmClient。embedding provider 与 LLM provider 相互独立。

## 5. LlmProperties

当前字段：

```text
provider
model
baseUrl
apiKey
temperature
maxTokens
```

`resolveBaseUrl` 会去除末尾 `/`，保证请求路径稳定拼接为 `/chat/completions`。

apiKey 只用于 Authorization header，不会出现在接口响应、业务日志或文档中。

## 6. LLM 调用链

```text
ChatService
  ↓
PromptBuilder
  ↓
LlmService
  ↓
LlmClient
  ↓
MockLlmClient 或 QwenLlmClient
```

职责：

- ChatService 继续负责 RAG 问答主流程编排。
- PromptBuilder 只负责根据 question 和 chunks 构造 RAG prompt。
- LlmService 是业务侧统一入口，校验 prompt，并补充默认 model 和 temperature。
- LlmClient 是模型适配接口。
- MockLlmClient 提供稳定、无网络依赖的测试答案。
- QwenLlmClient 只负责 DashScope HTTP 适配、鉴权、请求体构造、响应解析和错误转换。

ChatService、PromptBuilder 和 Controller 都没有千问 HTTP 调用逻辑。

## 7. QwenLlmClient 请求

请求地址：

```text
{QWEN_LLM_BASE_URL}/chat/completions
```

请求 header：

```text
Authorization: Bearer ${DASHSCOPE_API_KEY}
Content-Type: application/json
```

请求体：

```json
{
  "model": "qwen-plus",
  "messages": [
    {
      "role": "system",
      "content": "你是一个严谨的知识库问答助手。"
    },
    {
      "role": "user",
      "content": "<RAG prompt>"
    }
  ],
  "temperature": 0.2,
  "max_tokens": 1000
}
```

客户端从 `choices[0].message.content` 读取 assistant content。

## 8. 错误处理

错误码：

```text
QWEN_LLM_API_KEY_MISSING
QWEN_LLM_REQUEST_FAILED
QWEN_LLM_RESPONSE_INVALID
```

规则：

- provider 为 qwen 且 key 缺失或仍是模板占位符时，返回清晰配置错误。
- HTTP 非成功响应会转换为请求失败错误，只记录 HTTP status，不记录 key。
- 网络不可用时返回服务不可用错误。
- response 缺少 choices 或 assistant content 时返回响应格式错误。

## 9. 测试

自动测试使用 MockRestServiceServer，不调用真实千问，也不依赖真实 API key。

覆盖：

- 缺少 API key 返回 `QWEN_LLM_API_KEY_MISSING`。
- Authorization header 使用 Bearer token。
- 请求 URL 是 `/chat/completions`。
- 请求体包含 model、system/user messages、temperature 和 max_tokens。
- 正确解析 assistant content。
- response 缺少 content 时返回 `QWEN_LLM_RESPONSE_INVALID`。
- HTTP 失败时返回 `QWEN_LLM_REQUEST_FAILED`。
- LlmService 会补充默认 model 和 temperature。
- 默认 Mock LLM 的 `/api/chat/once` 测试继续通过。

测试配置明确使用：

```text
app.llm.provider=mock
app.llm.api-key=
```

因此 `mvn test` 不依赖真实 key。

## 10. 真实 LLM 验证

已使用本地 `.env` 中的 DashScope key 和 `qwen-plus` 发起最小 Chat Completions 请求。

验证结果：

```text
请求成功
响应模型：qwen-plus
assistant content 包含预期标记
```

验证过程中没有输出、写入或提交真实 API key。

自动化测试验证 Java QwenLlmClient 的请求与响应契约；真实请求验证 DashScope endpoint、key 和模型当前可用。

## 11. 本轮刻意不做

- 不做 SSE。
- 不做 `/api/chat/stream`。
- 不做 llm_call_log。
- 不持久化 token 统计。
- 不做复杂多轮记忆。
- 不做 Agent 工作流或 reranker。
- 不引入 Redis、Elasticsearch 或 RabbitMQ。
- 不接 OpenAI LLM。

## 12. 下一轮建议

进入 Phase 5.5：SSE 流式输出与 llm_call_log。
