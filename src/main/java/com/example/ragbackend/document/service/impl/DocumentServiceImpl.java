package com.example.ragbackend.document.service.impl;

import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.document.dto.DocumentCreateRequest;
import com.example.ragbackend.document.dto.DocumentResponse;
import com.example.ragbackend.document.entity.Document;
import com.example.ragbackend.document.enums.DocumentStatus;
import com.example.ragbackend.document.mapper.DocumentMapper;
import com.example.ragbackend.document.service.DocumentService;
import com.example.ragbackend.infrastructure.storage.FileStorageService;
import com.example.ragbackend.infrastructure.storage.StoredFile;
import com.example.ragbackend.knowledge.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final long DEFAULT_CREATED_BY = 1L;
    private static final int DEFAULT_CHUNK_COUNT = 0;
    private static final int DEFAULT_PROCESSING_VERSION = 1;
    private static final String DOCUMENT_NOT_FOUND_CODE = "DOCUMENT_NOT_FOUND";
    private static final String KNOWLEDGE_BASE_NOT_FOUND_CODE = "KNOWLEDGE_BASE_NOT_FOUND";

    private final DocumentMapper documentMapper;
    private final KnowledgeBaseService knowledgeBaseService;
    private final FileStorageService fileStorageService;

    @Override
    public DocumentResponse createMetadata(DocumentCreateRequest request) {
        ensureKnowledgeBaseExists(request.knowledgeBaseId());

        Document document = new Document();
        document.setKnowledgeBaseId(request.knowledgeBaseId());
        document.setFileName(request.fileName());
        document.setFileType(request.fileType());
        document.setFileSize(request.fileSize());
        document.setStoragePath(request.storagePath());
        document.setStatus(DocumentStatus.UPLOADED.name());
        document.setChunkCount(DEFAULT_CHUNK_COUNT);
        document.setProcessingVersion(DEFAULT_PROCESSING_VERSION);
        document.setCreatedBy(request.createdBy() == null ? DEFAULT_CREATED_BY : request.createdBy());

        documentMapper.insert(document);

        return toResponse(getExistingDocument(document.getId()));
    }

    @Override
    public DocumentResponse upload(Long knowledgeBaseId, MultipartFile file, Long createdBy) {
        ensureKnowledgeBaseExists(knowledgeBaseId);

        StoredFile storedFile = fileStorageService.store(file);

        Document document = new Document();
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setFileName(storedFile.originalFileName());
        document.setFileType(storedFile.fileType());
        document.setFileSize(storedFile.fileSize());
        document.setStoragePath(storedFile.storagePath());
        document.setStatus(DocumentStatus.UPLOADED.name());
        document.setChunkCount(DEFAULT_CHUNK_COUNT);
        document.setProcessingVersion(DEFAULT_PROCESSING_VERSION);
        document.setCreatedBy(createdBy == null ? DEFAULT_CREATED_BY : createdBy);

        try {
            documentMapper.insert(document);
        } catch (RuntimeException ex) {
            fileStorageService.delete(storedFile.storagePath());
            throw ex;
        }

        return toResponse(getExistingDocument(document.getId()));
    }

    @Override
    public DocumentResponse findById(Long id) {
        return toResponse(getExistingDocument(id));
    }

    @Override
    public List<DocumentResponse> findByKnowledgeBaseId(Long knowledgeBaseId) {
        ensureKnowledgeBaseExists(knowledgeBaseId);

        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<Document>()
                .eq(Document::getKnowledgeBaseId, knowledgeBaseId)
                .orderByDesc(Document::getCreatedAt)
                .orderByDesc(Document::getId);
        return documentMapper.selectList(queryWrapper)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public DocumentResponse updateStatus(Long id, DocumentStatus status) {
        Document document = getExistingDocument(id);
        document.setStatus(status.name());

        documentMapper.updateById(document);

        return toResponse(getExistingDocument(id));
    }

    @Override
    public void deleteById(Long id) {
        Document document = getExistingDocument(id);
        fileStorageService.delete(document.getStoragePath());
        documentMapper.deleteById(id);
    }

    private void ensureKnowledgeBaseExists(Long knowledgeBaseId) {
        if (!knowledgeBaseService.existsById(knowledgeBaseId)) {
            throw new BusinessException(
                    KNOWLEDGE_BASE_NOT_FOUND_CODE,
                    "Knowledge base not found: " + knowledgeBaseId
            );
        }
    }

    private Document getExistingDocument(Long id) {
        Document document = documentMapper.selectById(id);
        if (document == null) {
            throw new BusinessException(DOCUMENT_NOT_FOUND_CODE, "Document not found: " + id);
        }
        return document;
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getKnowledgeBaseId(),
                document.getFileName(),
                document.getFileType(),
                document.getFileSize(),
                document.getStoragePath(),
                document.getStatus(),
                document.getChunkCount(),
                document.getProcessingVersion(),
                document.getFailedStage(),
                document.getErrorMessage(),
                document.getCreatedBy(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
