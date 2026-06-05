package com.example.ragbackend.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.ragbackend.chunk.config.ChunkProperties;
import com.example.ragbackend.chunk.entity.DocumentChunk;
import com.example.ragbackend.chunk.service.DocumentChunkService;
import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.document.dto.DocumentProcessingResponse;
import com.example.ragbackend.document.entity.Document;
import com.example.ragbackend.document.enums.DocumentStatus;
import com.example.ragbackend.document.mapper.DocumentMapper;
import com.example.ragbackend.document.service.DocumentProcessingService;
import com.example.ragbackend.infrastructure.storage.StorageProperties;
import com.example.ragbackend.parser.DocumentParserRegistry;
import com.example.ragbackend.parser.ParsedDocument;
import com.example.ragbackend.splitter.SplitOptions;
import com.example.ragbackend.splitter.TextChunk;
import com.example.ragbackend.splitter.TextSplitter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class DocumentProcessingServiceImpl implements DocumentProcessingService {

    private static final String DOCUMENT_NOT_FOUND_CODE = "DOCUMENT_NOT_FOUND";
    private static final String DOCUMENT_PROCESS_NOT_ALLOWED_CODE = "DOCUMENT_PROCESS_NOT_ALLOWED";
    private static final String DOCUMENT_FILE_NOT_FOUND_CODE = "DOCUMENT_FILE_NOT_FOUND";
    private static final String DOCUMENT_FILE_PATH_INVALID_CODE = "DOCUMENT_FILE_PATH_INVALID";
    private static final String DOCUMENT_PROCESSING_EMPTY_CHUNKS_CODE = "DOCUMENT_PROCESSING_EMPTY_CHUNKS";
    private static final Set<DocumentStatus> PROCESS_ALLOWED_STATUSES = EnumSet.of(
            DocumentStatus.UPLOADED,
            DocumentStatus.FAILED,
            DocumentStatus.PARSED,
            DocumentStatus.CHUNKED
    );

    private final DocumentMapper documentMapper;
    private final DocumentParserRegistry documentParserRegistry;
    private final TextSplitter textSplitter;
    private final DocumentChunkService documentChunkService;
    private final StorageProperties storageProperties;
    private final ChunkProperties chunkProperties;
    private final PlatformTransactionManager transactionManager;

    @Override
    public DocumentProcessingResponse process(Long documentId) {
        Document document = getExistingDocument(documentId);
        DocumentStatus originalStatus = parseStatus(document);
        validateProcessAllowed(document, originalStatus);
        int processingVersion = nextProcessingVersion(document, originalStatus);
        AtomicReference<String> failedStage = new AtomicReference<>(DocumentStatus.PARSING.name());

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        try {
            return transactionTemplate.execute(status ->
                    processInTransaction(documentId, processingVersion, failedStage)
            );
        } catch (RuntimeException ex) {
            markFailed(documentId, failedStage.get(), ex);
            throw ex;
        }
    }

    private DocumentProcessingResponse processInTransaction(
            Long documentId, int processingVersion, AtomicReference<String> failedStage) {
        Document document = getExistingDocument(documentId);
        document.setProcessingVersion(processingVersion);

        updateStatus(document, DocumentStatus.PARSING);
        Path filePath = resolveStoredFile(document);

        ParsedDocument parsedDocument = documentParserRegistry.parse(document.getFileType(), filePath);

        updateStatus(document, DocumentStatus.PARSED);
        failedStage.set(DocumentStatus.CHUNKING.name());
        updateStatus(document, DocumentStatus.CHUNKING);

        List<TextChunk> textChunks = textSplitter.split(
                parsedDocument.content(),
                new SplitOptions(chunkProperties.getSize(), chunkProperties.getOverlap())
        );
        if (textChunks.isEmpty()) {
            throw new BusinessException(
                    DOCUMENT_PROCESSING_EMPTY_CHUNKS_CODE,
                    "Document processing produced no chunks: " + documentId
            );
        }

        documentChunkService.deactivateByDocumentId(document.getId());
        for (TextChunk textChunk : textChunks) {
            documentChunkService.create(toDocumentChunk(document, textChunk));
        }

        updateChunked(document, textChunks.size());

        return new DocumentProcessingResponse(
                document.getId(),
                document.getStatus(),
                document.getChunkCount(),
                document.getProcessingVersion()
        );
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
                    DOCUMENT_PROCESS_NOT_ALLOWED_CODE,
                    "Document status is not allowed for processing: " + document.getStatus()
            );
        }
    }

    private void validateProcessAllowed(Document document, DocumentStatus status) {
        if (!PROCESS_ALLOWED_STATUSES.contains(status)) {
            throw new BusinessException(
                    DOCUMENT_PROCESS_NOT_ALLOWED_CODE,
                    "Document status is not allowed for processing: "
                            + status.name()
                            + ", documentId: "
                            + document.getId()
            );
        }
    }

    private int nextProcessingVersion(Document document, DocumentStatus originalStatus) {
        int currentVersion = document.getProcessingVersion() == null ? 1 : document.getProcessingVersion();
        if (originalStatus == DocumentStatus.UPLOADED) {
            return currentVersion;
        }
        return currentVersion + 1;
    }

    private Path resolveStoredFile(Document document) {
        if (document.getStoragePath() == null || document.getStoragePath().isBlank()) {
            throw new BusinessException(
                    DOCUMENT_FILE_NOT_FOUND_CODE,
                    "Document storage path is empty: " + document.getId()
            );
        }

        try {
            Path localRoot = Path.of(storageProperties.getLocalRoot()).toAbsolutePath().normalize();
            Path filePath = localRoot.resolve(document.getStoragePath()).normalize();
            if (!filePath.startsWith(localRoot)) {
                throw new BusinessException(DOCUMENT_FILE_PATH_INVALID_CODE, "Document file path is invalid");
            }
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                throw new BusinessException(
                        DOCUMENT_FILE_NOT_FOUND_CODE,
                        "Document file does not exist: " + document.getId()
                );
            }
            return filePath;
        } catch (InvalidPathException ex) {
            throw new BusinessException(DOCUMENT_FILE_PATH_INVALID_CODE, "Document file path is invalid");
        }
    }

    private DocumentChunk toDocumentChunk(Document document, TextChunk textChunk) {
        DocumentChunk documentChunk = new DocumentChunk();
        documentChunk.setKnowledgeBaseId(document.getKnowledgeBaseId());
        documentChunk.setDocumentId(document.getId());
        documentChunk.setChunkIndex(textChunk.index());
        documentChunk.setContent(textChunk.content());
        documentChunk.setContentHash(sha256(textChunk.content()));
        documentChunk.setProcessingVersion(document.getProcessingVersion());
        documentChunk.setIsActive(true);
        documentChunk.setTokenCount(textChunk.tokenCount());

        return documentChunk;
    }

    private void updateStatus(Document document, DocumentStatus status) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Document> updateWrapper = new LambdaUpdateWrapper<Document>()
                .eq(Document::getId, document.getId())
                .set(Document::getStatus, status.name())
                .set(Document::getProcessingVersion, document.getProcessingVersion())
                .set(Document::getFailedStage, null)
                .set(Document::getErrorMessage, null)
                .set(Document::getUpdatedAt, now);
        documentMapper.update(null, updateWrapper);

        document.setStatus(status.name());
        document.setFailedStage(null);
        document.setErrorMessage(null);
        document.setUpdatedAt(now);
    }

    private void updateChunked(Document document, int chunkCount) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Document> updateWrapper = new LambdaUpdateWrapper<Document>()
                .eq(Document::getId, document.getId())
                .set(Document::getStatus, DocumentStatus.CHUNKED.name())
                .set(Document::getChunkCount, chunkCount)
                .set(Document::getProcessingVersion, document.getProcessingVersion())
                .set(Document::getFailedStage, null)
                .set(Document::getErrorMessage, null)
                .set(Document::getUpdatedAt, now);
        documentMapper.update(null, updateWrapper);

        document.setStatus(DocumentStatus.CHUNKED.name());
        document.setChunkCount(chunkCount);
        document.setFailedStage(null);
        document.setErrorMessage(null);
        document.setUpdatedAt(now);
    }

    private void markFailed(Long documentId, String failedStage, RuntimeException ex) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
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
        });
    }

    private String errorMessage(RuntimeException ex) {
        if (ex.getMessage() == null || ex.getMessage().isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return ex.getMessage();
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }
}
