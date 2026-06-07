# Phase 6 Demo Guide

本指南用于跑通 Phase 6 检索评测 demo。命令按 Windows PowerShell 编写，HTTP 命令使用 `curl.exe`，避免 PowerShell 自带 alias 差异。

第一版评测集需要人工选择 `relevantChunkIds`。`chunkId` 是数据库运行时生成的，不同本地环境和不同演示轮次可能不同。

## 1. 启动 Docker Compose

在项目根目录执行：

```powershell
docker compose up -d
docker compose ps
```

默认会启动 PostgreSQL 和 Qdrant。

## 2. 启动应用

默认 demo 使用 mock embedding 和 mock LLM，不需要真实 API key。

```powershell
mvn clean package
java -jar target/rag-backend-0.0.1-SNAPSHOT.jar
```

如果 Maven 没在 PATH 中，请使用本机 Maven 的完整路径启动。

验证应用健康：

```powershell
curl.exe http://localhost:8080/api/health
curl.exe http://localhost:8080/api/health/qdrant
```

## 3. 创建知识库

```powershell
$kbBody = @{
  name = "Phase 6 Demo KB"
  description = "Demo knowledge base for retrieval evaluation"
  ownerId = 1
  visibility = "PRIVATE"
} | ConvertTo-Json

$kb = curl.exe -s -X POST http://localhost:8080/api/knowledge-bases `
  -H "Content-Type: application/json" `
  -d $kbBody | ConvertFrom-Json

$kbId = $kb.data.id
$kbId
```

后续命令使用 `$kbId`。

## 4. 上传示例文档

示例文档位置：

```text
docs/demo/sample-documents/hr-handbook.md
docs/demo/sample-documents/expense-policy.md
docs/demo/sample-documents/engineering-guide.md
```

上传：

```powershell
$hr = curl.exe -s -X POST http://localhost:8080/api/documents/upload `
  -F "knowledgeBaseId=$kbId" `
  -F "createdBy=1" `
  -F "file=@docs/demo/sample-documents/hr-handbook.md" | ConvertFrom-Json

$expense = curl.exe -s -X POST http://localhost:8080/api/documents/upload `
  -F "knowledgeBaseId=$kbId" `
  -F "createdBy=1" `
  -F "file=@docs/demo/sample-documents/expense-policy.md" | ConvertFrom-Json

$engineering = curl.exe -s -X POST http://localhost:8080/api/documents/upload `
  -F "knowledgeBaseId=$kbId" `
  -F "createdBy=1" `
  -F "file=@docs/demo/sample-documents/engineering-guide.md" | ConvertFrom-Json

$hrId = $hr.data.id
$expenseId = $expense.data.id
$engineeringId = $engineering.data.id

$hrId
$expenseId
$engineeringId
```

## 5. Process 文档

```powershell
curl.exe -s -X POST "http://localhost:8080/api/documents/$hrId/process"
curl.exe -s -X POST "http://localhost:8080/api/documents/$expenseId/process"
curl.exe -s -X POST "http://localhost:8080/api/documents/$engineeringId/process"
```

处理成功后文档状态应为 `CHUNKED`。

## 6. Index 文档

```powershell
curl.exe -s -X POST "http://localhost:8080/api/documents/$hrId/index"
curl.exe -s -X POST "http://localhost:8080/api/documents/$expenseId/index"
curl.exe -s -X POST "http://localhost:8080/api/documents/$engineeringId/index"
```

索引成功后文档状态应为 `INDEXED`。

## 7. 查询 Chunks

```powershell
$hrChunks = curl.exe -s "http://localhost:8080/api/documents/$hrId/chunks" | ConvertFrom-Json
$expenseChunks = curl.exe -s "http://localhost:8080/api/documents/$expenseId/chunks" | ConvertFrom-Json
$engineeringChunks = curl.exe -s "http://localhost:8080/api/documents/$engineeringId/chunks" | ConvertFrom-Json

