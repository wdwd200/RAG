# Round 028 Implementation Notes

## 1. 本轮定位

本轮执行 Phase 6.4：示例知识库、示例评测集与一键演示流程。

本轮目标是补齐 Phase 6 评测链路的可复现 demo 资产，让新用户可以按照固定示例文档和操作指南跑通：

```text
创建知识库
  ↓
上传示例文档
  ↓
process
  ↓
index
  ↓
查看 chunks
  ↓
创建 evaluation dataset
  ↓
导入 evaluation questions
  ↓
运行 evaluation
  ↓
查看 report summary / bad cases
```

本轮不新增复杂后端功能，不修改评测指标定义，不做公开数据集自动导入。

## 2. 本轮新增文件

示例文档：

```text
docs/demo/sample-documents/hr-handbook.md
docs/demo/sample-documents/expense-policy.md
docs/demo/sample-documents/engineering-guide.md
```

示例评测集与报告说明：

```text
docs/demo/sample-evaluation-dataset.md
docs/demo/sample-evaluation-questions.json
docs/demo/sample-evaluation-report.md
docs/demo/phase-006-demo-guide.md
```

可选脚本：

```text
scripts/demo/README.md
scripts/demo/phase6-demo.ps1
```

其他更新：

```text
README.md
.gitignore
```

`.gitignore` 新增 `docs/demo/*.import.json`，用于避免人工替换 chunkId 后的本地临时导入文件误提交。

## 3. 示例知识库文档

本轮新增 3 份固定 Markdown 文档：

```text
hr-handbook.md:
年假规则、病假规则、试用期规则、加班调休规则。

expense-policy.md:
差旅报销材料、发票要求、报销时限、审批流程。

engineering-guide.md:
分支命名规范、commit 规范、code review 规则、发布流程。
```

这些文档是虚构示例，不包含真实公司内部信息、真实财务规则或敏感数据。内容保持短小稳定，便于生成 chunk 和人工标注 relevant chunk。

## 4. 示例评测集如何构造

评测问题模板位于：

```text
docs/demo/sample-evaluation-questions.json
```

模板提供至少 10 条问题，覆盖 3 份示例文档。每条问题包含：

```text
question
groundTruthAnswer
relevantChunkIds
questionType
sourceHint
```

其中 `sourceHint` 是人工定位字段，用于提示该问题对应哪份文档和哪个段落。

当前后端导入接口接受的字段是：

```text
question
groundTruthAnswer
relevantChunkIds
questionType
```

因此模板不是直接导入文件。导入前必须：

```text
1. 把 REPLACE_WITH_..._CHUNK_ID 替换为本地真实数字 chunkId。
2. 删除 sourceHint 字段。
```

## 5. 为什么 chunkId 需要运行后人工标注

`chunkId` 是关系数据库运行时生成的主键。

即使示例文档内容固定，以下因素仍会导致 chunkId 不同：

```text
数据库是否清空
此前是否已有 document / chunk 数据
文档上传顺序
文档是否重复 process
chunkSize / overlap 配置
不同本地演示轮次
```

所以示例评测集不能静态写死 chunkId。第一版采用更真实也更清楚的方式：

```text
固定问题
固定 groundTruthAnswer
固定 sourceHint
运行后查询 active chunks
人工选择 relevantChunkIds
```

这也符合当前系统的真实使用方式：评测集标注需要基于实际入库后的 chunk 结果。

## 6. Demo Guide

完整 demo 操作文档：

```text
docs/demo/phase-006-demo-guide.md
```

它包含 Windows PowerShell 风格的 `curl.exe` 流程：

```text
1. 启动 docker compose
2. 启动应用
3. 创建知识库
4. 上传示例文档
5. process 文档
6. index 文档
7. 查询 chunks
8. 创建 evaluation dataset
9. 根据 chunks 人工替换 sample-evaluation-questions.json 中的 chunkId
10. 导入 evaluation questions
11. 运行 evaluation
12. 查看 report
13. 查看 summary
14. 查看 bad cases
```

文档明确标出了人工替换点，不声称 demo 完全零人工。

## 7. 可选脚本

新增：

```text
scripts/demo/phase6-demo.ps1
```

脚本自动化到以下步骤：

```text
创建知识库
上传 3 份示例文档
process 文档
index 文档
查询并打印 chunks
```

脚本随后停在人工标注阶段。它不会自动替换 `relevantChunkIds`，不会自动导入评测问题，也不会写死真实 API key。

默认脚本使用本地应用地址：

```text
http://localhost:8080
```

可以通过参数覆盖：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/demo/phase6-demo.ps1 -BaseUrl "http://localhost:8080"
```

## 8. 可复现性说明

自动测试：

```text
mvn test 使用 H2、mock provider 和固定测试数据。
mvn test 不依赖 Docker。
mvn test 不依赖 Qdrant。
mvn test 不依赖真实 embedding。
mvn test 不依赖真实 LLM。
```

真实链路 demo：

```text
依赖 Docker，因为需要 PostgreSQL 和 Qdrant。
使用 mock provider 时不依赖真实 API key。
使用 Qwen embedding 或 Qwen LLM 时依赖本地 DASHSCOPE_API_KEY。
真实 Qwen key 只放在本地 .env、环境变量或 IDEA Run Configuration 中，不提交到 Git。
```

评测结果可复现性取决于：

```text
示例文档是否固定
chunkSize / overlap 是否固定
embedding provider 和 dimension 是否固定
Qdrant collection 是否固定
relevantChunkIds 是否对应当前 active chunks
topK 是否固定
```

## 9. README 更新

README 新增 Phase 6 Demo 入口，说明：

```text
示例文档位置
示例评测问题模板位置
demo guide 位置
示例报告说明位置
可选脚本位置
自动测试和真实链路 demo 的区别
relevantChunkIds 需要人工根据 chunks 选择
```

README 只保留入口和简要说明，完整操作步骤放在 `docs/demo/phase-006-demo-guide.md`。

## 10. 本轮刻意不做

- 不新增复杂后端功能。
- 不修改 Recall@K / HitRate@K / MRR 定义。
- 不做公开数据集自动导入。
- 不做 LLM-as-judge。
- 不做答案质量评测。
- 不做异步评测任务。
- 不做 reranker。
- 不引入 Redis / Elasticsearch / RabbitMQ。
- 不提交真实 API key。

## 11. 验证方式

自动化验证：

```bash
mvn test
git diff --check
```

PowerShell 脚本基础语法检查：

```powershell
$script = Get-Content -Raw scripts/demo/phase6-demo.ps1
[scriptblock]::Create($script) | Out-Null
```

完整 demo 验证需要本地启动 Docker、Qdrant、PostgreSQL 和应用，然后按 `docs/demo/phase-006-demo-guide.md` 执行。

## 12. 下一轮建议

进入 Phase 6.5：工程化收尾、项目文档、简历材料与可选增强规划。

下一轮重点建议放在交付整理、项目边界说明、演示材料、简历可描述成果和后续增强路线，不再继续扩大 Phase 6 的后端功能面。
