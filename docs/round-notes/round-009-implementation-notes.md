# Round 009 Implementation Notes

## 1. 本轮是什么

本轮执行 Phase 2.4：Phase 2 收尾、接口验证与文档上传链路导读。

本轮没有新增大功能，重点是确认 Phase 2 的 document 元数据和文件上传链路可运行、可验证、可讲清楚。

手动验证时发现 Spring multipart 默认限制会早于业务文件大小校验拦截大文件。本轮补充了 multipart 上限配置和统一异常处理，保证超过业务大小限制时能返回清晰的 `FILE_TOO_LARGE`。

## 2. 本轮新增或修改文件

- `README.md`
- `docs/phase-notes/phase-002-summary.md`
- `docs/round-notes/round-009-implementation-notes.md`

## 3. 本轮验证的接口

本轮已通过 jar 启动后的手动验证，验证范围：

- `GET /api/health`
- `GET /api/health/database`
- `POST /api/knowledge-bases`
- `POST /api/documents/upload`
- `GET /api/documents/{id}`
- `GET /api/knowledge-bases/{knowledgeBaseId}/documents`
- `DELETE /api/documents/{id}`

上传验证范围：

- txt 上传成功。
- md 上传成功。
- 不允许的后缀上传失败。
- 超过大小限制上传失败。
- 删除 document 后本地文件被删除。

手动验证结果：

- `GET /api/health`：通过。
- `GET /api/health/database`：通过。
- `POST /api/knowledge-bases`：通过。
- `POST /api/documents/upload`：txt 通过，md 通过。
- `GET /api/documents/{id}`：通过。
- `GET /api/knowledge-bases/{knowledgeBaseId}/documents`：通过。
- `DELETE /api/documents/{id}`：通过，并确认对应本地文件已删除。
- 不允许后缀上传：返回 `FILE_TYPE_NOT_ALLOWED`。
- 超过大小限制上传：返回 `FILE_TOO_LARGE`。

## 4. 上传链路

```text
HTTP multipart request
  ↓
DocumentController
  ↓
DocumentService
  ↓
LocalFileStorageService
  ↓
DocumentMapper
  ↓
document 表
```

`DocumentController` 只负责接收 multipart 参数并调用 Service。`DocumentService` 是主业务编排位置，先校验知识库，再调用 storage 保存文件，最后创建 document 记录。

## 5. 删除链路

```text
DELETE /api/documents/{id}
  ↓
DocumentController
  ↓
DocumentService
  ↓
FileStorageService.delete
  ↓
DocumentMapper.delete
```

删除 document 时，Service 先查 document，再删除本地文件，最后删除数据库记录。

## 6. 当前代码主线是否清楚

当前主线清楚：

- Controller 保持薄，只做 HTTP 入参与响应包装。
- Service 负责文档业务流程编排。
- Storage 只负责文件校验、保存和删除。
- Mapper 只负责数据库访问。

本轮未做业务代码重构。

## 7. 本轮没有做什么

- 未实现文档解析。
- 未实现 `document_chunk` 表。
- 未实现 chunk 切分。
- 未实现 embedding。
- 未实现 Qdrant Java 客户端。
- 未实现向量检索。
- 未实现 LLM。
- 未实现 SSE。

## 8. 下一轮建议

进入 Phase 3.1：`document_chunk` 表、`DocumentParser` 抽象与 txt/md 解析。