$hrChunks.data | Select-Object id, documentId, chunkIndex, isActive, content
$expenseChunks.data | Select-Object id, documentId, chunkIndex, isActive, content
$engineeringChunks.data | Select-Object id, documentId, chunkIndex, isActive, content
```

人工查看 `content`，根据 `docs/demo/sample-evaluation-questions.json` 中每题的 `sourceHint` 找到对应 chunk。

只选择 `isActive = true` 的 chunk。

## 8. 创建 Evaluation Dataset

```powershell
$datasetBody = @{
  name = "Phase 6 Demo Retrieval Evaluation"
  knowledgeBaseId = $kbId
  description = "Manual labels based on docs/demo/sample-documents"
} | ConvertTo-Json

$dataset = curl.exe -s -X POST http://localhost:8080/api/evaluation/datasets `
  -H "Content-Type: application/json" `
  -d $datasetBody | ConvertFrom-Json

$datasetId = $dataset.data.id
$datasetId
```

## 9. 人工替换评测问题模板中的 ChunkId

模板文件：

```text
docs/demo/sample-evaluation-questions.json
```

这个文件不是直接导入文件，因为它包含：

```text
REPLACE_WITH_..._CHUNK_ID
sourceHint
```

请复制一份到本地临时文件：

```powershell
Copy-Item docs/demo/sample-evaluation-questions.json docs/demo/sample-evaluation-questions.import.json
```

然后人工编辑 `docs/demo/sample-evaluation-questions.import.json`：

1. 把每个 `REPLACE_WITH_..._CHUNK_ID` 替换为真实数字 chunkId，例如 `[12]`。
2. 删除每条问题中的 `sourceHint` 字段。

接口接受的单条格式示例：

```json
{
  "question": "员工年假最晚可以累计到什么时候？",
  "groundTruthAnswer": "未使用的年假最晚可以累计到下一年度第一季度末，即下一年 3 月 31 日。",
  "relevantChunkIds": [12],
  "questionType": "fact"
}
```

不要提交 `sample-evaluation-questions.import.json`。

## 10. 导入 Evaluation Questions

确认临时导入文件已经替换真实 chunkId 并移除了 `sourceHint` 后执行：

```powershell
curl.exe -X POST "http://localhost:8080/api/evaluation/datasets/$datasetId/questions/import" `
  -H "Content-Type: application/json" `
  --data-binary "@docs/demo/sample-evaluation-questions.import.json"
```

查看导入结果：

```powershell
curl.exe "http://localhost:8080/api/evaluation/datasets/$datasetId/questions"
```

## 11. 运行 Evaluation

```powershell
$runBody = @{ topK = 5 } | ConvertTo-Json

$report = curl.exe -s -X POST "http://localhost:8080/api/evaluation/datasets/$datasetId/run" `
  -H "Content-Type: application/json" `
  -d $runBody | ConvertFrom-Json

$reportId = $report.data.id
$reportId
```

## 12. 查看 Report

```powershell
curl.exe "http://localhost:8080/api/evaluation/reports/$reportId"
```

## 13. 查看 Summary

```powershell
curl.exe "http://localhost:8080/api/evaluation/reports/$reportId/summary"
```

重点查看：

```text
recallAtK
hitRateAtK
mrr
badCaseCount
noHitCount
lowRecallCount
lowRankCount
```

## 14. 查看 Bad Cases

```powershell
curl.exe "http://localhost:8080/api/evaluation/reports/$reportId/bad-cases"
```

`failureReason` 含义：

```text
NO_HIT = topK 内没有命中任何 relevant chunk
LOW_RECALL = 已命中，但 Recall@K < 1
LOW_RANK = 已全部召回，但第一个命中排名大于 1
```

## 可选脚本

可选脚本：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/demo/phase6-demo.ps1
```

脚本会自动创建知识库、上传文档、process、index 并打印 chunks，然后停在人工标注阶段。它不会自动替换 `relevantChunkIds`，也不会自动导入评测问题。

## Mock 与 Qwen

默认 demo 使用 mock embedding，不需要真实 API key。

如果切换到 Qwen embedding：

```text
APP_EMBEDDING_PROVIDER=qwen
APP_EMBEDDING_DIMENSION=1024
QWEN_EMBEDDING_MODEL=text-embedding-v4
DASHSCOPE_API_KEY=<local-only>
```

真实 `DASHSCOPE_API_KEY` 只允许放在本地 `.env`、环境变量或 IDEA Run Configuration 中，不提交到 Git。
