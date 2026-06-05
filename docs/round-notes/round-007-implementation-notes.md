# Round 007 Implementation Notes

## 1. 本轮完成了什么

本轮执行 Phase 2.2：文件上传接口与本地 storage。

本轮在已有 document 元数据模块基础上，新增 multipart 上传接口，将原始文件保存到本地 storage，并创建 document 记录。

Phase 2.2.1 仅整理 Phase 2.2 上传模块代码格式、空行和文档可读性，没有新增业务功能。

本轮不做文件类型白名单、不做文件大小限制、不做删除 document 时同步删除本地文件、不做文档解析、不做 chunk、不做 embedding、不接入 Qdrant、不做向量检索、不接入 LLM。

## 2. 新增或修改文件

- `src/main/resources/application.yml`
- `src/test/resources/application-test.yml`
- `.gitignore`
- `src/main/java/com/example/ragbackend/infrastructure/storage/FileStorageService.java`
- `src/main/java/com/example/ragbackend/infrastructure/storage/LocalFileStorageService.java`
- `src/main/java/com/example/ragbackend/infrastructure/storage/StoredFile.java`
- `src/main/java/com/example/ragbackend/document/controller/DocumentController.java`
- `src/main/java/com/example/ragbackend/document/service/DocumentService.java`
- `src/main/java/com/example/ragbackend/document/service/impl/DocumentServiceImpl.java`
- `src/test/java/com/example/ragbackend/document/DocumentControllerTest.java`
- `README.md`
- `docs/round-notes/round-007-implementation-notes.md`

## 3. 新增配置

新增本地 storage 配置：

```yaml
app:
  storage:
    local-root: ${APP_STORAGE_LOCAL_ROOT:storage/documents}
```

说明：

- 默认路径用于本地开发。
- 可通过 `APP_STORAGE_LOCAL_ROOT` 覆盖。
- `.gitignore` 已忽略 `storage/`，避免本地上传文件进入仓库。
- 测试环境使用 `target/test-storage/documents`。

## 4. 本轮接口

```text
POST /api/documents/upload
```

请求参数：

```text
knowledgeBaseId
file
createdBy，可选，默认 1
```

行为：

- 校验 `knowledgeBaseId` 是否存在。
- 拒绝空文件。
- 清理原始文件名，避免路径穿越。
- 保存原始文件到本地 storage。
- 创建 document 记录。
- document 状态为 `UPLOADED`。

## 5. 上传调用链

```text
POST /api/documents/upload
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

如果文件保存成功但数据库写入失败，`DocumentService` 会调用 `FileStorageService.delete(...)` 尽量清理刚保存的文件，避免明显脏文件。

## 6. 核心文件职责

`FileStorageService.java`：定义文件保存和删除接口。

`LocalFileStorageService.java`：实现本地文件保存、目录创建、安全文件名处理、空文件拒绝和路径穿越防护。

`StoredFile.java`：封装保存后的文件信息，包括原始文件名、storage path、文件大小和文件类型。

`DocumentController.java`：新增 `POST /api/documents/upload`，只负责接收 multipart 参数并调用 Service。

`DocumentService.java`：新增 `upload` 方法。

`DocumentServiceImpl.java`：实现知识库校验、文件保存、document 记录创建和写库失败清理文件。

`DocumentControllerTest.java`：覆盖上传成功、上传后字段正确、空文件失败和知识库不存在上传失败。

## 7. 如何验证

运行测试：

```bash
mvn test
```

启动应用后上传文件：

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "knowledgeBaseId=1" \
  -F "createdBy=1" \
  -F "file=@/path/to/demo.pdf"
```

查询文档记录：

```bash
curl http://localhost:8080/api/documents/1
```

## 8. 测试覆盖

新增或更新测试覆盖：

- 上传文件成功。
- 上传后 document 状态为 `UPLOADED`。
- 上传后 document 记录包含 `fileName`、`fileType`、`fileSize`、`storagePath`。
- 空文件上传失败。
- 不存在的 `knowledgeBaseId` 上传失败。

测试使用 H2 和 `target/test-storage/documents`，不依赖 Docker，也不会留下大量临时文件。

## 9. 本轮刻意不做

- 不做文件类型白名单。
- 不做文件大小限制。
- 不做删除 document 时同步删除本地文件。
- 不做文档解析。
- 不做 `document_chunk` 表。
- 不做 chunk 切分。
- 不做 embedding。
- 不做 Qdrant Java 客户端。
- 不做向量检索。
- 不做 LLM。
- 不做 SSE。
- 不做 Redis / Elasticsearch / RabbitMQ。
- 不做 Spring Security / JWT。

## 10. 下一轮建议

进入 Phase 2.3：文件类型、大小限制与删除一致性。
