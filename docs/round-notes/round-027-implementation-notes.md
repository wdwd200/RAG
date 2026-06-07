# Round 027 Implementation Notes

## 1. 本轮定位

本轮执行 Phase 6.3：bad cases 输出、评测报告查询与问题级结果分析。

本轮基于 Phase 6.2 已有的 `evaluation_report` 和 `evaluation_question_result`，补强评测结果分析能力，让用户可以快速看到哪些问题未命中、哪些问题召回不完整、哪些问题虽然召回完整但第一个命中排名靠后。

本轮不重新设计 Recall@K / HitRate@K / MRR，不做 LLM-as-judge，不导入公开数据集。

## 2. 本轮完成

- 新增 `EvaluationAnalysisService`。
- 新增 `EvaluationAnalysisServiceImpl`。
- 新增 `EvaluationBadCaseResponse`。
- 新增 `EvaluationReportSummaryResponse`。
- 新增 `EvaluationAnalysisController`。
- 新增 `GET /api/evaluation/reports/{reportId}/summary`。
- 新增 `GET /api/evaluation/reports/{reportId}/bad-cases`。
- 补强评测分析测试，覆盖 bad cases 分类、排序、summary 计数、report 不存在错误、question results List 结构。
- 更新 README。

## 3. 关于是否新增 migration

本轮没有新增 `V11__enhance_evaluation_question_result.sql`。

原因是现有 `evaluation_question_result` 已经包含动态分析所需字段：

```text
expected_chunk_ids_json
retrieved_chunk_ids_json
hit
recall_at_k
reciprocal_rank
ranked_hit_position
```

`failureReason`、`badCaseCount`、`noHitCount`、`lowRecallCount` 和 `lowRankCount` 都是基于已有字段可以稳定计算出的派生结果。第一版选择查询时动态计算，避免把派生状态重复写入数据库，也避免后续调整 bad case 规则时需要回填历史数据。

## 4. 接口列表

```text
GET /api/evaluation/reports/{reportId}
GET /api/evaluation/reports/{reportId}/question-results
GET /api/evaluation/reports/{reportId}/summary
GET /api/evaluation/reports/{reportId}/bad-cases
```

`GET /api/evaluation/reports/{reportId}` 和 `GET /api/evaluation/reports/{reportId}/question-results` 沿用 Phase 6.2 已有接口。

## 5. Bad Cases 分析调用链

```text
GET /api/evaluation/reports/{reportId}/bad-cases
  ↓
EvaluationAnalysisController
  ↓
EvaluationAnalysisService
  ↓
EvaluationReportService
  ↓
evaluation_report + evaluation_question_result
  ↓
动态计算 failureReason
  ↓
返回 bad cases
```

`EvaluationAnalysisService` 是 report 分析主流程位置。Controller 只负责 HTTP 入口，不包含 bad case 判定逻辑。

## 6. Report Summary 调用链

```text
GET /api/evaluation/reports/{reportId}/summary
  ↓
EvaluationAnalysisController
  ↓
EvaluationAnalysisService
  ↓
EvaluationReportService
  ↓
evaluation_report + evaluation_question_result
  ↓
聚合 bad case 计数
  ↓
返回 summary
```

summary 复用 report 已落库指标，不重复计算 Recall@K / HitRate@K / MRR，只基于 question results 聚合 bad case 数量。

## 7. Bad Case 判定规则

```text
NO_HIT:
topK 内没有命中任何 relevant chunk，即 hit != true。

LOW_RECALL:
已经命中至少一个 relevant chunk，但 Recall@K < 1。

LOW_RANK:
已经全部召回，即 Recall@K = 1，但第一个命中排名大于 1。
```

完整命中且第一个命中排名为 1 的问题不会出现在 bad cases 中。

排序规则：

```text
1. NO_HIT 优先
2. LOW_RECALL 其次
3. LOW_RANK 最后
4. 同类中 recallAtK 更低的排前
5. rankedHitPosition 更大的排前
6. questionId 升序兜底
```

## 8. Summary 字段含义

