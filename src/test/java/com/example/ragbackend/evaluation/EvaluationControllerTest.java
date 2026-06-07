package com.example.ragbackend.evaluation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ragbackend.chunk.entity.DocumentChunk;
import com.example.ragbackend.chunk.mapper.DocumentChunkMapper;
import com.example.ragbackend.document.entity.Document;
import com.example.ragbackend.document.enums.DocumentStatus;
import com.example.ragbackend.document.mapper.DocumentMapper;
import com.example.ragbackend.knowledge.entity.KnowledgeBase;
import com.example.ragbackend.knowledge.mapper.KnowledgeBaseMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EvaluationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private DocumentChunkMapper documentChunkMapper;

    @BeforeEach
    void cleanData() {
        jdbcTemplate.execute("DELETE FROM evaluation_question_result");
        jdbcTemplate.execute("DELETE FROM evaluation_report");
        jdbcTemplate.execute("DELETE FROM evaluation_question");
        jdbcTemplate.execute("DELETE FROM evaluation_dataset");
        jdbcTemplate.execute("DELETE FROM document_chunk");
        jdbcTemplate.execute("DELETE FROM document");
        jdbcTemplate.execute("DELETE FROM knowledge_base");
    }

    @Test
    void createsEvaluationDataset() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase("Evaluation KB");

        mockMvc.perform(post("/api/evaluation/datasets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "HR handbook evaluation",
                                  "knowledgeBaseId": %d,
                                  "description": "Manual questions for HR handbook"
                                }
                                """.formatted(knowledgeBaseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.name").value("HR handbook evaluation"))
                .andExpect(jsonPath("$.data.knowledgeBaseId").value(knowledgeBaseId))
                .andExpect(jsonPath("$.data.description").value("Manual questions for HR handbook"))
                .andExpect(jsonPath("$.data.questionCount").value(0));
    }

    @Test
    void rejectsDatasetWhenKnowledgeBaseDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/evaluation/datasets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Missing KB evaluation",
                                  "knowledgeBaseId": 999999
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_BASE_NOT_FOUND"));
    }

    @Test
    void rejectsDatasetWhenKnowledgeBaseIdIsMissing() throws Exception {
        mockMvc.perform(post("/api/evaluation/datasets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Missing knowledge base id"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createsEvaluationQuestionAndRecordsChunkFacts() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase("Question KB");
        Long documentId = createDocument(knowledgeBaseId, "hr-handbook.txt");
        Long chunkId = createChunk(knowledgeBaseId, documentId, 0, "Annual leave carries to Q1.", true, 2);
        Long datasetId = createDataset(knowledgeBaseId, "Question dataset");

        mockMvc.perform(post("/api/evaluation/datasets/{datasetId}/questions", datasetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "How long can annual leave carry over?",
                                  "groundTruthAnswer": "Annual leave can carry over to Q1.",
                                  "relevantChunkIds": [%d],
                                  "questionType": "fact"
                                }
                                """.formatted(chunkId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.datasetId").value(datasetId))
                .andExpect(jsonPath("$.data.question").value("How long can annual leave carry over?"))
                .andExpect(jsonPath("$.data.groundTruthAnswer").value("Annual leave can carry over to Q1."))
                .andExpect(jsonPath("$.data.relevantChunkIds[0]").value(chunkId))
                .andExpect(jsonPath("$.data.relevantContentHashes[0]").value("hash-0"))
                .andExpect(jsonPath("$.data.documentProcessingVersion").value(2))
                .andExpect(jsonPath("$.data.questionType").value("fact"));
    }

    @Test
    void rejectsQuestionWhenRelevantChunkIdsAreEmpty() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase("Empty chunk ids KB");
        Long datasetId = createDataset(knowledgeBaseId, "Empty chunk ids dataset");

        mockMvc.perform(post("/api/evaluation/datasets/{datasetId}/questions", datasetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "Question without chunks",
                                  "relevantChunkIds": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsQuestionWhenRelevantChunkDoesNotExist() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase("Missing chunk KB");
        Long datasetId = createDataset(knowledgeBaseId, "Missing chunk dataset");

        mockMvc.perform(post("/api/evaluation/datasets/{datasetId}/questions", datasetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "Question with missing chunk",
                                  "relevantChunkIds": [999999]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("EVALUATION_RELEVANT_CHUNKS_NOT_FOUND"));
    }

    @Test
    void rejectsQuestionWhenRelevantChunkIsInactive() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase("Inactive chunk KB");
        Long documentId = createDocument(knowledgeBaseId, "inactive.txt");
        Long inactiveChunkId = createChunk(knowledgeBaseId, documentId, 0, "Inactive content", false, 1);
        Long datasetId = createDataset(knowledgeBaseId, "Inactive chunk dataset");

        mockMvc.perform(post("/api/evaluation/datasets/{datasetId}/questions", datasetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "Question with inactive chunk",
                                  "relevantChunkIds": [%d]
                                }
                                """.formatted(inactiveChunkId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("EVALUATION_RELEVANT_CHUNKS_NOT_FOUND"));
    }

    @Test
    void rejectsQuestionWhenRelevantChunkBelongsToAnotherKnowledgeBase() throws Exception {
        Long datasetKnowledgeBaseId = createKnowledgeBase("Dataset KB");
        Long otherKnowledgeBaseId = createKnowledgeBase("Other KB");
        Long otherDocumentId = createDocument(otherKnowledgeBaseId, "other.txt");
        Long otherChunkId = createChunk(otherKnowledgeBaseId, otherDocumentId, 0, "Other KB content", true, 1);
        Long datasetId = createDataset(datasetKnowledgeBaseId, "Dataset with wrong KB chunk");

        mockMvc.perform(post("/api/evaluation/datasets/{datasetId}/questions", datasetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "Question with wrong KB chunk",
                                  "relevantChunkIds": [%d]
                                }
                                """.formatted(otherChunkId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("EVALUATION_RELEVANT_CHUNK_KB_MISMATCH"));
    }

    @Test
    void creatingQuestionUpdatesDatasetQuestionCount() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase("Count KB");
        Long documentId = createDocument(knowledgeBaseId, "count.txt");
        Long chunkId = createChunk(knowledgeBaseId, documentId, 0, "Counted content", true, 1);
        Long datasetId = createDataset(knowledgeBaseId, "Count dataset");

        createQuestion(datasetId, chunkId, "First counted question");

        mockMvc.perform(get("/api/evaluation/datasets/{id}", datasetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(datasetId))
                .andExpect(jsonPath("$.data.questionCount").value(1));
    }

    @Test
    void importsQuestionsInBatch() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase("Import KB");
        Long documentId = createDocument(knowledgeBaseId, "import.txt");
        Long firstChunkId = createChunk(knowledgeBaseId, documentId, 0, "First import content", true, 3);
        Long secondChunkId = createChunk(knowledgeBaseId, documentId, 1, "Second import content", true, 3);
        Long datasetId = createDataset(knowledgeBaseId, "Import dataset");

        mockMvc.perform(post("/api/evaluation/datasets/{datasetId}/questions/import", datasetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questions": [
                                    {
                                      "question": "First imported question",
                                      "groundTruthAnswer": "First answer",
                                      "relevantChunkIds": [%d],
                                      "questionType": "fact"
                                    },
                                    {
                                      "question": "Second imported question",
                                      "groundTruthAnswer": "Second answer",
                                      "relevantChunkIds": [%d],
                                      "questionType": "fact"
                                    }
                                  ]
                                }
                                """.formatted(firstChunkId, secondChunkId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].question").value("First imported question"))
                .andExpect(jsonPath("$.data[0].relevantContentHashes[0]").value("hash-0"))
                .andExpect(jsonPath("$.data[1].question").value("Second imported question"))
                .andExpect(jsonPath("$.data[1].relevantContentHashes[0]").value("hash-1"));

        mockMvc.perform(get("/api/evaluation/datasets/{id}", datasetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questionCount").value(2));
    }

    @Test
    void listsDatasetQuestions() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase("List KB");
        Long documentId = createDocument(knowledgeBaseId, "list.txt");
        Long firstChunkId = createChunk(knowledgeBaseId, documentId, 0, "First list content", true, 1);
        Long secondChunkId = createChunk(knowledgeBaseId, documentId, 1, "Second list content", true, 1);
        Long datasetId = createDataset(knowledgeBaseId, "List dataset");
        createQuestion(datasetId, firstChunkId, "First listed question");
        createQuestion(datasetId, secondChunkId, "Second listed question");

        mockMvc.perform(get("/api/evaluation/datasets/{datasetId}/questions", datasetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].question").value("First listed question"))
                .andExpect(jsonPath("$.data[1].question").value("Second listed question"));
    }

    @Test
    void returnsClearErrorWhenQuestionDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/evaluation/questions/{id}", 999999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("EVALUATION_QUESTION_NOT_FOUND"));
    }

    private Long createKnowledgeBase(String name) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setName(name);
        knowledgeBase.setDescription("Created for evaluation tests");
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
        document.setStatus(DocumentStatus.CHUNKED.name());
        document.setChunkCount(0);
        document.setProcessingVersion(1);
        document.setCreatedBy(1L);
        documentMapper.insert(document);
        return document.getId();
    }

    private Long createChunk(
            Long knowledgeBaseId,
            Long documentId,
            Integer chunkIndex,
            String content,
            Boolean isActive,
            Integer processingVersion
    ) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setKnowledgeBaseId(knowledgeBaseId);
        chunk.setDocumentId(documentId);
        chunk.setChunkIndex(chunkIndex);
        chunk.setContent(content);
        chunk.setContentHash("hash-" + chunkIndex);
        chunk.setProcessingVersion(processingVersion);
        chunk.setIsActive(isActive);
        chunk.setTokenCount(3);
        chunk.setMetadataJson("{\"source\":\"evaluation-test\"}");
        documentChunkMapper.insert(chunk);
        return chunk.getId();
    }

    private Long createDataset(Long knowledgeBaseId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/evaluation/datasets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "knowledgeBaseId": %d,
                                  "description": "Created for evaluation question tests"
                                }
                                """.formatted(name, knowledgeBaseId)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.at("/data/id").asLong();
    }

    private Long createQuestion(Long datasetId, Long chunkId, String question) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/evaluation/datasets/{datasetId}/questions", datasetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "%s",
                                  "groundTruthAnswer": "Expected answer",
                                  "relevantChunkIds": [%d],
                                  "questionType": "fact"
                                }
                                """.formatted(question, chunkId)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.at("/data/id").asLong();
    }
}
