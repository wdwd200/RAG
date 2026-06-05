# Round 010 Implementation Notes

## 1. 本轮完成了什么

本轮执行 Phase 3.1：`document_chunk` 表、`DocumentParser` 抽象与 txt/md 解析。

本轮完成了两条后续主线的基础设施：

- 数据库侧新增 `document_chunk` 表，用来承载后续文档切分后的 chunk 数据。
- 代码侧新增 parser 抽象和 txt/md parser，让系统具备读取本地原始文本文件并返回解析结果的能力。

本轮只做 parser 和 chunk 表基础，不做 TextSplitter，不生成 chunk，不提供 `/api/documents/{id}/process`。

## 2. 新增或修改了哪些文件

新增文件：

- `src/main/resources/db/migration/V4__create_document_chunk_table.sql`
- `src/main/java/com/example/ragbackend/chunk/entity/DocumentChunk.java`
- `src/main/java/com/example/ragbackend/chunk/mapper/DocumentChunkMapper.java`
- `src/main/java/com/example/ragbackend/chunk/service/DocumentChunkService.java`
- `src/main/java/com/example/ragbackend/chunk/service/impl/DocumentChunkServiceImpl.java`
- `src/main/java/com/example/ragbackend/chunk/dto/DocumentChunkResponse.java`
- `src/main/java/com/example/ragbackend/parser/DocumentParser.java`
- `src/main/java/com/example/ragbackend/parser/ParsedDocument.java`
- `src/main/java/com/example/ragbackend/parser/DocumentParserRegistry.java`
- `src/main/java/com/example/ragbackend/parser/UnsupportedDocumentTypeException.java`
- `src/main/java/com/example/ragbackend/parser/AbstractTextDocumentParser.java`
- `src/main/java/com/example/ragbackend/parser/TxtDocumentParser.java`
- `src/main/java/com/example/ragbackend/parser/MarkdownDocumentParser.java`
- `src/test/java/com/example/ragbackend/chunk/DocumentChunkPersistenceTest.java`
- `src/test/java/com/example/ragbackend/parser/DocumentParserTest.java`
- `docs/round-notes/round-010-implementation-notes.md`

修改文件：

- `src/main/java/com/example/ragbackend/config/MybatisPlusConfig.java`
- `src/test/java/com/example/ragbackend/knowledge/KnowledgeBasePersistenceTest.java`
- `src/test/java/com/example/ragbackend/knowledge/KnowledgeBaseControllerTest.java`
- `src/test/java/com/example/ragbackend/document/DocumentControllerTest.java`
- `README.md`

## 3. 每个核心文件的职责

`V4__create_document_chunk_table.sql`：创建 `document_chunk` 表、外键和常用索引。

`DocumentChunk`：映射 `document_chunk` 表字段。

`DocumentChunkMapper`：基于 MyBatis-Plus 的 chunk 数据库访问入口。

`DocumentChunkService` / `DocumentChunkServiceImpl`：提供基础 chunk 数据访问方法，包括 `create`、`findById`、`findByDocumentId`、`deleteByDocumentId`。

`DocumentChunkResponse`：Service 对外返回 chunk 数据时使用的只读 DTO。

`DocumentParser`：文档解析器抽象，定义 `supports(fileType)` 和 `parse(filePath)`。

`ParsedDocument`：parser 解析后的统一返回模型，包含文件名、文件类型、文本内容和元数据。

`DocumentParserRegistry`：根据 `fileType` 查找合适 parser，并把解析请求转发给具体 parser。

`UnsupportedDocumentTypeException`：当文件类型没有对应 parser 时返回清晰业务错误。

`AbstractTextDocumentParser`：封装 txt/md parser 共同的本地文件读取逻辑和基础错误处理。

`TxtDocumentParser`：支持 `txt` 文件，按 UTF-8 文本读取。

`MarkdownDocumentParser`：支持 `md` 文件，当前按 UTF-8 普通文本读取，不做 Markdown AST 解析。

