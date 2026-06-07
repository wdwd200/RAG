package com.example.ragbackend.evaluation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ragbackend.chunk.entity.DocumentChunk;
import com.example.ragbackend.chunk.mapper.DocumentChunkMapper;
import com.example.ragbackend.document.entity.Document;
import com.example.ragbackend.document.enums.DocumentStatus;
import com.example.ragbackend.document.mapper.DocumentMapper;
import com.example.ragbackend.evaluation.entity.EvaluationDataset;
import com.example.ragbackend.evaluation.entity.EvaluationQuestion;
import com.example.ragbackend.evaluation.mapper.EvaluationDatasetMapper;
import com.example.ragbackend.evaluation.mapper.EvaluationQuestionMapper;
import com.example.ragbackend.knowledge.entity.KnowledgeBase;
import com.example.ragbackend.knowledge.mapper.KnowledgeBaseMapper;
import com.example.ragbackend.retrieval.dto.RetrieveRequest;
import com.example.ragbackend.retrieval.dto.RetrieveResponse;
import com.example.ragbackend.retrieval.dto.RetrievedChunk;
import com.example.ragbackend.retrieval.service.RetrievalService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EvaluationRunControllerTest {

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

    @Autowired
    private EvaluationDatasetMapper evaluationDatasetMapper;

    @Autowired
    private EvaluationQuestionMapper evaluationQuestionMapper;

    @MockBean
    private RetrievalService retrievalService;

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
    void runsRetrievalEvaluationAndSavesReportAndQuestionResults() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase();
        Long documentId = createDocument(knowledgeBaseId);
        Long firstChunkId = createChunk(knowledgeBaseId, documentId, 0, "First relevant chunk");
        Long secondChunkId = createChunk(knowledgeBaseId, documentId, 1, "Second relevant chunk");
        Long distractorChunkId = createChunk(knowledgeBaseId, documentId, 2, "Distractor chunk");
        Long datasetId = createDataset(knowledgeBaseId, "Run dataset");
        Long firstQuestionId = createQuestion(datasetId, "First question", List.of(firstChunkId));
        Long secondQuestionId = createQuestion(datasetId, "Second question", List.of(secondChunkId));

        when(retrievalService.retrieve(any(RetrieveRequest.class))).thenAnswer(invocation -> {
            RetrieveRequest request = invocation.getArgument(0);
            if (request.question().equals("First question")) {
                return retrieveResponse(knowledgeBaseId, request.question(), request.topK(), List.of(
                        retrievedChunk(distractorChunkId, documentId, knowledgeBaseId, 2),
                        retrievedChunk(firstChunkId, documentId, knowledgeBaseId, 0)
                ));
            }
            return retrieveResponse(knowledgeBaseId, request.question(), request.topK(), List.of(
                    retrievedChunk(secondChunkId, documentId, knowledgeBaseId, 1),
                    retrievedChunk(distractorChunkId, documentId, knowledgeBaseId, 2)
            ));
        });

        MvcResult result = mockMvc.perform(post("/api/evaluation/datasets/{datasetId}/run", datasetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topK": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.datasetId").value(datasetId))
                .andExpect(jsonPath("$.data.knowledgeBaseId").value(knowledgeBaseId))
                .andExpect(jsonPath("$.data.topK").value(5))
                .andExpect(jsonPath("$.data.questionCount").value(2))
                .andExpect(jsonPath("$.data.recallAtK").value(1.0d))
                .andExpect(jsonPath("$.data.hitRateAtK").value(1.0d))
                .andExpect(jsonPath("$.data.mrr").value(0.75d))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.questionResultCount").value(2))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        Long reportId = response.at("/data/id").asLong();

        mockMvc.perform(get("/api/evaluation/reports/{reportId}", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(reportId))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.questionResultCount").value(2));

        mockMvc.perform(get("/api/evaluation/reports/{reportId}/question-results", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].questionId").value(firstQuestionId))
                .andExpect(jsonPath("$.data[0].expectedChunkIds[0]").value(firstChunkId))
                .andExpect(jsonPath("$.data[0].retrievedChunkIds[0]").value(distractorChunkId))
                .andExpect(jsonPath("$.data[0].retrievedChunkIds[1]").value(firstChunkId))
                .andExpect(jsonPath("$.data[0].hit").value(true))
                .andExpect(jsonPath("$.data[0].rankedHitPosition").value(2))
                .andExpect(jsonPath("$.data[0].reciprocalRank").value(0.5d))
                .andExpect(jsonPath("$.data[1].questionId").value(secondQuestionId))
                .andExpect(jsonPath("$.data[1].retrievedChunkIds[0]").value(secondChunkId))
                .andExpect(jsonPath("$.data[1].rankedHitPosition").value(1))
                .andExpect(jsonPath("$.data[1].reciprocalRank").value(1.0d));
    }

    @Test
    void returnsClearErrorWhenDatasetDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/evaluation/datasets/{datasetId}/run", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topK": 5
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("EVALUATION_DATASET_NOT_FOUND"));
    }

    @Test
    void returnsClearErrorWhenReportDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/evaluation/reports/{reportId}", 999999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("EVALUATION_REPORT_NOT_FOUND"));
    }

    @Test
    void returnsClearErrorWhenDatasetHasNoQuestions() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase();
        Long datasetId = createDataset(knowledgeBaseId, "Empty run dataset");

        mockMvc.perform(post("/api/evaluation/datasets/{datasetId}/run", datasetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topK": 5
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("EVALUATION_DATASET_EMPTY"));
    }

    @Test
    void rejectsTopKAboveMaximum() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase();
        Long datasetId = createDataset(knowledgeBaseId, "TopK dataset");

        mockMvc.perform(post("/api/evaluation/datasets/{datasetId}/run", datasetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topK": 21
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void usesDefaultTopKWhenRequestBodyIsEmptyObject() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase();
        Long documentId = createDocument(knowledgeBaseId);
        Long chunkId = createChunk(knowledgeBaseId, documentId, 0, "Default topK chunk");
        Long datasetId = createDataset(knowledgeBaseId, "Default topK dataset");
        createQuestion(datasetId, "Default topK question", List.of(chunkId));

        when(retrievalService.retrieve(any(RetrieveRequest.class))).thenAnswer(invocation -> {
            RetrieveRequest request = invocation.getArgument(0);
            return retrieveResponse(knowledgeBaseId, request.question(), request.topK(), List.of(
                    retrievedChunk(chunkId, documentId, knowledgeBaseId, 0)
            ));
        });

        mockMvc.perform(post("/api/evaluation/datasets/{datasetId}/run", datasetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.topK").value(5))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    private Long createKnowledgeBase() {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setName("Evaluation run KB");
        knowledgeBase.setDescription("Created for evaluation run tests");
        knowledgeBase.setOwnerId(1L);
        knowledgeBase.setVisibility("PRIVATE");
        knowledgeBaseMapper.insert(knowledgeBase);
        return knowledgeBase.getId();
    }

    private Long createDocument(Long knowledgeBaseId) {
        Document document = new Document();
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setFileName("evaluation-run.txt");
        document.setFileType("txt");
        document.setFileSize(1024L);
        document.setStoragePath("documents/evaluation-run.txt");
        document.setStatus(DocumentStatus.INDEXED.name());
        document.setChunkCount(3);
        document.setProcessingVersion(1);
        document.setCreatedBy(1L);
        documentMapper.insert(document);
        return document.getId();
    }

    private Long createChunk(Long knowledgeBaseId, Long documentId, Integer chunkIndex, String content) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setKnowledgeBaseId(knowledgeBaseId);
        chunk.setDocumentId(documentId);
        chunk.setChunkIndex(chunkIndex);
        chunk.setContent(content);
        chunk.setContentHash("run-hash-" + chunkIndex);
        chunk.setProcessingVersion(1);
        chunk.setIsActive(true);
        chunk.setTokenCount(3);
        chunk.setMetadataJson("{\"source\":\"evaluation-run-test\"}");
        documentChunkMapper.insert(chunk);
        return chunk.getId();
    }

    private Long createDataset(Long knowledgeBaseId, String name) {
        EvaluationDataset dataset = new EvaluationDataset();
        dataset.setName(name);
        dataset.setKnowledgeBaseId(knowledgeBaseId);
        dataset.setDescription("Created for evaluation run tests");
        dataset.setQuestionCount(0);
        evaluationDatasetMapper.insert(dataset);
        return dataset.getId();
    }

    private Long createQuestion(Long datasetId, String questionText, List<Long> relevantChunkIds) throws Exception {
        EvaluationQuestion question = new EvaluationQuestion();
        question.setDatasetId(datasetId);
        question.setQuestion(questionText);
        question.setGroundTruthAnswer("Expected answer");
        question.setRelevantChunkIdsJson(objectMapper.writeValueAsString(relevantChunkIds));
        question.setRelevantContentHashesJson("[]");
        question.setDocumentProcessingVersion(1);
        question.setQuestionType("fact");
        evaluationQuestionMapper.insert(question);
        return question.getId();
    }

    private RetrieveResponse retrieveResponse(
            Long knowledgeBaseId,
            String question,
            Integer topK,
            List<RetrievedChunk> chunks
    ) {
        return new RetrieveResponse(knowledgeBaseId, question, topK, chunks);
    }

    private RetrievedChunk retrievedChunk(
            Long chunkId,
            Long documentId,
            Long knowledgeBaseId,
            Integer chunkIndex
    ) {
        return new RetrievedChunk(
                chunkId,
                documentId,
                knowledgeBaseId,
                chunkIndex,
                0.9d,
                "Retrieved content " + chunkIndex
        );
    }
}
