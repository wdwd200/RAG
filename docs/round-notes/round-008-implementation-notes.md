# Round 008 Implementation Notes

## 1. 本轮完成了什么

本轮执行 Phase 2.3：文件类型、大小限制与删除一致性。

在已有文件上传能力基础上，本轮新增了配置化上传限制，并调整 document 删除流程：删除 document 时会先尝试删除本地 storage 文件，再删除数据库记录。

## 2. 新增或修改文件

- `src/main/resources/application.yml`
- `src/test/resources/application-test.yml`
- `src/main/java/com/example/ragbackend/RagBackendApplication.java`
- `src/main/java/com/example/ragbackend/infrastructure/storage/StorageProperties.java`
- `src/main/java/com/example/ragbackend/infrastructure/storage/LocalFileStorageService.java`
- `src/main/java/com/example/ragbackend/document/service/impl/DocumentServiceImpl.java`
- `src/test/java/com/example/ragbackend/document/DocumentControllerTest.java`
- `README.md`
- `docs/round-notes/round-008-implementation-notes.md`

## 3. 新增配置项

```yaml
app:
  storage:
    local-root: ${APP_STORAGE_LOCAL_ROOT:storage/documents}
    allowed-extensions: ${APP_STORAGE_ALLOWED_EXTENSIONS:txt,md,pdf,docx}
    max-file-size-bytes: ${APP_STORAGE_MAX_FILE_SIZE_BYTES:10485760}
```

说明：

- `local-root`：本地文件保存根目录。
- `allowed-extensions`：允许上传的文件后缀，默认 `txt,md,pdf,docx`。
- `max-file-size-bytes`：单文件最大大小，默认 10MB。

测试环境使用 `target/test-storage/documents`，并把最大文件大小设为 20 bytes，方便覆盖超限场景。

## 4. 上传校验调用链

```text
POST /api/documents/upload
  ↓
DocumentController
  ↓
DocumentService
  ↓
LocalFileStorageService
  ↓
文件类型 / 文件大小校验
  ↓
保存文件
  ↓
DocumentMapper
  ↓
document 表
```

上传保存前会校验：

- 文件不能为空。
- 文件名不能为空。
- 文件后缀必须在 `allowed-extensions` 中。
- 文件大小不能超过 `max-file-size-bytes`。

校验失败时不会保存文件，也不会创建 document 记录。

## 5. 删除调用链

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

删除 document 时先读取 document 记录。如果记录不存在，返回 `DOCUMENT_NOT_FOUND`。

如果存在 `storagePath`，会先删除本地文件，再删除数据库记录。本地文件已不存在时不会导致删除失败；如果文件删除过程发生异常，则不会继续删除数据库记录，避免文件残留但元数据已删除。

## 6. 核心文件职责

`StorageProperties.java`：绑定 `app.storage` 配置，集中管理本地路径、允许后缀和最大文件大小。

`LocalFileStorageService.java`：负责上传文件校验、安全文件名处理、保存文件、路径穿越防护和本地文件删除。

`DocumentServiceImpl.java`：负责 document 业务流程，上传时协调 storage 和数据库写入，删除时协调本地文件删除和数据库记录删除。

`DocumentControllerTest.java`：覆盖上传限制和删除一致性场景。

## 7. 如何运行和验证

运行测试：

```bash
mvn test
```

检查 diff：

```bash
git diff --check
```

手动上传允许类型文件：

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "knowledgeBaseId=1" \
  -F "file=@/path/to/demo.md"
```

手动删除 document：

```bash
curl -X DELETE http://localhost:8080/api/documents/1
```

## 8. 本轮测试覆盖

- 上传 txt 成功。
- 上传 md 成功。
- 上传不允许后缀失败。
- 上传超过大小限制失败。
- 上传空文件失败。
- 删除 document 时同步删除本地文件。
- 本地文件已不存在时，删除 document 仍可成功。

## 9. 本轮刻意不做

- 不做文档解析。
- 不做 `document_chunk` 表。
- 不做 chunk 切分。
- 不做 embedding。
- 不接入 Qdrant Java 客户端。
- 不做向量检索。
- 不接入 LLM。
- 不做 SSE。
- 不做 Redis / Elasticsearch / RabbitMQ。
- 不做 Spring Security / JWT。

## 10. 下一轮建议

进入 Phase 2.4：Phase 2 收尾、接口验证与文档上传链路导读。
