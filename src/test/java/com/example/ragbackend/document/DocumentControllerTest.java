package com.example.ragbackend.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.FileSystemUtils;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentControllerTest {

    private static final Path TEST_STORAGE_ROOT = Path.of("target/test-storage/documents");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanData() {
        jdbcTemplate.execute("DELETE FROM document");
        jdbcTemplate.execute("DELETE FROM knowledge_base");
        FileSystemUtils.deleteRecursively(TEST_STORAGE_ROOT.toFile());
    }

    @AfterEach
    void cleanFiles() {
        FileSystemUtils.deleteRecursively(TEST_STORAGE_ROOT.toFile());
    }

    @Test
    void createsDocumentMetadataWithUploadedStatus() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase();

        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(documentRequest(knowledgeBaseId, "guide.pdf")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.knowledgeBaseId").value(knowledgeBaseId))
                .andExpect(jsonPath("$.data.fileName").value("guide.pdf"))
                .andExpect(jsonPath("$.data.fileType").value("pdf"))
                .andExpect(jsonPath("$.data.fileSize").value(1024))
                .andExpect(jsonPath("$.data.storagePath").value("documents/guide.pdf"))
                .andExpect(jsonPath("$.data.status").value("UPLOADED"))
                .andExpect(jsonPath("$.data.chunkCount").value(0))
                .andExpect(jsonPath("$.data.processingVersion").value(1))
                .andExpect(jsonPath("$.data.createdBy").value(1));
    }

    @Test
    void getsDocumentMetadataById() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase();
        Long documentId = createDocument(knowledgeBaseId, "readme.md");

        mockMvc.perform(get("/api/documents/{id}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(documentId))
                .andExpect(jsonPath("$.data.knowledgeBaseId").value(knowledgeBaseId))
                .andExpect(jsonPath("$.data.fileName").value("readme.md"));
    }

    @Test
    void listsDocumentsByKnowledgeBaseId() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase();
        createDocument(knowledgeBaseId, "first.pdf");
        createDocument(knowledgeBaseId, "second.pdf");

        mockMvc.perform(get("/api/knowledge-bases/{knowledgeBaseId}/documents", knowledgeBaseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void deletesDocumentMetadata() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase();
        Long documentId = createDocument(knowledgeBaseId, "delete-me.pdf");

        mockMvc.perform(delete("/api/documents/{id}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"));

        mockMvc.perform(get("/api/documents/{id}", documentId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DOCUMENT_NOT_FOUND"));
    }

    @Test
    void returnsErrorWhenKnowledgeBaseDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(documentRequest(999999L, "missing-base.pdf")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_BASE_NOT_FOUND"));
    }

    @Test
    void returnsErrorWhenDocumentDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/documents/{id}", 999999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DOCUMENT_NOT_FOUND"));
    }

    @Test
    void uploadsFileAndCreatesDocumentMetadata() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "upload-note.txt",
                "text/plain",
                "hello upload".getBytes(StandardCharsets.UTF_8)
        );

        MvcResult result = mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .param("knowledgeBaseId", knowledgeBaseId.toString())
                        .param("createdBy", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.knowledgeBaseId").value(knowledgeBaseId))
                .andExpect(jsonPath("$.data.fileName").value("upload-note.txt"))
                .andExpect(jsonPath("$.data.fileType").value("txt"))
                .andExpect(jsonPath("$.data.fileSize").value(12))
                .andExpect(jsonPath("$.data.status").value("UPLOADED"))
                .andExpect(jsonPath("$.data.createdBy").value(7))
                .andExpect(jsonPath("$.data.storagePath").isNotEmpty())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        String storagePath = response.at("/data/storagePath").asText();
        Path storedFile = TEST_STORAGE_ROOT.resolve(storagePath);

        assertThat(storedFile).exists();
        assertThat(Files.readString(storedFile)).isEqualTo("hello upload");
    }

    @Test
    void returnsErrorWhenUploadingEmptyFile() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .param("knowledgeBaseId", knowledgeBaseId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FILE_EMPTY"));
    }

    @Test
    void returnsErrorWhenUploadingToMissingKnowledgeBase() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "missing-base.txt",
                "text/plain",
                "content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .param("knowledgeBaseId", "999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_BASE_NOT_FOUND"));
    }

    private Long createKnowledgeBase() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Document test knowledge base",
                                  "description": "Created for document tests",
                                  "ownerId": 1,
                                  "visibility": "PRIVATE"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.at("/data/id").asLong();
    }

    private Long createDocument(Long knowledgeBaseId, String fileName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(documentRequest(knowledgeBaseId, fileName)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.at("/data/id").asLong();
    }

    private String documentRequest(Long knowledgeBaseId, String fileName) {
        return """
                {
                  "knowledgeBaseId": %d,
                  "fileName": "%s",
                  "fileType": "pdf",
                  "fileSize": 1024,
                  "storagePath": "documents/%s",
                  "createdBy": 1
                }
                """.formatted(knowledgeBaseId, fileName, fileName);
    }
}
