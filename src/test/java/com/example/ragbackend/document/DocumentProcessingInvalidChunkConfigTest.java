package com.example.ragbackend.document;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
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

@SpringBootTest(properties = {
        "app.chunk.size=4",
        "app.chunk.overlap=4"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentProcessingInvalidChunkConfigTest {

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
    void invalidSplitterOptionsMarkDocumentFailedAtChunking() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase();
        JsonNode uploadedDocument = uploadFile(knowledgeBaseId, "invalid-chunk.txt", "abcdefgh");
        Long documentId = uploadedDocument.at("/data/id").asLong();

        mockMvc.perform(post("/api/documents/{id}/process", documentId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_CHUNK_OPTIONS"));

        mockMvc.perform(get("/api/documents/{id}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.failedStage").value("CHUNKING"))
                .andExpect(jsonPath("$.data.errorMessage").isNotEmpty());
    }

    private Long createKnowledgeBase() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/knowledge-bases")
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "Invalid chunk config knowledge base",
                                    "description": "Created for invalid config tests",
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
}
