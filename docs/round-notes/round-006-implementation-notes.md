# Round 006 Implementation Notes

## 1. 本轮完成了什么

本轮执行 Phase 2.1：`document` 表、文档状态枚举与文档元数据基础。

本轮只实现文档元数据管理，不实现真实文件上传、不接收 multipart、不做本地文件存储、不解析文档、不生成 chunk、不做 embedding，也不接入 Qdrant 或 LLM。

## 2. 新增或修改文件

- `src/main/resources/db/migration/V3__create_document_table.sql`
- `src/main/java/com/example/ragbackend/config/MybatisPlusConfig.java`
- `src/main/java/com/example/ragbackend/document/enums/DocumentStatus.java`
- `src/main/java/com/example/ragbackend/document/entity/Document.java`
- `src/main/java/com/example/ragbackend/document/mapper/DocumentMapper.java`
- `src/main/java/com/example/ragbackend/document/dto/DocumentCreateRequest.java`
- `src/main/java/com/example/ragbackend/document/dto/DocumentResponse.java`
- `src/main/java/com/example/ragbackend/document/service/DocumentService.java`
- `src/main/java/com/example/ragbackend/document/service/impl/DocumentServiceImpl.java`
- `src/main/java/com/example/ragbackend/document/controller/DocumentController.java`
- `src/test/java/com/example/ragbackend/document/DocumentControllerTest.java`
- `src/test/java/com/example/ragbackend/knowledge/KnowledgeBaseControllerTest.java`
- `src/test/java/com/example/ragbackend/knowledge/KnowledgeBasePersistenceTest.java`
- `README.md`
- `docs/round-notes/round-006-implementation-notes.md`

## 3. 数据库变更

新增 `document` 表：

```text
id
knowledge_base_id
file_name
file_type
file_size
storage_path
status
chunk_count
processing_version
failed_stage
error_message
created_by
created_at
updated_at
```

新增索引：

```text
idx_document_knowledge_base_id
```

本轮增加了 `document.knowledge_base_id` 到 `knowledge_base.id` 的外键，用于保证文档元数据必须归属到已存在的知识库。

## 4. 文档状态

新增 `DocumentStatus`：

```text
UPLOADED
PARSING
PARSED
CHUNKING
CHUNKED
EMBEDDING
INDEXING
INDEXED
FAILED
```

新建文档元数据默认状态为 `UPLOADED`。

## 5. 本轮接口

```text
POST   /api/documents
GET    /api/documents/{id}
GET    /api/knowledge-bases/{knowledgeBaseId}/documents
DELETE /api/documents/{id}
```

说明：

- `POST /api/documents` 只创建文档元数据。
- 不接收 multipart 文件。
- 不实现 `/api/documents/upload`。
- 不实现 `/api/documents/{id}/process`。

## 6. 调用链

```text
HTTP Request
  ↓
DocumentController
  ↓
DocumentService
  ↓
DocumentMapper
  ↓
document 表
```

创建文档元数据时，Service 会先校验 `knowledgeBaseId` 是否存在：

```text
DocumentService
  ↓
KnowledgeBaseService.existsById()
  ↓
knowledge_base 表
```

## 7. 核心文件职责

`V3__create_document_table.sql`：创建 `document` 表、外键和 `knowledge_base_id` 索引。

`DocumentStatus.java`：定义文档处理生命周期状态。

`Document.java`：映射 `document` 表。

`DocumentMapper.java`：基于 MyBatis-Plus 访问 `document` 表。

`DocumentCreateRequest.java`：创建文档元数据的请求 DTO。

`DocumentResponse.java`：文档元数据响应 DTO。

`DocumentService.java`：定义文档元数据创建、查询、状态更新和删除能力。

`DocumentServiceImpl.java`：实现文档元数据业务逻辑，并处理知识库不存在、文档不存在等业务错误。

`DocumentController.java`：暴露文档元数据 HTTP API。

`DocumentControllerTest.java`：覆盖文档元数据创建、查询、列表、删除和错误场景。

## 8. 如何验证

运行测试：

```bash
mvn test
```

创建文档元数据：

```bash
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{"knowledgeBaseId":1,"fileName":"demo.pdf","fileType":"pdf","fileSize":1024,"storagePath":"documents/demo.pdf","createdBy":1}'
```

查询文档元数据：

```bash
curl http://localhost:8080/api/documents/1
```

查询知识库下的文档列表：

```bash
curl http://localhost:8080/api/knowledge-bases/1/documents
```

## 9. 测试覆盖

新增测试覆盖：

- 创建文档元数据成功，状态为 `UPLOADED`。
- 根据 ID 查询文档。
- 根据 `knowledgeBaseId` 查询文档列表。
- 删除文档元数据。
- `knowledgeBaseId` 不存在时创建失败。
- 文档不存在时查询失败。

## 10. 本轮刻意不做

- 不做真实文件上传。
- 不做 multipart。
- 不做本地 storage。
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

## 11. 下一轮建议

进入 Phase 2.2：文件上传接口与本地 storage。
