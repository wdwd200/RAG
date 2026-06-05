# Phase 2 Summary

## 1. Phase 2 完成了什么

Phase 2 完成了 document 元数据模块和本地文件上传链路。

当前已经具备：

- `document` 表和基础状态字段。
- 文档元数据创建、查询、列表和删除接口。
- multipart 文件上传接口。
- 本地 storage 保存原始文件。
- 上传前文件类型和大小限制。
- Spring multipart 上限已设置为高于业务文件大小限制，避免默认 multipart 限制提前拦截 10MB 业务校验。
- 删除 document 时同步删除本地文件。
- H2 自动化测试覆盖主要上传和删除场景。

## 2. 当前 document 模块目录结构

```text
src/main/java/com/example/ragbackend/document
├── controller
│   └── DocumentController.java
├── dto
│   ├── DocumentCreateRequest.java
│   └── DocumentResponse.java
├── entity
│   └── Document.java
├── enums
│   └── DocumentStatus.java
├── mapper
│   └── DocumentMapper.java
└── service
    ├── DocumentService.java
    └── impl
        └── DocumentServiceImpl.java
```

文件存储相关代码在：

```text
src/main/java/com/example/ragbackend/infrastructure/storage
├── FileStorageService.java
├── LocalFileStorageService.java
├── StorageProperties.java
└── StoredFile.java
```

## 3. 文档元数据链路

```text
POST /api/documents
  ↓
DocumentController
  ↓
DocumentService
  ↓
DocumentMapper
  ↓
document 表
```

Controller 只接收请求并返回统一 `ApiResponse`。Service 校验知识库是否存在，组装 `Document`，再交给 Mapper 写入数据库。

## 4. 文件上传链路

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
保存原始文件到本地 storage
  ↓
DocumentMapper
  ↓
document 表
```

上传接口保存原始文件后创建 document 记录，状态为 `UPLOADED`。如果文件保存成功但数据库写入失败，Service 会尽量删除刚保存的文件。

## 5. 文件删除链路

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

删除 document 时，Service 先查记录，再删除本地文件，最后删除数据库记录。本地文件已经不存在时，删除 document 仍可成功；如果文件删除过程异常，则不会继续删除数据库记录。

## 6. 每个核心文件的职责

`DocumentController`：HTTP 入口，接收文档元数据、上传、查询、列表和删除请求。

`DocumentService`：文档业务流程接口，定义 document 模块对 Controller 暴露的能力。

`DocumentServiceImpl`：文档业务流程编排，负责知识库存在性校验、上传后的 document 记录创建、查询和删除一致性。

`FileStorageService`：文件存储接口，定义保存文件和删除文件的能力。

`LocalFileStorageService`：本地文件保存、校验、删除实现，负责文件名清理、后缀限制、大小限制、路径穿越防护和本地文件 IO。

`StorageProperties`：绑定 `app.storage` 配置，包括本地根目录、允许后缀和最大文件大小。

`StoredFile`：封装保存后的文件信息，包括原始文件名、storage path、文件大小和文件类型。

`DocumentMapper`：document 表访问入口。

`DocumentStatus`：文档处理状态枚举，当前上传完成后使用 `UPLOADED`。

## 7. 如何启动项目

启动本地依赖：

```bash
docker compose up -d
```

打包：

```bash
mvn clean package
```

启动应用：

```bash
java -jar target/rag-backend-0.0.1-SNAPSHOT.jar
```

默认服务端口为 `8080`。

## 8. 如何验证接口

健康检查：

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/api/health/database
```

创建知识库：

```bash
curl -X POST http://localhost:8080/api/knowledge-bases \
  -H "Content-Type: application/json" \
  -d '{"name":"Phase 2 验证知识库","description":"Phase 2 manual verification","ownerId":1,"visibility":"PRIVATE"}'
```

上传文件：

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "knowledgeBaseId=1" \
  -F "file=@/path/to/demo.txt"
```

查询 document：

```bash
curl http://localhost:8080/api/documents/1
```

查询知识库下的 documents：

```bash
curl http://localhost:8080/api/knowledge-bases/1/documents
```

删除 document：

```bash
curl -X DELETE http://localhost:8080/api/documents/1
```

## 9. 当前还没有做什么

- 没有做文档解析。
- 没有创建 `document_chunk` 表。
- 没有做 chunk 切分。
- 没有做 embedding。
- 没有接入 Qdrant Java 客户端。
- 没有做向量检索。
- 没有接入 LLM。
- 没有做 SSE。
- 没有接入 Redis / Elasticsearch / RabbitMQ。
- 没有做 Spring Security / JWT。

## 10. 下一阶段 Phase 3 要做什么

Phase 3 建议从文档解析和 chunk 切分开始：

- 新增 `document_chunk` 表。
- 新增 `DocumentParser` 抽象。
- 先支持 txt / md 文本解析。
- 将解析后的内容切分为 chunk。
- 为后续 embedding 和向量检索准备数据结构。
