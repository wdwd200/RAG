# Round 026 Implementation Notes

## 1. 本轮定位

本轮执行 Phase 6.2：检索评测运行与 Recall@K / HitRate@K / MRR 计算。

本轮基于 Phase 6.1 的 `evaluation_dataset` 和 `evaluation_question`，同步运行一次检索评测，保存 report 和 question-level results。本轮只评测检索是否召回正确 chunk，不评测 LLM 答案质量。

## 2. 本轮完成

- 新增 `evaluation_report` 表。
- 新增 `evaluation_question_result` 表。
- 新增 EvaluationReport / EvaluationQuestionResult Entity、Mapper、DTO。
- 新增 `RetrievalMetricsCalculator`。
- 新增 `QuestionMetricResult` 和 `RetrievalMetricResult`。
- 新增 `EvaluationReportService`，只负责 report 和 question result 数据访问。
- 新增 `EvaluationRunService`，同步编排评测运行主流程。
- 新增 `EvaluationRunController`。
- 新增 `POST /api/evaluation/datasets/{datasetId}/run`。
- 新增 `GET /api/evaluation/reports/{reportId}`。
- 新增 `GET /api/evaluation/reports/{reportId}/question-results`。
- 新增指标计算单元测试。
- 新增评测运行 HTTP 集成测试。
- 更新 README。

## 3. 新增表

`evaluation_report`：

```text
id
dataset_id
knowledge_base_id
top_k
question_count
recall_at_k
hit_rate_at_k
mrr
status
error_message
created_at
finished_at
```

索引：

```text
idx_evaluation_report_dataset_id
idx_evaluation_report_knowledge_base_id
```

`status` 第一版使用：

```text
RUNNING
SUCCESS
FAILED
```

`evaluation_question_result`：

```text
id
report_id
question_id
question
expected_chunk_ids_json
retrieved_chunk_ids_json
hit
reciprocal_rank
recall_at_k
ranked_hit_position
created_at
```

索引：

```text
idx_evaluation_question_result_report_id
idx_evaluation_question_result_question_id
```

`ranked_hit_position` 从 1 开始，表示第一个命中的排名；未命中时为空。

## 4. 接口列表

```text
POST /api/evaluation/datasets/{datasetId}/run
GET  /api/evaluation/reports/{reportId}
GET  /api/evaluation/reports/{reportId}/question-results
```

运行请求：

```json
{
  "topK": 5
}
```

`topK` 为空时默认 5，允许范围为 1 到 20。

## 5. 检索评测调用链

```text
POST /api/evaluation/datasets/{datasetId}/run
  ↓
EvaluationRunController
  ↓
EvaluationRunService
  ↓
EvaluationDatasetService 读取 dataset
  ↓
EvaluationQuestionService 读取 dataset 下问题
  ↓
RetrievalService
  ↓
RetrievalMetricsCalculator
  ↓
EvaluationReportService
  ↓
evaluation_report / evaluation_question_result 表
```

`EvaluationRunService` 是本轮评测运行主流程编排位置。`RetrievalService` 仍然只负责检索，不依赖 evaluation 模块，也不负责指标计算。

## 6. 指标定义

Recall@K：

```text
单题 Recall@K = topK 检索结果命中的相关 chunk 数 / 标准相关 chunk 数
report.recallAtK = 所有题 Recall@K 的平均值
```

HitRate@K：

```text
单题 topK 中只要命中任意相关 chunk，则 hit = true
report.hitRateAtK = hit 问题数 / 总问题数
```

MRR：

```text
单题第一个命中的相关 chunk 排名为 r，则 reciprocalRank = 1 / r
未命中 reciprocalRank = 0
report.mrr = 所有题 reciprocalRank 的平均值
```

## 7. question-level result 保存规则

每个评测问题会保存一条 `evaluation_question_result`：

- `expected_chunk_ids_json`：来自 `evaluation_question.relevantChunkIds`。
- `retrieved_chunk_ids_json`：本次 `RetrievalService` 实际返回的 chunkId 列表。
- `hit`：topK 中是否命中任意 expected chunk。
- `reciprocal_rank`：第一个命中的倒数排名。
- `recall_at_k`：该题的 Recall@K。
- `ranked_hit_position`：第一个命中的排名，从 1 开始；未命中为空。

本轮不保存完整 answer，因为本轮不生成 answer，也不评测 LLM。

## 8. 失败策略

第一版选择简单策略：

```text
任一问题检索失败，则整个 report 标记为 FAILED，并保存 errorMessage。
```

dataset 不存在、dataset 无 questions、topK 超过上限属于运行前校验错误，不创建 report。

## 9. 本轮不调用 LLM

本轮评测只调用：

```text
RetrievalService
```

本轮不调用：

```text
ChatService
LlmService
PromptBuilder
QwenLlmClient
MockLlmClient
```

因此不会生成 answer，也不会消耗真实 LLM 额度。自动测试使用 mock `RetrievalService`，不依赖 Docker、Qdrant、真实 embedding 或真实 LLM。

## 10. 逻辑可读性检查

- EvaluationRunController 只负责评测运行 HTTP 入口。
- EvaluationRunService 只负责评测运行编排。
- RetrievalMetricsCalculator 只负责指标计算，不访问数据库。
- EvaluationReportService 只负责 report / question result 数据访问。
- EvaluationQuestionService 仍只负责 question 数据访问和标注数据读取。
- RetrievalService 没有依赖 evaluation 模块。
- 本轮没有混入 bad cases 专门逻辑。

## 11. 测试覆盖

新增 `RetrievalMetricsCalculatorTest`，覆盖：

- 单题 Recall@K。
- HitRate@K。
- MRR。
- 多问题平均指标。
- 无命中时 recall、hitRate、mrr 为 0。
- 空输入返回清晰错误。
- relevantChunkIds 为空返回清晰错误。

新增 `EvaluationRunControllerTest`，覆盖：

- `POST /api/evaluation/datasets/{datasetId}/run` 成功生成 report。
- report 中 questionCount 正确。
- report 中 recallAtK / hitRateAtK / mrr 正确。
- question result 保存 retrievedChunkIds。
- dataset 不存在时运行失败。
- dataset 无 questions 时运行失败。
- topK 超过上限时返回清晰错误。
- topK 默认值为 5。
- 查询 report 详情成功。
- 查询 report 不存在时返回清晰错误。
- 查询 question results 成功。

## 12. 本轮刻意不做

- 不做 LLM 答案质量评测。
- 不做 LLM-as-judge。
- 不做 bad cases 专门接口。
- 不做公开数据集自动导入。
- 不做异步评测任务。
- 不做评测任务队列。
- 不做 reranker。
- 不引入 Redis / Elasticsearch / RabbitMQ。

## 13. 下一轮建议

进入 Phase 6.3：bad cases 输出、评测报告查询与问题级结果分析。
