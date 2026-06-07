# Sample Evaluation Dataset

## 1. 目标

本示例评测集用于验证 Phase 6 的检索评测链路是否能稳定跑通：

```text
evaluation dataset
  -> evaluation questions
  -> retrieval evaluation run
  -> report
  -> summary
  -> bad cases
```

评测问题只检查检索是否召回正确 chunk，不评估 LLM 回答质量。

## 2. 示例文档覆盖

示例问题覆盖 3 份固定文档：

```text
docs/demo/sample-documents/hr-handbook.md
docs/demo/sample-documents/expense-policy.md
docs/demo/sample-documents/engineering-guide.md
```

每份文档都包含若干明确事实，方便人工标注 relevant chunk。

## 3. 问题与标准答案

问题模板位于：

```text
docs/demo/sample-evaluation-questions.json
```

模板中每条问题包含：

```text
question
groundTruthAnswer
relevantChunkIds
questionType
sourceHint
```

`groundTruthAnswer` 是人工可读的标准答案说明；当前检索评测不会调用 LLM，也不会根据 answer 打分。

## 4. 如何人工选择 relevantChunkIds

第一版 relevant chunk 标注需要人工完成：

1. 上传示例文档。
2. 调用 `POST /api/documents/{documentId}/process` 生成 chunks。
3. 调用 `GET /api/documents/{documentId}/chunks` 查看 chunk 列表。
4. 根据每个问题的 `sourceHint` 找到包含对应事实的 chunk。
5. 把该 chunk 的 `id` 写入 `relevantChunkIds`。

查询 chunks：

```powershell
curl.exe http://localhost:8080/api/documents/<DOCUMENT_ID>/chunks
```

返回中的关键字段：

```text
id
documentId
chunkIndex
content
isActive
```

只选择 `isActive = true` 的 chunk。

## 5. 为什么不能写死 chunkId

`chunkId` 是数据库运行时生成的主键。

不同数据库、不同演示轮次、不同清库方式、不同文档处理顺序都可能产生不同的 `chunkId`。即使示例文档内容固定，运行时 ID 也不应被当作固定值。

因此本示例提供的是：

```text
问题 + 标准答案 + sourceHint + chunkId 占位符
```

而不是假装某个 chunkId 永远固定。

## 6. 如何把模板改成接口可接受的导入 JSON

`sample-evaluation-questions.json` 是人工标注模板，不是直接导入文件。

导入前需要做两件事：

1. 把 `REPLACE_WITH_..._CHUNK_ID` 替换为真实数字 chunkId。
2. 移除辅助字段 `sourceHint`。

接口实际接受的格式如下：

```json
{
  "questions": [
    {
      "question": "员工年假最晚可以累计到什么时候？",
      "groundTruthAnswer": "未使用的年假最晚可以累计到下一年度第一季度末，即下一年 3 月 31 日。",
      "relevantChunkIds": [123],
      "questionType": "fact"
    }
  ]
}
```

导入命令：

```powershell
curl.exe -X POST http://localhost:8080/api/evaluation/datasets/<DATASET_ID>/questions/import `
  -H "Content-Type: application/json" `
  --data-binary "@docs/demo/sample-evaluation-questions.import.json"
```

`sample-evaluation-questions.import.json` 是你本地替换 chunkId 后的临时文件，不需要提交到 Git。

## 7. 推荐标注策略

- 如果一个 chunk 同时包含多个相关事实，可以让多个问题引用同一个 chunkId。
- 如果一个问题的答案横跨多个 chunk，可以在 `relevantChunkIds` 中填入多个 chunkId。
- 如果示例文档被重新处理，旧 chunk 会变成 inactive，需要重新查询 active chunks 并更新标注。
- 如果调整 `APP_CHUNK_SIZE` 或 `APP_CHUNK_OVERLAP`，chunk 边界可能变化，需要重新标注。
