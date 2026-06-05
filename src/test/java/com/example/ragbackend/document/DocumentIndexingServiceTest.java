package com.example.ragbackend.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ragbackend.chunk.entity.DocumentChunk;
import com.example.ragbackend.chunk.mapper.DocumentChunkMapper;
import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.document.dto.DocumentProcessingResponse;
import com.example.ragbackend.document.entity.Document;
import com.example.ragbackend.document.enums.DocumentStatus;
import com.example.ragbackend.document.mapper.DocumentMapper;
import com.example.ragbackend.document.service.DocumentIndexingService;
import com.example.ragbackend.embedding.service.EmbeddingService;
import com.example.ragbackend.knowledge.entity.KnowledgeBase;
import com.example.ragbackend.knowledge.mapper.KnowledgeBaseMapper;
import com.example.ragbackend.vector.model.ChunkVector;
import com.example.ragbackend.vector.service.VectorStoreService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DocumentIndexingServiceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private DocumentChunkMapper documentChunkMapper;

    @Autowired
    private DocumentIndexingService documentIndexingService;

    @MockBean
    private EmbeddingService embeddingService;

    @MockBean
    private VectorStoreService vectorStoreService;

    @BeforeEach
    void cleanData() {
        jdbcTemplate.execute("DELETE FROM document_chunk");
        jdbcTemplate.execute("DELETE FROM document");
        jdbcTemplate.execute("DELETE FROM knowledge_base");
    }

    @Test
    void indexesChunkedDocumentAndWritesVectorIds() {
        Long knowledgeBaseId = createKnowledgeBase();
        Long documentId = createDocument(knowledgeBaseId, DocumentStatus.CHUNKED, 2);
        DocumentChunk second = createChunk(knowledgeBaseId, documentId, 1, "second chunk");
        DocumentChunk first = createChunk(knowledgeBaseId, documentId, 0, "first chunk");
        when(embeddingService.embed(anyString())).thenReturn(vector());

        DocumentProcessingResponse response = documentIndexingService.index(documentId);

        assertThat(response.documentId()).isEqualTo(documentId);
        assertThat(response.status()).isEqualTo(DocumentStatus.INDEXED.name());
        assertThat(response.chunkCount()).isEqualTo(2);
        assertThat(response.processingVersion()).isEqualTo(1);

        Document indexedDocument = documentMapper.selectById(documentId);
        assertThat(indexedDocument.getStatus()).isEqualTo(DocumentStatus.INDEXED.name());
        assertThat(indexedDocument.getFailedStage()).isNull();
        assertThat(indexedDocument.getErrorMessage()).isNull();

        List<DocumentChunk> activeChunks = activeChunks(documentId);
        assertThat(activeChunks)
                .extracting(DocumentChunk::getChunkIndex)
                .containsExactly(0, 1);
        assertThat(activeChunks)
                .extracting(DocumentChunk::getVectorId)
                .containsExactly(first.getId().toString(), second.getId().toString());

        verify(embeddingService, times(2)).embed(anyString());
        verify(vectorStoreService).ensureCollection();
        ArgumentCaptor<ChunkVector> captor = ArgumentCaptor.forClass(ChunkVector.class);
        verify(vectorStoreService, times(2)).upsert(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(ChunkVector::chunkId)
                .containsExactly(first.getId(), second.getId());
        assertThat(captor.getAllValues())
                .extracting(ChunkVector::documentId)
                .containsOnly(documentId);
        assertThat(captor.getAllValues())
                .extracting(ChunkVector::knowledgeBaseId)
                .containsOnly(knowledgeBaseId);
    }

    @Test
    void rejectsDocumentThatIsNotChunked() {
        Long knowledgeBaseId = createKnowledgeBase();
        Long documentId = createDocument(knowledgeBaseId, DocumentStatus.UPLOADED, 0);

        assertThatThrownBy(() -> documentIndexingService.index(documentId))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("DOCUMENT_INDEX_NOT_ALLOWED"));

        verify(embeddingService, never()).embed(anyString());
        verify(vectorStoreService, never()).ensureCollection();
    }

    @Test
    void rejectsDocumentWithoutActiveChunks() {
        Long knowledgeBaseId = createKnowledgeBase();
        Long documentId = createDocument(knowledgeBaseId, DocumentStatus.CHUNKED, 0);

        assertThatThrownBy(() -> documentIndexingService.index(documentId))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("DOCUMENT_INDEX_EMPTY_CHUNKS"));

        Document document = documentMapper.selectById(documentId);
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.CHUNKED.name());
        verify(embeddingService, never()).embed(anyString());
        verify(vectorStoreService, never()).ensureCollection();
    }

    @Test
    void embeddingFailureMarksDocumentFailedAtEmbeddingStage() {
        Long knowledgeBaseId = createKnowledgeBase();
        Long documentId = createDocument(knowledgeBaseId, DocumentStatus.CHUNKED, 1);
        createChunk(knowledgeBaseId, documentId, 0, "chunk");
        when(embeddingService.embed(anyString()))
                .thenThrow(new BusinessException("EMBEDDING_FAILED", "embedding failed"));

        assertThatThrownBy(() -> documentIndexingService.index(documentId))
                .isInstanceOf(BusinessException.class);

        Document failedDocument = documentMapper.selectById(documentId);
        assertThat(failedDocument.getStatus()).isEqualTo(DocumentStatus.FAILED.name());
        assertThat(failedDocument.getFailedStage()).isEqualTo(DocumentStatus.EMBEDDING.name());
        assertThat(failedDocument.getErrorMessage()).contains("embedding failed");
        verify(vectorStoreService, never()).ensureCollection();
    }

    @Test
    void vectorUpsertFailureMarksDocumentFailedAtIndexingStage() {
        Long knowledgeBaseId = createKnowledgeBase();
        Long documentId = createDocument(knowledgeBaseId, DocumentStatus.CHUNKED, 1);
        createChunk(knowledgeBaseId, documentId, 0, "chunk");
        when(embeddingService.embed(anyString())).thenReturn(vector());
        doThrow(new BusinessException("QDRANT_OPERATION_FAILED", "upsert failed"))
                .when(vectorStoreService)
                .upsert(any(ChunkVector.class));

        assertThatThrownBy(() -> documentIndexingService.index(documentId))
                .isInstanceOf(BusinessException.class);

        Document failedDocument = documentMapper.selectById(documentId);
        assertThat(failedDocument.getStatus()).isEqualTo(DocumentStatus.FAILED.name());
        assertThat(failedDocument.getFailedStage()).isEqualTo(DocumentStatus.INDEXING.name());
        assertThat(failedDocument.getErrorMessage()).contains("upsert failed");
        verify(vectorStoreService).ensureCollection();
    }

    private Long createKnowledgeBase() {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setName("Indexing test knowledge base");
        knowledgeBase.setDescription("Created for indexing tests");
        knowledgeBase.setOwnerId(1L);
        knowledgeBase.setVisibility("PRIVATE");
        knowledgeBaseMapper.insert(knowledgeBase);
        return knowledgeBase.getId();
    }

    private Long createDocument(Long knowledgeBaseId, DocumentStatus status, int chunkCount) {
        Document document = new Document();
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setFileName("indexing.txt");
        document.setFileType("txt");
        document.setFileSize(100L);
        document.setStoragePath("documents/indexing.txt");
        document.setStatus(status.name());
        document.setChunkCount(chunkCount);
        document.setProcessingVersion(1);
        document.setCreatedBy(1L);
        documentMapper.insert(document);
        return document.getId();
    }

    private DocumentChunk createChunk(Long knowledgeBaseId, Long documentId, int chunkIndex, String content) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setKnowledgeBaseId(knowledgeBaseId);
        chunk.setDocumentId(documentId);
        chunk.setChunkIndex(chunkIndex);
        chunk.setContent(content);
        chunk.setContentHash("hash-" + chunkIndex);
        chunk.setProcessingVersion(1);
        chunk.setIsActive(true);
        chunk.setTokenCount(2);
        documentChunkMapper.insert(chunk);
        return chunk;
    }

    private List<DocumentChunk> activeChunks(Long documentId) {
        LambdaQueryWrapper<DocumentChunk> queryWrapper = new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getDocumentId, documentId)
                .eq(DocumentChunk::getIsActive, true)
                .orderByAsc(DocumentChunk::getChunkIndex)
                .orderByAsc(DocumentChunk::getId);
        return documentChunkMapper.selectList(queryWrapper);
    }

    private List<Float> vector() {
        return List.of(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f);
    }
}
