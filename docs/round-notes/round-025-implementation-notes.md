# Round 025 Implementation Notes

## 1. 本轮定位

本轮执行 Phase 6.1：`evaluation_dataset` / `evaluation_question` 表与评测集导入基础。

目标是建立 RAG 检索评测的数据基础，让系统可以创建评测集、录入单条评测问题、批量导入评测问题，并记录人工标注的相关 chunk。后续 Recall@K / HitRate@K / MRR 会基于这些数据计算。

本轮不运行评测、不计算指标、不创建 `evaluation_report`。

## 2. 本轮完成

- 新增 `evaluation_dataset` 表。
- 新增 `evaluation_question` 表。
- 新增 EvaluationDataset Entity、Mapper、DTO、Service 和 Controller。
- 新增 EvaluationQuestion Entity、Mapper、DTO、Service 和 Controller。
- 新增评测集创建、列表、详情接口。
- 新增评测问题单条创建、批量导入、列表、详情接口。
- 创建 / 导入评测问题时校验 dataset 存在。
- 创建 / 导入评测问题时校验 relevantChunkIds 非空。
- 创建 / 导入评测问题时校验 relevant chunks 存在、active，且属于 dataset 对应的 knowledgeBaseId。
- 创建 / 导入评测问题时写入 `relevant_chunk_ids_json`。
- 创建 / 导入评测问题时从关系库 active chunk 读取并写入 `relevant_content_hashes_json`。
- relevant chunks 的 `processingVersion` 全部一致时写入 `document_processing_version`，不一致时保留为空。
- 创建 / 导入评测问题后更新 `evaluation_dataset.question_count`。
- 新增 Phase 6.1 相关 HTTP 集成测试。
- 更新 README。

## 3. 新增表

`evaluation_dataset`：

```text
id
name
knowledge_base_id
description
question_count
created_at
```

索引：

```text
idx_evaluation_dataset_knowledge_base_id
```

`evaluation_question`：

```text
id
dataset_id
question
ground_truth_answer
relevant_chunk_ids_json
relevant_content_hashes_json
document_processing_version
question_type
created_at
```

索引：

```text
idx_evaluation_question_dataset_id
```

本轮没有创建 `evaluation_report` 表。

## 4. 接口列表

```text
POST /api/evaluation/datasets
GET  /api/evaluation/datasets
GET  /api/evaluation/datasets/{id}
POST /api/evaluation/datasets/{datasetId}/questions
POST /api/evaluation/datasets/{datasetId}/questions/import
GET  /api/evaluation/datasets/{datasetId}/questions
GET  /api/evaluation/questions/{id}
```

所有接口继续返回统一 `ApiResponse`。

## 5. 评测集录入链路

```text
POST /api/evaluation/datasets
  ↓
EvaluationDatasetController
  ↓
EvaluationDatasetService
  ↓
KnowledgeBaseService 校验 knowledgeBaseId
  ↓
evaluation_dataset 表
```

## 6. 评测问题录入链路

```text
POST /api/evaluation/datasets/{datasetId}/questions
  ↓
EvaluationQuestionController
  ↓
EvaluationQuestionService
  ↓
EvaluationDatasetService 校验 datasetId
  ↓
DocumentChunkService / Mapper 校验 relevantChunkIds
  ↓
读取 active chunk 的 contentHash 和 processingVersion
  ↓
evaluation_question 表
  ↓
evaluation_dataset.question_count + 1
```

批量导入复用同一套单条问题校验逻辑，并在同一事务内写入多条问题，最后按导入成功数量更新 `question_count`。

## 7. relevantChunkIds 规则

创建或导入评测问题时：

- `relevantChunkIds` 不能为空。
- 每个 chunkId 必须是正数。
- 每个 chunkId 必须存在。
- 每个 chunk 必须是 active chunk。
- 每个 chunk 必须属于 dataset 对应的 `knowledgeBaseId`。
- 关系库 `document_chunk` 是 chunk 标注事实源。
- `relevant_chunk_ids_json` 保存请求中的 chunkId 列表。
- `relevant_content_hashes_json` 保存对应 chunk 的 `contentHash`。

如果 chunk 不存在或已 inactive，返回清晰错误；如果 chunk 属于其他知识库，也返回清晰错误。

## 8. 第一版评测集来源

第一版评测集采用人工标注方式：

```text
先上传示例文档
  ↓
POST /api/documents/{documentId}/process 生成 chunks
  ↓
GET /api/documents/{documentId}/chunks 查看 chunk
  ↓
人工选择 relevantChunkIds
  ↓
写入 evaluation_question
```

本轮没有实现公开数据集自动导入，也没有声明已经支持公开数据集自动导入。

## 9. 逻辑可读性检查

- EvaluationDatasetController 只负责 dataset HTTP 入口。
- EvaluationQuestionController 只负责 question HTTP 入口。
- EvaluationDatasetService 只负责 dataset 数据访问和 knowledgeBaseId 基础校验。
- EvaluationQuestionService 只负责 question 创建、导入、chunk 标注校验和 contentHash 记录。
- DocumentChunkService 仍是关系库 chunk 事实数据入口。
- 本轮没有把评测运行逻辑混进 evaluation service。
- 本轮没有把指标计算逻辑混进 evaluation service。

## 10. 测试覆盖

新增 `EvaluationControllerTest`，覆盖：

- 创建 evaluation dataset 成功。
- knowledgeBaseId 不存在时创建 dataset 失败。
- knowledgeBaseId 缺失时创建 dataset 失败。
- 创建 evaluation question 成功。
- relevantChunkIds 为空时创建 question 失败。
- relevantChunkIds 不存在时创建 question 失败。
- relevant chunk inactive 时创建 question 失败。
- relevant chunk 不属于 dataset 的 knowledgeBaseId 时创建 question 失败。
- 创建 question 后 dataset.questionCount 更新。
- 批量导入 questions 成功。
- 查询 dataset 下 question 列表成功。
- 查询不存在 question 返回清晰错误。

测试继续使用 H2、Mock Embedding 和 Mock LLM，不依赖 Docker，不依赖真实 Qwen key。

## 11. 本轮刻意不做

- 不创建 `evaluation_report` 表。
- 不运行批量检索评测。
- 不计算 Recall@K。
- 不计算 HitRate@K。
- 不计算 MRR。
- 不做 bad cases 分析。
- 不做公开数据集自动导入。
- 不做 LLM-as-judge。
- 不做答案质量评测。
- 不做 reranker。
- 不引入 Redis / Elasticsearch / RabbitMQ。

## 12. 下一轮建议

进入 Phase 6.2：检索评测运行与 Recall@K / HitRate@K / MRR 计算。
