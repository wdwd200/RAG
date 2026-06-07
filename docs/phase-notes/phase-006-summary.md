# Phase 6 Summary

## 1. Phase 6 完成了什么

Phase 6 为项目补齐了检索评测能力和可复现 demo：

- 新增 evaluation dataset / evaluation question，用于维护人工标注的检索评测集。
- 新增 evaluation report / evaluation question result，用于保存每次评测运行和问题级结果。
- 新增 Recall@K、HitRate@K、MRR 指标计算。
- 新增 report summary 和 bad cases 分析。
- 新增固定示例文档、评测问题模板、demo guide 和示例报告说明。

Phase 6 只评估检索是否召回正确 chunk，不评估 LLM 生成答案质量。

## 2. 核心表职责

`evaluation_dataset`：

```text
评测集主表，绑定 knowledgeBaseId，记录名称、描述和问题数量。
```

`evaluation_question`：

```text
评测问题表，保存 question、groundTruthAnswer、relevantChunkIds 和 relevant content hash。
relevantChunkIds 指向关系库中的 active document_chunk。
```

`evaluation_report`：

```text
一次评测运行的 report，保存 datasetId、knowledgeBaseId、topK、questionCount、Recall@K、HitRate@K、MRR 和运行状态。
```

`evaluation_question_result`：

```text
问题级结果表，保存每个问题的 expectedChunkIds、retrievedChunkIds、hit、recallAtK、reciprocalRank 和 rankedHitPosition。
```

## 3. 评测集如何构造

第一版评测集由人工构造：

```text
上传文档
  ↓
process 生成 active chunks
  ↓
GET /api/documents/{documentId}/chunks 查看 chunk
  ↓
人工选择 relevantChunkIds
  ↓
写入 evaluation_question
```

`chunkId` 是数据库运行时生成的，不同数据库、不同演示轮次、不同处理顺序可能不同。因此 demo 模板只提供问题、标准答案和 `sourceHint`，不写死最终 chunkId。

## 4. 指标定义

Recall@K：

```text
单题 Recall@K = topK 命中的相关 chunk 数 / 标注的相关 chunk 数
report.recallAtK = 所有题 Recall@K 的平均值
```

HitRate@K：

```text
单题 topK 中只要命中任意 relevant chunk，则 hit = true
report.hitRateAtK = hit 问题数 / 总问题数
```

MRR：

```text
单题第一个命中 relevant chunk 的排名为 r，则 reciprocalRank = 1 / r
未命中时 reciprocalRank = 0
report.mrr = 所有题 reciprocalRank 的平均值
```

## 5. Bad Cases 分类

`NO_HIT`：

```text
topK 内没有命中任何 relevant chunk。
```

`LOW_RECALL`：

```text
topK 内命中至少一个 relevant chunk，但 Recall@K < 1。
```

`LOW_RANK`：

```text
已经全部召回 relevant chunks，但第一个命中排名大于 1。
```

完整命中且第一个命中排名为 1 的问题不会出现在 bad cases 中。

## 6. Demo 文档位置

示例文档：

```text
docs/demo/sample-documents/hr-handbook.md
docs/demo/sample-documents/expense-policy.md
docs/demo/sample-documents/engineering-guide.md
```

示例评测集说明：

```text
docs/demo/sample-evaluation-dataset.md
docs/demo/sample-evaluation-questions.json
docs/demo/sample-evaluation-report.md
```

完整 demo guide：

```text
docs/demo/phase-006-demo-guide.md
```

最终演示清单：

```text
docs/demo/final-demo-checklist.md
```

## 7. 自动测试与真实链路 Demo 的区别

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
真实 key 只放在 .env、环境变量或 IDEA Run Configuration 中，不提交到 Git。
```

## 8. 当前还没有做什么

- 没有做公开数据集自动导入。
- 没有做 LLM-as-judge。
- 没有评估 LLM answer 质量。
- 没有做异步评测任务。
- 没有做 Redis 缓存、限流或分布式锁。
- 没有做 Elasticsearch / BM25 / Hybrid Search。
- 没有做 reranker。
- 没有做权限系统。

## 9. 后续可选增强方向

- Redis 缓存与限流。
- Elasticsearch / BM25 与 Hybrid Search。
- Reranker。
- 异步文档处理和任务状态查询。
- Testcontainers 集成测试。
- 公开数据集导入。
- LLM-as-judge 答案质量评估。
- 知识库权限系统。
