# Sample Evaluation Report

## 1. 示例报告说明

Phase 6 的评测报告来自真实运行后的接口响应：

```text
GET /api/evaluation/reports/{reportId}
GET /api/evaluation/reports/{reportId}/summary
GET /api/evaluation/reports/{reportId}/question-results
GET /api/evaluation/reports/{reportId}/bad-cases
```

本文件只说明如何解读报告，不提供固定分数。实际 Recall@K、HitRate@K、MRR 和 bad cases 数量以本地运行结果为准。

## 2. Report 字段

常见 report 字段：

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

`status = SUCCESS` 表示评测运行完成。`status = FAILED` 表示至少一个运行步骤失败，错误信息会写入 report 的 error message。

## 3. 指标解释

Recall@K：

```text
单题 topK 命中的相关 chunk 数 / 标注的相关 chunk 数。
report.recallAtK 是所有题的平均值。
```

HitRate@K：

```text
单题 topK 中只要命中任意一个相关 chunk，就算 hit。
report.hitRateAtK 是 hit 问题数 / 总问题数。
```

MRR：

```text
单题第一个相关 chunk 的命中排名为 r，则 reciprocalRank = 1 / r。
report.mrr 是所有题 reciprocalRank 的平均值。
```

## 4. Bad Cases 解读

`GET /api/evaluation/reports/{reportId}/bad-cases` 会返回需要人工关注的问题。

failureReason 含义：

```text
NO_HIT:
topK 内没有命中任何 relevant chunk。

LOW_RECALL:
topK 内命中了至少一个 relevant chunk，但没有召回全部 relevant chunks。

LOW_RANK:
已经召回全部 relevant chunks，但第一个命中排名大于 1。
```

bad cases 用于定位检索链路问题，不等同于最终问答质量评估。

## 5. Summary 解读

`GET /api/evaluation/reports/{reportId}/summary` 会在基础 report 指标上补充：

```text
badCaseCount
noHitCount
lowRecallCount
lowRankCount
```

如果 `badCaseCount` 较高，优先查看 bad cases 中的 `failureReason` 和 `retrievedChunkIds`。

## 6. 如何根据 Bad Cases 优化

如果 `NO_HIT` 较多：

- 检查 `relevantChunkIds` 是否标注了当前 active chunk。
- 检查文档是否已经 index。
- 检查 query 是否与文档事实表达差异过大。
- 尝试使用真实 embedding 或调整 embedding 模型。

如果 `LOW_RECALL` 较多：

- 检查一个问题是否横跨多个 chunk。
- 适当增加 `topK`。
- 调整 `APP_CHUNK_SIZE` 或 `APP_CHUNK_OVERLAP`，减少事实被切断的概率。

如果 `LOW_RANK` 较多：

- 检查 chunk 是否过长，导致主题混杂。
- 检查问题是否过宽泛。
- 比较 mock embedding 与 Qwen embedding 的排序差异。
- 后续可以考虑 reranker，但 Phase 6.4 不实现 reranker。

## 7. Mock 与 Qwen 差异

默认测试和 demo 脚本假设使用 mock provider，不依赖真实 API key。mock embedding 是确定性的，适合验证流程可跑通，但不能代表真实语义检索质量。

如果切换到 Qwen：

```text
APP_EMBEDDING_PROVIDER=qwen
APP_EMBEDDING_DIMENSION=1024
DASHSCOPE_API_KEY=<local-only>
```

Qwen key 只放本地 `.env`、环境变量或 IDEA Run Configuration，不提交到 Git。切换维度时需要确保 Qdrant collection 的 vector size 与 embedding dimension 一致。
