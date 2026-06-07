# Phase 6 Demo Scripts

本目录提供 Phase 6 评测 demo 的可选辅助脚本。

## 脚本列表

```text
phase6-demo.ps1
```

该脚本尽量自动化以下步骤：

```text
创建知识库
上传 docs/demo/sample-documents 下的 3 份示例文档
process 文档
index 文档
查询每个文档的 active chunks
```

脚本会停在人工标注阶段。原因是 `relevantChunkIds` 是数据库运行时生成的，必须根据本地 `GET /api/documents/{documentId}/chunks` 返回结果人工选择。

## 前置条件

1. Docker Compose 已启动 PostgreSQL 和 Qdrant。
2. 应用已经启动在 `http://localhost:8080`。
3. 默认使用 mock embedding，不需要真实 API key。

启动基础设施：

```powershell
docker compose up -d
```

运行应用：

```powershell
mvn clean package
java -jar target/rag-backend-0.0.1-SNAPSHOT.jar
```

## 运行脚本

```powershell
powershell -ExecutionPolicy Bypass -File scripts/demo/phase6-demo.ps1
```

自定义服务地址：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/demo/phase6-demo.ps1 -BaseUrl "http://localhost:8080"
```

## 脚本不做什么

- 不写死真实 API key。
- 不依赖 Qwen。
- 不自动替换 `sample-evaluation-questions.json` 中的 chunkId。
- 不自动导入评测问题。
- 不修改数据库数据。

完整人工流程见：

```text
docs/demo/phase-006-demo-guide.md
```
