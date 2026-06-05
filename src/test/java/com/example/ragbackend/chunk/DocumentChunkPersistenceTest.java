package com.example.ragbackend.chunk;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ragbackend.chunk.dto.DocumentChunkResponse;
import com.example.ragbackend.chunk.entity.DocumentChunk;
import com.example.ragbackend.chunk.mapper.DocumentChunkMapper;
import com.example.ragbackend.chunk.service.DocumentChunkService;
import com.example.ragbackend.document.entity.Document;
import com.example.ragbackend.document.enums.DocumentStatus;
import com.example.ragbackend.document.mapper.DocumentMapper;
import com.example.ragbackend.knowledge.entity.KnowledgeBase;
import com.example.ragbackend.knowledge.mapper.KnowledgeBaseMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DocumentChunkPersistenceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private DocumentChunkMapper documentChunkMapper;

    @Autowired
    private DocumentChunkService documentChunkService;

    @BeforeEach
    void cleanData() {
        jdbcTemplate.execute("DELETE FROM document_chunk");
        jdbcTemplate.execute("DELETE FROM document");
        jdbcTemplate.execute("DELETE FROM knowledge_base");
    }

    @Test
    void flywayCreatesDocumentChunkTable() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'document_chunk'",
                Integer.class
        );

        assertThat(tableCount).isEqualTo(1);
    }

    @Test
    void mapperInsertsAndFindsDocumentChunk() {
        Long knowledgeBaseId = createKnowledgeBase();
        Long documentId = createDocument(knowledgeBaseId, "mapper-note.txt");
        DocumentChunk chunk = createChunk(knowledgeBaseId, documentId, 0, "Mapper chunk");

        int inserted = documentChunkMapper.insert(chunk);
        DocumentChunk found = documentChunkMapper.selectById(chunk.getId());

        assertThat(inserted).isEqualTo(1);
        assertThat(found).isNotNull();
        assertThat(found.getKnowledgeBaseId()).isEqualTo(knowledgeBaseId);
        assertThat(found.getDocumentId()).isEqualTo(documentId);
        assertThat(found.getChunkIndex()).isZero();
        assertThat(found.getContent()).isEqualTo("Mapper chunk");
        assertThat(found.getContentHash()).isEqualTo("hash-0");
        assertThat(found.getProcessingVersion()).isEqualTo(1);
        assertThat(found.getIsActive()).isTrue();
        assertThat(found.getTokenCount()).isEqualTo(2);
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void serviceCreatesFindsListsAndDeletesDocumentChunks() {
        Long knowledgeBaseId = createKnowledgeBase();
        Long documentId = createDocument(knowledgeBaseId, "service-note.md");

        DocumentChunkResponse second = documentChunkService.create(
                createChunk(knowledgeBaseId, documentId, 1, "Second chunk"));
        DocumentChunkResponse first = documentChunkService.create(
                createChunk(knowledgeBaseId, documentId, 0, "First chunk"));

        DocumentChunkResponse found = documentChunkService.findById(first.id());
        List<DocumentChunkResponse> byDocument = documentChunkService.findByDocumentId(documentId);

        assertThat(found.content()).isEqualTo("First chunk");
        assertThat(found.processingVersion()).isEqualTo(1);
        assertThat(found.isActive()).isTrue();
        assertThat(byDocument)
                .extracting(DocumentChunkResponse::id)
                .containsExactly(first.id(), second.id());

        documentChunkService.deleteByDocumentId(documentId);

        assertThat(documentChunkService.findByDocumentId(documentId)).isEmpty();
        assertThat(documentChunkMapper.selectById(first.id())).isNull();
        assertThat(documentChunkMapper.selectById(second.id())).isNull();
    }

    private Long createKnowledgeBase() {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setName("Chunk test knowledge base");
        knowledgeBase.setDescription("Created for chunk tests");
        knowledgeBase.setOwnerId(1L);
        knowledgeBase.setVisibility("PRIVATE");

        knowledgeBaseMapper.insert(knowledgeBase);

        return knowledgeBase.getId();
    }

    private Long createDocument(Long knowledgeBaseId, String fileName) {
        Document document = new Document();
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setFileName(fileName);
        document.setFileType(fileName.substring(fileName.lastIndexOf('.') + 1));
        document.setFileSize(1024L);
        document.setStoragePath("documents/" + fileName);
        document.setStatus(DocumentStatus.UPLOADED.name());
        document.setChunkCount(0);
        document.setProcessingVersion(1);
        document.setCreatedBy(1L);

        documentMapper.insert(document);

        return document.getId();
    }

    private DocumentChunk createChunk(
            Long knowledgeBaseId, Long documentId, Integer chunkIndex, String content) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setKnowledgeBaseId(knowledgeBaseId);
        chunk.setDocumentId(documentId);
        chunk.setChunkIndex(chunkIndex);
        chunk.setContent(content);
        chunk.setContentHash("hash-" + chunkIndex);
        chunk.setTokenCount(2);
        chunk.setMetadataJson("{\"source\":\"test\"}");

        return chunk;
    }
}
