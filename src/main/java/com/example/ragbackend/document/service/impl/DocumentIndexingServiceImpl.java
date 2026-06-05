package com.example.ragbackend.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.ragbackend.chunk.dto.DocumentChunkResponse;
import com.example.ragbackend.chunk.service.DocumentChunkService;
import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.document.dto.DocumentProcessingResponse;
import com.example.ragbackend.document.entity.Document;
import com.example.ragbackend.document.enums.DocumentStatus;
import com.example.ragbackend.document.mapper.DocumentMapper;
import com.example.ragbackend.document.service.DocumentIndexingService;
import com.example.ragbackend.embedding.service.EmbeddingService;
import com.example.ragbackend.vector.model.ChunkVector;
import com.example.ragbackend.vector.service.VectorStoreService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentIndexingServiceImpl implements DocumentIndexingService {

    private static final String DOCUMENT_NOT_FOUND_CODE = "DOCUMENT_NOT_FOUND";
    private static final String DOCUMENT_INDEX_NOT_ALLOWED_CODE = "DOCUMENT_INDEX_NOT_ALLOWED";
    private static final String DOCUMENT_INDEX_EMPTY_CHUNKS_CODE = "DOCUMENT_INDEX_EMPTY_CHUNKS";

    private final DocumentMapper documentMapper;
    private final DocumentChunkService documentChunkService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    @Override
    public DocumentProcessingResponse index(Long documentId) {
        Document document = getExistingDocument(documentId);
        DocumentStatus originalStatus = parseStatus(document);
        validateIndexAllowed(document, originalStatus);

        List<DocumentChunkResponse> activeChunks = documentChunkService.findActiveByDocumentId(documentId);
        if (activeChunks.isEmpty()) {
            throw new BusinessException(
                    DOCUMENT_INDEX_EMPTY_CHUNKS_CODE,
                    "Document has no active chunks: " + documentId
            );
        }

        AtomicReference<String> failedStage = new AtomicReference<>(DocumentStatus.EMBEDDING.name());
        try {
            updateStatus(document, DocumentStatus.EMBEDDING);
            List<IndexedChunk> indexedChunks = embedChunks(activeChunks);

            failedStage.set(DocumentStatus.INDEXING.name());
            updateStatus(document, DocumentStatus.INDEXING);
            vectorStoreService.ensureCollection();
            for (IndexedChunk indexedChunk : indexedChunks) {
                vectorStoreService.upsert(indexedChunk.chunkVector());
                documentChunkService.updateVectorId(indexedChunk.chunk().id(), indexedChunk.vectorId());
            }

            updateIndexed(document, indexedChunks.size());
            return new DocumentProcessingResponse(
                    document.getId(),
                    document.getStatus(),
                    document.getChunkCount(),
                    document.getProcessingVersion()
            );
        } catch (RuntimeException ex) {
            markFailed(documentId, failedStage.get(), ex);
            throw ex;
        }
    }

    private List<IndexedChunk> embedChunks(List<DocumentChunkResponse> activeChunks) {
        List<IndexedChunk> indexedChunks = new ArrayList<>(activeChunks.size());
        for (DocumentChunkResponse chunk : activeChunks) {
            List<Float> vector = embeddingService.embed(chunk.content());
            String vectorId = vectorId(chunk);
            indexedChunks.add(new IndexedChunk(chunk, vectorId, toChunkVector(chunk, vector)));
        }
        return indexedChunks;
    }

    private ChunkVector toChunkVector(DocumentChunkResponse chunk, List<Float> vector) {
        return new ChunkVector(
                chunk.id(),
                chunk.documentId(),
                chunk.knowledgeBaseId(),
                chunk.chunkIndex(),
                chunk.contentHash(),
                chunk.processingVersion(),
                vector
        );
    }

    private String vectorId(DocumentChunkResponse chunk) {
        return String.valueOf(chunk.id());
    }

    private Document getExistingDocument(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(DOCUMENT_NOT_FOUND_CODE, "Document not found: " + documentId);
        }
        return document;
    }

    private DocumentStatus parseStatus(Document document) {
        try {
            return DocumentStatus.valueOf(document.getStatus());
        } catch (RuntimeException ex) {
            throw new BusinessException(
                    DOCUMENT_INDEX_NOT_ALLOWED_CODE,
                    "Document status is not allowed for indexing: " + document.getStatus()
            );
        }
    }

    private void validateIndexAllowed(Document document, DocumentStatus status) {
        if (status != DocumentStatus.CHUNKED) {
            throw new BusinessException(
                    DOCUMENT_INDEX_NOT_ALLOWED_CODE,
                    "Document status is not allowed for indexing: "
                            + status.name()
                            + ", documentId: "
                            + document.getId()
            );
        }
    }

    private void updateStatus(Document document, DocumentStatus status) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Document> updateWrapper = new LambdaUpdateWrapper<Document>()
                .eq(Document::getId, document.getId())
                .set(Document::getStatus, status.name())
                .set(Document::getFailedStage, null)
                .set(Document::getErrorMessage, null)
                .set(Document::getUpdatedAt, now);
        documentMapper.update(null, updateWrapper);

        document.setStatus(status.name());
        document.setFailedStage(null);
        document.setErrorMessage(null);
        document.setUpdatedAt(now);
    }

    private void updateIndexed(Document document, int chunkCount) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Document> updateWrapper = new LambdaUpdateWrapper<Document>()
                .eq(Document::getId, document.getId())
                .set(Document::getStatus, DocumentStatus.INDEXED.name())
                .set(Document::getChunkCount, chunkCount)
                .set(Document::getFailedStage, null)
                .set(Document::getErrorMessage, null)
                .set(Document::getUpdatedAt, now);
        documentMapper.update(null, updateWrapper);

        document.setStatus(DocumentStatus.INDEXED.name());
        document.setChunkCount(chunkCount);
        document.setFailedStage(null);
        document.setErrorMessage(null);
        document.setUpdatedAt(now);
    }

    private void markFailed(Long documentId, String failedStage, RuntimeException ex) {
        if (documentMapper.selectById(documentId) == null) {
            return;
        }

        LambdaUpdateWrapper<Document> updateWrapper = new LambdaUpdateWrapper<Document>()
                .eq(Document::getId, documentId)
                .set(Document::getStatus, DocumentStatus.FAILED.name())
                .set(Document::getFailedStage, failedStage)
                .set(Document::getErrorMessage, errorMessage(ex))
                .set(Document::getUpdatedAt, LocalDateTime.now());
        documentMapper.update(null, updateWrapper);
    }

    private String errorMessage(RuntimeException ex) {
        if (ex.getMessage() == null || ex.getMessage().isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return ex.getMessage();
    }

    private record IndexedChunk(
            DocumentChunkResponse chunk,
            String vectorId,
            ChunkVector chunkVector
    ) {
    }
}