## 4. 本轮功能入口在哪里

本轮没有新增 REST API。

代码入口是：

- parser 入口：`DocumentParserRegistry.parse(fileType, filePath)`
- chunk 数据访问入口：`DocumentChunkService`
- 数据库结构入口：`V4__create_document_chunk_table.sql`

## 5. 请求进入系统后的核心调用链

Parser 调用链：

```text
DocumentParserRegistry
  ↓
TxtDocumentParser / MarkdownDocumentParser
  ↓
读取本地文件
  ↓
ParsedDocument
```

Chunk 数据访问链路：

```text
DocumentChunkService
  ↓
DocumentChunkMapper
  ↓
document_chunk 表
```

后续 Phase 3.2 可以自然接成：

```text
DocumentProcessingService
  ↓
DocumentParserRegistry
  ↓
TextSplitter
  ↓
DocumentChunkService
```

## 6. 新增配置项

本轮没有新增 `application.yml` 配置项。

本轮新增数据库 migration：

- `V4__create_document_chunk_table.sql`

新增 Mapper 扫描包：

- `com.example.ragbackend.chunk.mapper`

## 7. 为什么采用当前设计

`DocumentParser` 被设计成只负责文件解析，避免把文件读取逻辑写进 Controller 或后续处理服务里。这样 Phase 3.2 增加 TextSplitter 时，可以直接复用 parser 输出的 `ParsedDocument`。

`DocumentChunkService` 被设计成只负责 chunk 数据访问，不负责解析、不负责切分、不负责状态流转。这样 chunk 的持久化边界清楚，后续文档处理编排可以由新的 `DocumentProcessingService` 承担。

txt 和 md parser 共享 `AbstractTextDocumentParser`，因为第一版都只是读取 UTF-8 文本。Markdown 先按普通文本处理，避免本轮提前引入 AST 解析复杂度。

`document_chunk` 表保留了 `processing_version`、`is_active`、`vector_id`、`page_number`、`metadata_json` 等字段，是为了支撑后续重新处理、向量索引映射、页码来源和扩展元数据。本轮只建事实表，不接 embedding 或 Qdrant Java client。

## 8. 如何运行和验证

运行自动测试：

```bash
mvn test
```

检查 diff 空白问题：

```bash
git diff --check
```

本轮重点验证：

- Flyway 能创建 `document_chunk` 表。
- `DocumentChunkMapper` 能插入和查询 chunk。
- `DocumentChunkService` 能创建、查询、按 document 查询和按 document 删除 chunk。
- txt 文件可以解析为 UTF-8 文本。
- md 文件可以解析为 UTF-8 文本。
- 不支持的文件类型会返回 `UNSUPPORTED_DOCUMENT_TYPE`。
- 文件不存在会返回 `PARSER_FILE_NOT_FOUND`。
- 空文件会返回 `PARSER_EMPTY_FILE`。

## 9. 哪些细节暂时不用深究

- 暂时不用深究 token 计算方式，`token_count` 只是预留字段。
- 暂时不用深究 chunk 大小、重叠窗口和切分算法，这些留给 TextSplitter。
- 暂时不用深究 Markdown AST，当前 md 文件按普通文本读取。
- 暂时不用深究 PDF / docx 解析，本轮不支持。
- 暂时不用深究 document 状态流转，本轮不改变 document 状态。
- 暂时不用深究 Qdrant、embedding、向量检索和 LLM，本轮不接入。

## 10. 下一轮建议

进入 Phase 3.2：TextSplitter、文档处理服务与 chunk 入库。

建议下一轮围绕这条主线实现：

```text
DocumentProcessingService
  ↓
DocumentParserRegistry
  ↓
TextSplitter
  ↓
DocumentChunkService
```

下一轮可以新增 `/api/documents/{id}/process`，但应继续避免 embedding、Qdrant 和 LLM 过早进入主线。
