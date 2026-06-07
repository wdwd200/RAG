# Optional Enhancements Roadmap

本文件只做后续增强规划，不代表第一版已经实现。

## 1. Redis 缓存与限流

可选用途：

- 缓存知识库配置和文档状态摘要。
- 缓存热点检索结果。
- 缓存热点 chunk 详情。
- 对高频 chat / retrieval 请求做简单限流。

需要注意：

- 缓存内容必须有明确失效策略。
- 文档重新 process / index 后，需要清理相关缓存。
- 不应把 Redis 当作事实源。

## 2. 缓存穿透 / 击穿 / 雪崩应对

缓存穿透：

```text
对不存在的数据缓存空值，设置短 TTL。
对明显非法 id 做参数校验。
```

缓存击穿：

```text
热点 key 过期时使用互斥重建。
单个线程回源，其他请求等待或返回旧值。
```

缓存雪崩：

```text
TTL 增加随机抖动。
避免大量 key 同时过期。
重要缓存分批预热。
```

这些属于后续性能增强，第一版未实现。

## 3. Elasticsearch / BM25

可引入 Elasticsearch 或 OpenSearch 做关键词检索：

- 适合专有名词、编号、政策条款、短关键词。
- 弥补纯向量检索对精确词匹配的不足。
- 可保存 documentId、chunkId、knowledgeBaseId、content、metadata。

需要保持关系库事实源原则：搜索结果仍回查 `document_chunk`。

## 4. Hybrid Search

Hybrid Search 可以合并：

```text
向量检索分数
BM25 分数
业务过滤条件
```

常见策略：

- Reciprocal Rank Fusion。
- 分数归一化后加权。
- 先扩大召回，再 rerank。

引入前应先用 evaluation report 判断当前检索短板。

## 5. Reranker

Reranker 用于提升 topK 排序质量。

适合改善：

```text
LOW_RANK bad cases
MRR 偏低
topK 内有正确 chunk 但排名靠后
```

第一版未实现 reranker。后续可在 RetrievalService 后增加 rerank stage，但要保持 Controller 不感知底层实现细节。

## 6. 异步文档处理

当前文档 process / index 是同步接口。后续可以改为异步任务：

- 上传后创建处理任务。
- Worker 执行 parse / split / embedding / index。
- 前端轮询任务状态或使用 SSE。
- 支持失败重试和任务取消。

可选组件：

- Spring TaskExecutor。
- RabbitMQ。
- Redis Stream。
- 数据库任务表。

第一版未引入消息队列。

## 7. Testcontainers

当前自动测试使用 H2 和 mock。后续可以新增 Testcontainers：

- PostgreSQL container 测 Flyway 和 Mapper。
- Qdrant container 测真实 vector search。
- 保持单元测试和集成测试分层。

建议：

```text
mvn test: 快速 H2 / mock 测试
mvn verify 或 profile: Testcontainers 集成测试
```

## 8. 权限系统

第一版没有登录和权限。后续可以增加：

- 用户表。
- 知识库 owner / member。
- JWT。
- Spring Security。
- 文档级访问控制。
- audit log 中记录 userId。

权限系统加入后，检索、问答、评测都必须校验 knowledgeBase 访问权限。

## 9. 公开数据集导入

后续可支持导入公开 RAG / QA 数据集：

- 数据集格式转换。
- 自动创建 evaluation_dataset。
- 自动写入 evaluation_question。
- 标准答案和 relevant 文档定位映射。

需要注意：公开数据集通常没有本项目运行时 chunkId，仍需要映射到当前 chunk 或通过 content hash 辅助匹配。

## 10. LLM-as-judge

当前评测只评估检索，不评估答案质量。

LLM-as-judge 可用于：

- 答案是否忠于上下文。
- 是否覆盖标准答案要点。
- 是否产生幻觉。
- 引用是否支持结论。

风险：

- 成本更高。
- 评测结果不完全稳定。
- judge prompt 需要版本管理。
- 需要记录 judge model、prompt、temperature 和原始输出。

建议在检索评测稳定后再引入。
