# Interview Talking Points

## 1. 项目怎么讲

可以按一条主链路讲：

```text
知识库管理
  -> 文档上传
  -> 文档解析和 chunk 切分
  -> embedding 向量化
  -> Qdrant 检索
  -> RAG 问答
  -> requestId 审计
  -> evaluation 评测
```

项目重点不是堆模型，而是把 RAG 后端主链路的工程边界做清楚：事实源、索引、模型适配、审计日志、评测闭环各自有明确职责。

## 2. 为什么关系库是事实源，Qdrant 是索引

关系库保存稳定业务事实：

- document 状态。
- active chunk 内容。
- processingVersion。
- chat_message。
- retrieval_log。
- llm_call_log。
- evaluation 数据。

Qdrant 保存向量索引，用于近邻检索。Qdrant payload 只辅助检索过滤，不作为最终返回内容。检索结果中的 `chunkId` 会回查关系库，最终返回 active chunk 的 content。

这样设计可以避免：

- Qdrant payload 与关系库内容不一致时前端看到旧内容。
- chunk 重新处理后仍返回 inactive chunk。
- 跨知识库检索导致数据污染。

## 3. 文档状态机如何设计

文档状态体现主流程：

```text
UPLOADED
  -> CHUNKED
  -> EMBEDDING
  -> INDEXING
  -> INDEXED
```

失败时进入：

```text
FAILED
```

并记录：

```text
failedStage
errorMessage
```

这样前端和运维排查可以知道失败发生在 parsing、chunking、embedding 还是 indexing 阶段。

## 4. Chunk 重新处理如何避免脏数据

重新 process 文档时，不直接覆盖旧 chunk，而是：

```text
旧 active chunk 标记为 inactive
插入新 chunk
新 chunk 使用新的 processingVersion
查询接口只返回 active chunk
```

这样可以保留历史数据，同时避免当前检索和评测误用旧 chunk。

## 5. requestId 如何串联日志

每次问答生成一个 requestId：

```text
USER chat_message
retrieval_log 多条
llm_call_log 一条
ASSISTANT chat_message
```

用户拿到 answer 后，可以用 requestId 查询：

```text
GET /api/audit/retrieval-logs/{requestId}
GET /api/audit/llm-call-logs/{requestId}
```

这样可以追踪这次回答用了哪些 chunk、检索排名如何、调用了哪个 provider / model、是否失败以及错误摘要。

## 6. Recall@K / HitRate@K / MRR 如何解释

Recall@K：

```text
检索 topK 中命中的相关 chunk 数 / 标注的相关 chunk 数。
看召回完整度。
```

HitRate@K：

```text
topK 中只要命中任意相关 chunk 就算 hit。
看问题是否至少找到了一个正确依据。
```

MRR：

```text
第一个正确 chunk 的排名越靠前越好。
第一个命中排第 r，则 reciprocalRank = 1 / r。
看排序质量。
```

bad cases：

```text
NO_HIT: 没命中
LOW_RECALL: 命中了但没召全
LOW_RANK: 召全了但第一个命中排名靠后
```

## 7. 为什么第一版不做 Redis / Elasticsearch / reranker

第一版目标是跑通主链路和评测闭环：

```text
文档 -> chunk -> embedding -> Qdrant -> RAG -> audit -> evaluation
```

Redis、Elasticsearch、reranker 都是增强层。如果主链路和评测没有稳定，过早加入这些组件会增加排查复杂度。

可以这样回答：

```text
第一版先保证事实源、索引、模型适配和评测闭环清楚。后续有了评测指标和 bad cases 后，再基于实际瓶颈选择引入 Hybrid Search、reranker 或缓存。
```

## 8. 如果继续增强，下一步怎么做

优先级建议：

1. Testcontainers：让 PostgreSQL / Qdrant 集成测试更接近真实环境。
2. Hybrid Search：引入 BM25 补足纯向量检索对关键词、编号、术语的弱点。
3. Reranker：提升 topK 排序质量，重点观察 MRR 和 LOW_RANK。
4. 异步文档处理：上传后进入任务队列，支持进度查询和失败重试。
5. Redis：缓存热点知识库配置、热点检索结果和限流。
6. LLM-as-judge：在检索评测稳定后，再评估答案质量。

## 9. 不要夸大的点

不要说已经实现：

- 生产级权限系统。
- Redis 缓存。
- Elasticsearch / Hybrid Search。
- Reranker。
- 公开数据集自动导入。
- LLM-as-judge。
- Graph RAG。
- 多租户 SaaS。

可以说这些已经进入后续增强规划。
