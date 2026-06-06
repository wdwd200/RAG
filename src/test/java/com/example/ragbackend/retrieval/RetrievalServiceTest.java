package com.example.ragbackend.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ragbackend.chunk.entity.DocumentChunk;
import com.example.ragbackend.chunk.mapper.DocumentChunkMapper;
import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.document.entity.Document;
import com.example.ragbackend.document.enums.DocumentStatus;
import com.example.ragbackend.document.mapper.DocumentMapper;
import com.example.ragbackend.embedding.service.EmbeddingService;
import com.example.ragbackend.knowledge.entity.KnowledgeBase;
import com.example.ragbackend.knowledge.mapper.KnowledgeBaseMapper;
import com.example.ragbackend.retrieval.dto.RetrieveRequest;
import com.example.ragbackend.retrieval.dto.RetrieveResponse;
import com.example.ragbackend.retrieval.service.RetrievalService;
import com.example.ragbackend.vector.model.VectorSearchRequest;
import com.example.ragbackend.vector.model.VectorSearchResult;
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
class RetrievalServiceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private DocumentChunkMapper documentChunkMapper;

    @Autowired
    private RetrievalService retrievalService;

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
    void retrievesActiveChunksFromDatabaseAndPassesKnowledgeBaseFilter() {
        Long knowledgeBaseId = createKnowledgeBase("Retrieval knowledge base");
        Long otherKnowledgeBaseId = createKnowledgeBase("Other knowledge base");
        Long documentId = createDocument(knowledgeBaseId);
        Long otherDocumentId = createDocument(otherKnowledgeBaseId);
        DocumentChunk activeChunk = createChunk(knowledgeBaseId, documentId, 0, "database fact", true);
        DocumentChunk inactiveChunk = createChunk(knowledgeBaseId, documentId, 1, "inactive fact", false);
        DocumentChunk otherChunk = createChunk(otherKnowledgeBaseId, otherDocumentId, 0, "other fact", true);

        List<Float> queryVector = List.of(0.1f, 0.2f, 0.3f);
        when(embeddingService.embed("annual leave")).thenReturn(queryVector);
        when(vectorStoreService.search(any(VectorSearchRequest.class))).thenReturn(List.of(
                searchResult(inactiveChunk, 0.99d),
                searchResult(activeChunk, 0.95d),
                searchResult(otherChunk, 0.90d),
                new VectorSearchResult("999999", 999999L, documentId, knowledgeBaseId, 2, "missing", 1, 0.80d)
        ));

        RetrieveResponse response = retrievalService.retrieve(
                new RetrieveRequest(knowledgeBaseId, "annual leave", null)
        );

        assertThat(response.knowledgeBaseId()).isEqualTo(knowledgeBaseId);
        assertThat(response.topK()).isEqualTo(5);
        assertThat(response.chunks()).hasSize(1);
        assertThat(response.chunks().get(0).chunkId()).isEqualTo(activeChunk.getId());
        assertThat(response.chunks().get(0).content()).isEqualTo("database fact");
        assertThat(response.chunks().get(0).score()).isEqualTo(0.95d);

        verify(embeddingService).embed("annual leave");
        ArgumentCaptor<VectorSearchRequest> requestCaptor = ArgumentCaptor.forClass(VectorSearchRequest.class);
        verify(vectorStoreService).search(requestCaptor.capture());
        VectorSearchRequest searchRequest = requestCaptor.getValue();
        assertThat(searchRequest.queryVector()).isEqualTo(queryVector);
        assertThat(searchRequest.knowledgeBaseId()).isEqualTo(knowledgeBaseId);
        assertThat(searchRequest.topK()).isEqualTo(5);
    }

    @Test
    void rejectsTopKAboveMaximum() {
        assertThatThrownBy(() -> retrievalService.retrieve(new RetrieveRequest(1L, "question", 21)))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("RETRIEVAL_TOP_K_INVALID"));
    }

    private Long createKnowledgeBase(String name) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setName(name);
        knowledgeBase.setDescription("Created for retrieval tests");
        knowledgeBase.setOwnerId(1L);
        knowledgeBase.setVisibility("PRIVATE");
        knowledgeBaseMapper.insert(knowledgeBase);
        return knowledgeBase.getId();
    }

    private Long createDocument(Long knowledgeBaseId) {
        Document document = new Document();
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setFileName("retrieval.txt");
        document.setFileType("txt");
        document.setFileSize(100L);
        document.setStoragePath("documents/retrieval.txt");
        document.setStatus(DocumentStatus.INDEXED.name());
        document.setChunkCount(1);
        document.setProcessingVersion(1);
        document.setCreatedBy(1L);
        documentMapper.insert(document);
        return document.getId();
    }

    private DocumentChunk createChunk(
            Long knowledgeBaseId,
            Long documentId,
            int chunkIndex,
            String content,
            boolean active) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setKnowledgeBaseId(knowledgeBaseId);
        chunk.setDocumentId(documentId);
        chunk.setChunkIndex(chunkIndex);
        chunk.setContent(content);
        chunk.setContentHash("hash-" + documentId + "-" + chunkIndex);
        chunk.setProcessingVersion(1);
        chunk.setIsActive(active);
        chunk.setTokenCount(2);
        chunk.setVectorId("vector-" + documentId + "-" + chunkIndex);
        documentChunkMapper.insert(chunk);
        return chunk;
    }

    private VectorSearchResult searchResult(DocumentChunk chunk, double score) {
        return new VectorSearchResult(
                chunk.getId().toString(),
                chunk.getId(),
                chunk.getDocumentId(),
                chunk.getKnowledgeBaseId(),
                chunk.getChunkIndex(),
                chunk.getContentHash(),
                chunk.getProcessingVersion(),
                score
        );
    }
}
