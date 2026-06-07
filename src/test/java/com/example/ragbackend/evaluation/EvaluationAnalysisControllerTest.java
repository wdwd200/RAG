package com.example.ragbackend.evaluation;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ragbackend.evaluation.entity.EvaluationQuestionResult;
import com.example.ragbackend.evaluation.entity.EvaluationReport;
import com.example.ragbackend.evaluation.mapper.EvaluationQuestionResultMapper;
import com.example.ragbackend.evaluation.mapper.EvaluationReportMapper;
import com.example.ragbackend.llm.service.LlmService;
import com.example.ragbackend.retrieval.service.RetrievalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EvaluationAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EvaluationReportMapper evaluationReportMapper;

    @Autowired
    private EvaluationQuestionResultMapper evaluationQuestionResultMapper;

    @MockBean
    private RetrievalService retrievalService;

    @MockBean
    private LlmService llmService;

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
    void queriesBadCasesAndSortsBySeverity() throws Exception {
        Long reportId = createReport();
        createQuestionResult(reportId, 101L, "No hit question", List.of(10L), List.of(20L, 30L),
                false, 0.0d, 0.0d, null);
        createQuestionResult(reportId, 102L, "Low recall question", List.of(40L, 50L), List.of(40L, 60L),
                true, 1.0d, 0.5d, 1);
        createQuestionResult(reportId, 103L, "Low rank question", List.of(70L), List.of(80L, 90L, 70L),
                true, 1.0d / 3.0d, 1.0d, 3);
        createQuestionResult(reportId, 104L, "Good question", List.of(100L), List.of(100L),
                true, 1.0d, 1.0d, 1);

        mockMvc.perform(get("/api/evaluation/reports/{reportId}/bad-cases", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].questionId").value(101))
                .andExpect(jsonPath("$.data[0].failureReason").value("NO_HIT"))
                .andExpect(jsonPath("$.data[0].expectedChunkIds[0]").value(10))
                .andExpect(jsonPath("$.data[0].retrievedChunkIds[0]").value(20))
                .andExpect(jsonPath("$.data[1].questionId").value(102))
                .andExpect(jsonPath("$.data[1].failureReason").value("LOW_RECALL"))
                .andExpect(jsonPath("$.data[1].recallAtK").value(0.5d))
                .andExpect(jsonPath("$.data[2].questionId").value(103))
                .andExpect(jsonPath("$.data[2].failureReason").value("LOW_RANK"))
                .andExpect(jsonPath("$.data[2].rankedHitPosition").value(3));

        verifyNoInteractions(retrievalService, llmService);
    }

    @Test
    void excludesFullyHitFirstRankQuestionsFromBadCases() throws Exception {
        Long reportId = createReport();
        createQuestionResult(reportId, 201L, "Good question", List.of(100L), List.of(100L, 200L),
                true, 1.0d, 1.0d, 1);

        mockMvc.perform(get("/api/evaluation/reports/{reportId}/bad-cases", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(0));

        verifyNoInteractions(retrievalService, llmService);
    }

    @Test
    void reportSummaryCountsBadCaseTypes() throws Exception {
        Long reportId = createReport();
        createQuestionResult(reportId, 301L, "No hit question", List.of(10L), List.of(20L),
                false, 0.0d, 0.0d, null);
        createQuestionResult(reportId, 302L, "Low recall question", List.of(40L, 50L), List.of(40L),
                true, 1.0d, 0.5d, 1);
        createQuestionResult(reportId, 303L, "Low rank question", List.of(70L), List.of(80L, 70L),
                true, 0.5d, 1.0d, 2);
        createQuestionResult(reportId, 304L, "Good question", List.of(90L), List.of(90L),
                true, 1.0d, 1.0d, 1);

        mockMvc.perform(get("/api/evaluation/reports/{reportId}/summary", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reportId").value(reportId))
                .andExpect(jsonPath("$.data.badCaseCount").value(3))
                .andExpect(jsonPath("$.data.noHitCount").value(1))
                .andExpect(jsonPath("$.data.lowRecallCount").value(1))
                .andExpect(jsonPath("$.data.lowRankCount").value(1))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));

        verifyNoInteractions(retrievalService, llmService);
    }

    @Test
    void returnsClearErrorWhenReportDoesNotExistForBadCases() throws Exception {
        mockMvc.perform(get("/api/evaluation/reports/{reportId}/bad-cases", 999999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("EVALUATION_REPORT_NOT_FOUND"));

        verifyNoInteractions(retrievalService, llmService);
    }

    @Test
    void questionResultsReturnChunkIdsAsLists() throws Exception {
        Long reportId = createReport();
        createQuestionResult(reportId, 401L, "List shape question", List.of(10L, 20L), List.of(30L, 10L),
                true, 0.5d, 0.5d, 2);

        mockMvc.perform(get("/api/evaluation/reports/{reportId}/question-results", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].expectedChunkIds[0]").value(10))
                .andExpect(jsonPath("$.data[0].expectedChunkIds[1]").value(20))
                .andExpect(jsonPath("$.data[0].retrievedChunkIds[0]").value(30))
                .andExpect(jsonPath("$.data[0].retrievedChunkIds[1]").value(10))
                .andExpect(jsonPath("$.data[0].question").value("List shape question"))
                .andExpect(jsonPath("$.data[0].rankedHitPosition").value(2));

        verifyNoInteractions(retrievalService, llmService);
    }

    private Long createReport() {
        EvaluationReport report = new EvaluationReport();
        report.setDatasetId(1L);
        report.setKnowledgeBaseId(10L);
        report.setTopK(5);
        report.setQuestionCount(4);
        report.setRecallAtK(0.75d);
        report.setHitRateAtK(0.75d);
        report.setMrr(0.625d);
        report.setStatus("SUCCESS");
        report.setFinishedAt(LocalDateTime.now());
        evaluationReportMapper.insert(report);
        return report.getId();
    }

    private Long createQuestionResult(
            Long reportId,
            Long questionId,
            String question,
            List<Long> expectedChunkIds,
            List<Long> retrievedChunkIds,
            Boolean hit,
            Double reciprocalRank,
            Double recallAtK,
            Integer rankedHitPosition
    ) throws Exception {
        EvaluationQuestionResult result = new EvaluationQuestionResult();
        result.setReportId(reportId);
        result.setQuestionId(questionId);
        result.setQuestion(question);
        result.setExpectedChunkIdsJson(objectMapper.writeValueAsString(expectedChunkIds));
        result.setRetrievedChunkIdsJson(objectMapper.writeValueAsString(retrievedChunkIds));
        result.setHit(hit);
        result.setReciprocalRank(reciprocalRank);
        result.setRecallAtK(recallAtK);
        result.setRankedHitPosition(rankedHitPosition);
        evaluationQuestionResultMapper.insert(result);
        return result.getId();
    }
}
