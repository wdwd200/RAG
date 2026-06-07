# Round 029 Implementation Notes

## 1. 本轮定位

本轮执行 Phase 6.5：工程化收尾、项目文档、简历材料与可选增强规划。

这是项目第一版最终收尾轮。本轮不新增业务表、不新增业务接口、不实现 Redis / Elasticsearch / Hybrid Search / reranker / LLM-as-judge / 权限系统，只做文档整理、演示入口、简历材料、增强规划、敏感信息检查和最终验证。

## 2. 新增总览文档

新增：

```text
docs/project-overview.md
docs/api-index.md
docs/phase-notes/phase-006-summary.md
docs/demo/final-demo-checklist.md
```

职责：

```text
project-overview:
帮助新读者快速理解项目定位、六阶段完成情况、总体架构、核心调用链、数据表和 PostgreSQL / Qdrant 边界。

api-index:
按 Health、KnowledgeBase、Document、Chunk、Retrieval、Chat、Audit、Evaluation、Demo 整理主要接口入口。

phase-006-summary:
总结 Phase 6 的评测集、评测指标、bad cases、demo 资产和可复现性边界。

final-demo-checklist:
整理最终演示前的环境准备、启动、健康检查、mock 链路、可选 Qwen 链路、evaluation demo 和清理步骤。
```

## 3. 新增简历 / 面试材料

新增：

```text
docs/resume/project-description.md
docs/resume/interview-talking-points.md
```

`project-description.md` 包含：

```text
一句话项目描述
简历 bullet 版本
技术栈版本
可量化能力描述
不夸大的边界说明
```

`interview-talking-points.md` 包含：

```text
项目怎么讲
为什么关系库是事实源，Qdrant 是索引
文档状态机如何设计
chunk 重新处理如何避免脏数据
requestId 如何串联日志
Recall@K / HitRate@K / MRR 如何解释
为什么第一版不做 Redis / Elasticsearch / reranker
如果继续增强，下一步怎么做
```

这些材料明确避免夸大，不声称第一版已经实现 Redis、Elasticsearch、reranker、Graph RAG、权限系统或公开数据集自动导入。

## 4. 新增可选增强规划

新增：

```text
docs/roadmap/optional-enhancements.md
```

规划内容包括：

```text
Redis 缓存与限流
缓存穿透 / 击穿 / 雪崩应对
Elasticsearch / BM25
Hybrid Search
Reranker
异步文档处理
Testcontainers
权限系统
公开数据集导入
LLM-as-judge
```

这些都是后续增强方向，不是第一版已完成能力。

## 5. README 最终整理

README 已调整为第一版完成状态的入口页，主要保留：

```text
当前状态：第一版 6 阶段已完成
核心功能列表
技术栈
快速启动
自动测试说明
Mock 模式说明
Qwen 模式说明
核心 API 入口
Demo 文档入口
项目文档索引
后续增强方向
```

详细说明转移到 `docs/` 下，避免 README 继续膨胀。

## 6. 敏感信息与仓库卫生检查

本轮需要确认：

```text
.env 不提交
.env.local 不提交
.env.*.local 不提交
storage/ 不提交
docs/demo/*.import.json 不提交
target/ 不提交
.env.qwen.example 只保留占位符
```

真实 `DASHSCOPE_API_KEY` 只允许存在于本地 `.env`、环境变量或 IDEA Run Configuration，不允许提交到 Git。

## 7. 第一版最终能力

当前项目第一版具备：

- 知识库管理。
- 文档上传和本地 storage。
- 文档解析、chunk 切分和处理状态机。
- Mock / Qwen embedding。
- Qdrant 向量索引和按 `knowledgeBaseId` 过滤检索。
- 一次性 RAG 问答。
- SSE 流式问答事件。
- requestId 审计追踪。
- retrieval_log 和 llm_call_log 查询。
- evaluation dataset / question 管理。
- Recall@K、HitRate@K、MRR 检索评测。
- report summary 和 bad cases 分析。
- 可复现 demo 文档、模板、guide 和最终演示清单。

## 8. 自动测试与真实 Demo 边界

`mvn test`：

```text
使用 H2、mock provider 和固定测试数据。
不依赖 Docker。
不依赖 Qdrant。
不依赖真实 embedding。
不依赖真实 LLM。
```

真实链路 demo：

```text
依赖 Docker，因为需要 PostgreSQL 和 Qdrant。
使用 mock provider 时不需要 API key。
使用 Qwen provider 时需要本地 DASHSCOPE_API_KEY。
真实 key 不提交到 Git。
```

## 9. 最终验证要求

本轮必须执行：

```bash
mvn test
git diff --check
```

如果本地 Docker 和应用验证可用，可额外验证：

```text
GET /api/health
GET /api/health/database
GET /api/health/qdrant
```

本轮实际验证结果：

```text
mvn test: 通过，130 tests。
mvn clean package: 通过。
git diff --check: 通过。
敏感信息检查: 通过，未发现真实 API key 被提交。
Docker compose: PostgreSQL 和 Qdrant 容器运行中。
GET /api/health: UP。
GET /api/health/database: UP。
GET /api/health/qdrant: UP。
```

## 10. 本轮刻意不做

- 不新增业务表。
- 不新增业务接口。
- 不实现 Redis。
- 不实现 Elasticsearch。
- 不实现 Hybrid Search。
- 不实现 reranker。
- 不实现 LLM-as-judge。
- 不实现公开数据集自动导入。
- 不实现权限系统。
- 不做大重构。
- 不提交真实 API key。

## 11. 后续可选增强

项目第一版完成后，可按实际目标选择增强：

```text
Redis 缓存与限流
Hybrid Search
Reranker
Testcontainers
异步文档处理
公开数据集导入
LLM-as-judge
权限系统
```