```text
badCaseCount:
NO_HIT + LOW_RECALL + LOW_RANK 总数。

noHitCount:
NO_HIT 问题数量。

lowRecallCount:
LOW_RECALL 问题数量。

lowRankCount:
LOW_RANK 问题数量。
```

summary 还返回 report 基础字段：

```text
reportId
datasetId
knowledgeBaseId
topK
questionCount
recallAtK
hitRateAtK
mrr
status
createdAt
finishedAt
```

## 9. Question Results 查询

`GET /api/evaluation/reports/{reportId}/question-results` 继续返回问题级结果，并确认 `expectedChunkIds` 和 `retrievedChunkIds` 是反序列化后的 `List<Long>`，不是原始 JSON 字符串。

本轮不对 question results 做分页。返回顺序沿用 `EvaluationReportService` 中的查询顺序。

## 10. 本轮不重新运行评测

Phase 6.3 的 summary 和 bad cases 查询只读取已有 report 数据：

```text
不调用 RetrievalService
不调用 ChatService
不调用 LlmService
不调用 PromptBuilder
不调用任何 LLM Client
```

因此查询分析接口不会重新检索、不会生成 answer，也不会消耗真实 embedding 或 LLM 额度。

## 11. 测试覆盖

新增 `EvaluationAnalysisControllerTest`，覆盖：

- 查询 bad cases 成功。
- 未命中问题会出现在 bad cases 中。
- `recallAtK < 1` 的问题会出现在 bad cases 中。
- 全部命中且 rank = 1 的问题不会出现在 bad cases 中。
- bad cases 排序中 `NO_HIT` 优先于 `LOW_RECALL`，`LOW_RECALL` 优先于 `LOW_RANK`。
- report summary 返回 `badCaseCount`。
- report summary 返回 `noHitCount`。
- report summary 返回 `lowRecallCount`。
- report summary 返回 `lowRankCount`。
- report 不存在时返回清晰错误。
- question results 返回 `expectedChunkIds` / `retrievedChunkIds` 的 List 结构。
- 查询分析接口不调用 `RetrievalService`。
- 查询分析接口不调用 `LlmService`。

测试使用 H2 和固定测试数据，并通过 mock 隔离 `RetrievalService` 和 `LlmService`，默认不依赖 Docker、Qdrant、真实 embedding 或真实 LLM。

## 12. 逻辑可读性检查

- `EvaluationRunService` 仍只负责运行评测。
- `RetrievalMetricsCalculator` 仍只负责指标计算。
- `EvaluationAnalysisService` 只负责 report / question result 分析。
- `EvaluationReportService` 只负责 report 数据访问。
- Controller 只做 HTTP 入口。
- `RetrievalService` 没有依赖 evaluation 模块。
- bad case 逻辑没有写进 Controller。
- 本轮没有调用 LLM。

## 13. 如何运行和验证

自动化验证：

```bash
mvn test
git diff --check
```

本地启动后可验证：

```bash
curl http://localhost:8080/api/evaluation/reports/1/summary
curl http://localhost:8080/api/evaluation/reports/1/question-results
curl http://localhost:8080/api/evaluation/reports/1/bad-cases
```

这些查询依赖已有 report 数据。如果本地没有 report，需要先创建评测集、创建评测问题并运行：

```bash
curl -X POST http://localhost:8080/api/evaluation/datasets/1/run \
  -H "Content-Type: application/json" \
  -d '{"topK":5}'
```

## 14. 本轮刻意不做

- 不新增评测指标。
- 不修改 Recall@K / HitRate@K / MRR 定义。
- 不做 LLM 答案质量评测。
- 不做 LLM-as-judge。
- 不做公开数据集自动导入。
- 不做异步评测任务。
- 不做评测任务队列。
- 不做 reranker。
- 不引入 Redis / Elasticsearch / RabbitMQ。

## 15. 下一轮建议

进入 Phase 6.4：示例知识库、示例评测集与一键演示流程。

下一轮重点是固定示例文档、固定 chunk 处理参数、固定 evaluation dataset 和可复现的演示命令，让真实链路评测更容易复现。
