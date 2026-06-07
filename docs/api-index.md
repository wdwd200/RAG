# API Index

本文件只做当前主要接口索引，完整请求参数以 Swagger 为准：

```text
http://localhost:8080/swagger-ui/index.html
```

标记说明：

```text
[Demo Core] 核心 demo 必需接口
[Audit] 审计辅助接口
[Analysis] 分析辅助接口
```

## Health

- `GET /api/health` [Demo Core]：应用统一响应格式健康检查。
- `GET /api/health/database` [Demo Core]：检查数据库连通性。
- `GET /api/health/qdrant` [Demo Core]：检查 Qdrant 连通性并尝试初始化 collection。
- `GET /actuator/health`：Spring Boot Actuator health。

## KnowledgeBase

- `POST /api/knowledge-bases` [Demo Core]：创建知识库。
- `GET /api/knowledge-bases`：查询知识库列表。
- `GET /api/knowledge-bases/{id}`：查询单个知识库。
- `PUT /api/knowledge-bases/{id}`：更新知识库。
- `DELETE /api/knowledge-bases/{id}`：删除知识库。

## Document

- `POST /api/documents/upload` [Demo Core]：上传文档文件并创建文档记录。
- `POST /api/documents`：创建文档元数据。
- `GET /api/documents/{id}`：查询文档元数据。
- `GET /api/knowledge-bases/{knowledgeBaseId}/documents`：查询某知识库下的文档。
- `POST /api/documents/{id}/process` [Demo Core]：解析文档并生成 active chunks。
- `POST /api/documents/{id}/index` [Demo Core]：对 active chunks 生成 embedding 并写入 Qdrant。
- `DELETE /api/documents/{id}`：删除文档元数据。

## Chunk

- `GET /api/documents/{documentId}/chunks` [Demo Core]：查询文档下 active chunks，用于检查切分结果和人工标注评测问题。
- `GET /api/chunks/{id}`：查询单个 active chunk。

## Embedding / Vector / Retrieval

- `POST /api/retrieval/search` [Demo Core]：按 `knowledgeBaseId` 和问题进行向量检索，返回关系库 active chunks。
- `GET /api/health/qdrant` [Demo Core]：Qdrant collection 初始化和连通性检查。

当前没有开放独立 embedding HTTP 接口。embedding 通过文档索引、检索和 RAG 问答链路间接调用。

## Chat

- `POST /api/chat/once` [Demo Core]：一次性 RAG 问答，返回 answer、references 和 requestId。
- `POST /api/chat/stream` [Demo Core]：SSE RAG 问答，返回 retrieval、answer_delta、references、done 等事件。

## Audit

- `GET /api/audit/retrieval-logs/{requestId}` [Audit]：按 requestId 查询检索命中日志。
- `GET /api/audit/llm-call-logs/{requestId}` [Audit]：按 requestId 查询 LLM 调用日志。

## Evaluation

- `POST /api/evaluation/datasets` [Demo Core]：创建评测集。
- `GET /api/evaluation/datasets`：查询评测集列表。
- `GET /api/evaluation/datasets/{id}`：查询评测集详情。
- `POST /api/evaluation/datasets/{datasetId}/questions`：创建单条评测问题。
- `POST /api/evaluation/datasets/{datasetId}/questions/import` [Demo Core]：批量导入评测问题。
- `GET /api/evaluation/datasets/{datasetId}/questions` [Analysis]：查询评测集下的问题。
- `GET /api/evaluation/questions/{id}`：查询单个评测问题。
- `POST /api/evaluation/datasets/{datasetId}/run` [Demo Core]：运行同步检索评测，生成 report。
- `GET /api/evaluation/reports/{reportId}` [Analysis]：查询评测 report。
- `GET /api/evaluation/reports/{reportId}/question-results` [Analysis]：查询问题级评测结果。
- `GET /api/evaluation/reports/{reportId}/summary` [Analysis]：查询 report summary 和 bad case 计数。
- `GET /api/evaluation/reports/{reportId}/bad-cases` [Analysis]：查询未命中、低召回、低排名问题。

## Demo

Demo 不是后端接口，而是文档和脚本入口：

- `docs/demo/final-demo-checklist.md` [Demo Core]：最终演示清单。
- `docs/demo/phase-006-demo-guide.md` [Demo Core]：Phase 6 evaluation demo 完整流程。
- `docs/demo/sample-documents/` [Demo Core]：固定示例文档。
- `docs/demo/sample-evaluation-questions.json` [Demo Core]：评测问题人工标注模板。
- `scripts/demo/phase6-demo.ps1`：可选 PowerShell 辅助脚本。
