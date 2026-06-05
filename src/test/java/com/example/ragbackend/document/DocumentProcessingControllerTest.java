package com.example.ragbackend.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.FileSystemUtils;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentProcessingControllerTest {

    private static final Path TEST_STORAGE_ROOT = Path.of("target/test-storage/documents");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanData() {
        jdbcTemplate.execute("DELETE FROM document_chunk");
        jdbcTemplate.execute("DELETE FROM document");
        jdbcTemplate.execute("DELETE FROM knowledge_base");
        FileSystemUtils.deleteRecursively(TEST_STORAGE_ROOT.toFile());
    }

    @AfterEach
    void cleanFiles() {
        FileSystemUtils.deleteRecursively(TEST_STORAGE_ROOT.toFile());
    }

    @Test
    void processesTxtDocumentAndListsChunks() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase();
        JsonNode uploadedDocument = uploadFile(knowledgeBaseId, "note.txt", "abcdefghijklmnopqrst");
        Long documentId = uploadedDocument.at("/data/id").asLong();

        mockMvc.perform(post("/api/documents/{id}/process", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentId").value(documentId))
                .andExpect(jsonPath("$.data.status").value("CHUNKED"))
                .andExpect(jsonPath("$.data.chunkCount").value(3))
                .andExpect(jsonPath("$.data.processingVersion").value(1));

        mockMvc.perform(get("/api/documents/{id}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CHUNKED"))
                .andExpect(jsonPath("$.data.chunkCount").value(3));

        MvcResult chunksResult = mockMvc.perform(get("/api/documents/{documentId}/chunks", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].chunkIndex").value(0))
                .andExpect(jsonPath("$.data[0].content").value("abcdefgh"))
                .andExpect(jsonPath("$.data[1].chunkIndex").value(1))
                .andExpect(jsonPath("$.data[1].content").value("ghijklmn"))
                .andReturn();

        JsonNode chunks = objectMapper.readTree(chunksResult.getResponse().getContentAsString());
        Long firstChunkId = chunks.at("/data/0/id").asLong();

        mockMvc.perform(get("/api/chunks/{id}", firstChunkId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(firstChunkId))
                .andExpect(jsonPath("$.data.documentId").value(documentId))
                .andExpect(jsonPath("$.data.contentHash").isNotEmpty())
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    void processesMarkdownDocumentAndCreatesChunks() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase();
        JsonNode uploadedDocument = uploadFile(knowledgeBaseId, "guide.md", "# Title\nmarkdown");
        Long documentId = uploadedDocument.at("/data/id").asLong();

        mockMvc.perform(post("/api/documents/{id}/process", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CHUNKED"))
                .andExpect(jsonPath("$.data.chunkCount").value(3));

        mockMvc.perform(get("/api/documents/{documentId}/chunks", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].content").value("# Title\n"));
    }

    @Test
    void reprocessesChunkedDocumentWithNextProcessingVersionAndInactiveOldChunks() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase();
        JsonNode uploadedDocument = uploadFile(knowledgeBaseId, "reprocess.txt", "abcdefghijklmnopqrst");
        Long documentId = uploadedDocument.at("/data/id").asLong();

        mockMvc.perform(post("/api/documents/{id}/process", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CHUNKED"))
                .andExpect(jsonPath("$.data.processingVersion").value(1));

        mockMvc.perform(post("/api/documents/{id}/process", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CHUNKED"))
                .andExpect(jsonPath("$.data.chunkCount").value(3))
                .andExpect(jsonPath("$.data.processingVersion").value(2));

        assertThat(chunkCount(documentId, true, 2)).isEqualTo(3);
        assertThat(chunkCount(documentId, false, 1)).isEqualTo(3);

        mockMvc.perform(get("/api/documents/{documentId}/chunks", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].processingVersion").value(2))
                .andExpect(jsonPath("$.data[0].isActive").value(true));
    }

    @Test
    void returnsErrorWhenProcessingMissingDocument() throws Exception {
        mockMvc.perform(post("/api/documents/{id}/process", 999999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DOCUMENT_NOT_FOUND"));
    }

    @Test
    void missingStoredFileMarksDocumentFailedAtParsingAndCanRetry() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase();
        JsonNode uploadedDocument = uploadFile(knowledgeBaseId, "missing.txt", "abcdefgh");
        Long documentId = uploadedDocument.at("/data/id").asLong();
        Path storedFile = TEST_STORAGE_ROOT.resolve(uploadedDocument.at("/data/storagePath").asText());

        Files.delete(storedFile);

        mockMvc.perform(post("/api/documents/{id}/process", documentId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DOCUMENT_FILE_NOT_FOUND"));

        mockMvc.perform(get("/api/documents/{id}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.failedStage").value("PARSING"))
                .andExpect(jsonPath("$.data.errorMessage").isNotEmpty());

        Files.createDirectories(storedFile.getParent());
        Files.writeString(storedFile, "abcdefgh", StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/documents/{id}/process", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CHUNKED"))
                .andExpect(jsonPath("$.data.processingVersion").value(2));
    }

    @Test
    void returnsErrorAndMarksDocumentFailedWhenFileTypeIsUnsupported() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase();
        JsonNode uploadedDocument = uploadFile(knowledgeBaseId, "guide.pdf", "%PDF");
        Long documentId = uploadedDocument.at("/data/id").asLong();

        mockMvc.perform(post("/api/documents/{id}/process", documentId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_DOCUMENT_TYPE"));

        mockMvc.perform(get("/api/documents/{id}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.failedStage").value("PARSING"))
                .andExpect(jsonPath("$.data.errorMessage").isNotEmpty());
    }

    @Test
    void indexedDocumentIsNotAllowedToProcess() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase();
        JsonNode uploadedDocument = uploadFile(knowledgeBaseId, "indexed.txt", "abcdefgh");
        Long documentId = uploadedDocument.at("/data/id").asLong();
        jdbcTemplate.update("UPDATE document SET status = 'INDEXED' WHERE id = ?", documentId);

        mockMvc.perform(post("/api/documents/{id}/process", documentId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DOCUMENT_PROCESS_NOT_ALLOWED"));

        mockMvc.perform(get("/api/documents/{id}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INDEXED"));
    }

    @Test
    void returnsErrorWhenChunkDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/chunks/{id}", 999999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DOCUMENT_CHUNK_NOT_FOUND"));
    }

    private Long createKnowledgeBase() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/knowledge-bases")
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "Processing test knowledge base",
                                    "description": "Created for processing tests",
                                    "ownerId": 1,
                                    "visibility": "PRIVATE"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.at("/data/id").asLong();
    }

    private JsonNode uploadFile(Long knowledgeBaseId, String fileName, String content) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult result = mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .param("knowledgeBaseId", knowledgeBaseId.toString()))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private Integer chunkCount(Long documentId, boolean active, int processingVersion) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM document_chunk
                WHERE document_id = ?
                  AND is_active = ?
                  AND processing_version = ?
                """,
                Integer.class,
                documentId,
                active,
                processingVersion
        );
    }
}
