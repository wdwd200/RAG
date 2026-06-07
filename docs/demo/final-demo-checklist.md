# Final Demo Checklist

## 1. 环境准备

确认本地有：

- Java 17。
- Maven。
- Docker Desktop。
- Git。
- PowerShell。

确认真实 key 不在仓库中：

```text
.env 不提交
.env.local 不提交
.env.*.local 不提交
DASHSCOPE_API_KEY 只放本地
```

## 2. 启动 PostgreSQL / Qdrant

```powershell
docker compose up -d
docker compose ps
```

Docker 数据可能保存在本地 volume 中。如果删除 volume，会丢失 PostgreSQL / Qdrant 测试数据。

## 3. 启动应用

```powershell
mvn clean package
java -jar target/rag-backend-0.0.1-SNAPSHOT.jar
```

如果 Maven 不在 PATH 中，使用本机 Maven 的完整路径。

## 4. 运行自动测试

```powershell
mvn test
```

自动测试使用 H2、mock provider 和固定测试数据，不依赖 Docker、Qdrant、真实 embedding 或真实 LLM。

## 5. 健康检查

```powershell
curl.exe http://localhost:8080/api/health
curl.exe http://localhost:8080/api/health/database
curl.exe http://localhost:8080/api/health/qdrant
```

Swagger：

```text
http://localhost:8080/swagger-ui/index.html
```

## 6. Mock 链路 Demo

默认配置使用 mock embedding 和 mock LLM。

推荐演示链路：

```text
创建知识库
  ↓
上传 docs/demo/sample-documents 下的文档
  ↓
POST /api/documents/{id}/process
  ↓
POST /api/documents/{id}/index
  ↓
POST /api/retrieval/search
  ↓
POST /api/chat/once
  ↓
GET /api/audit/retrieval-logs/{requestId}
  ↓
GET /api/audit/llm-call-logs/{requestId}
```

可选辅助脚本：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/demo/phase6-demo.ps1
```

脚本会停在人工标注 evaluation questions 的位置。

## 7. Qwen 链路 Demo，可选

Qwen demo 会消耗真实 API 调用额度，按需执行。

本地配置示例：

```text
APP_EMBEDDING_PROVIDER=qwen
APP_EMBEDDING_DIMENSION=1024
QWEN_EMBEDDING_MODEL=text-embedding-v4
APP_LLM_PROVIDER=qwen
APP_LLM_MODEL=qwen-plus
DASHSCOPE_API_KEY=<local-only>
```

真实 `DASHSCOPE_API_KEY` 只放本地 `.env`、环境变量或 IDEA Run Configuration，不提交到 Git。

注意 Qdrant collection vector size 必须和 embedding dimension 一致。mock 384 维和 Qwen 1024 维建议使用不同 collection name。

## 8. Phase 6 Evaluation Demo

完整步骤见：

```text
docs/demo/phase-006-demo-guide.md
```

关键流程：

```text
创建 evaluation dataset
  ↓
查询 chunks
  ↓
人工替换 sample-evaluation-questions.json 中的 chunkId
  ↓
删除 sourceHint 生成本地 import JSON
  ↓
导入 evaluation questions
  ↓
运行 evaluation
  ↓
查看 report / summary / bad cases
```

不要提交 `docs/demo/*.import.json`。

## 9. 常见问题排查

数据库连接失败：

```text
确认 docker compose ps 中 PostgreSQL 是 running。
确认 .env 或 application.yml 中 datasource 配置与 docker-compose.yml 一致。
```

Qdrant 健康检查失败：

```text
确认 Qdrant 容器 running。
确认 QDRANT_HTTP_PORT 默认是 6333。
确认 collection vector size 与 embedding dimension 一致。
```

文档 index 失败：

```text
确认文档状态已经 CHUNKED。
确认 Qdrant 可访问。
确认 APP_EMBEDDING_PROVIDER 对应 client 配置完整。
```

evaluation 导入失败：

```text
确认 relevantChunkIds 是数字。
确认 chunk 存在、active，并属于 dataset 对应 knowledgeBaseId。
确认导入 JSON 已删除 sourceHint。
```

Qwen 调用失败：

```text
确认 DASHSCOPE_API_KEY 只在本地配置且值正确。
确认 baseUrl 使用 DashScope compatible mode。
确认本地网络可以访问 DashScope。
```

## 10. 演示结束后的清理步骤

停止容器但保留数据：

```powershell
docker compose down
```

停止容器并删除 volume：

```powershell
docker compose down -v
```

删除 volume 会丢失 PostgreSQL 和 Qdrant 中的本地测试数据。执行前确认不再需要这些数据。

清理本地临时导入文件：

```powershell
Remove-Item docs/demo/*.import.json -ErrorAction SilentlyContinue
```

不要删除 `.env` 中真实 key 的同时又把它提交到 Git；`.env` 应持续保持 ignored。
