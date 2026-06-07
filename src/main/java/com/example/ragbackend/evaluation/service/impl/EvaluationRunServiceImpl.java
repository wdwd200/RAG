package com.example.ragbackend.evaluation.service.impl;

import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.evaluation.dto.EvaluationDatasetResponse;
import com.example.ragbackend.evaluation.dto.EvaluationQuestionResponse;
import com.example.ragbackend.evaluation.dto.EvaluationQuestionResultCreateRequest;
import com.example.ragbackend.evaluation.dto.EvaluationReportResponse;
import com.example.ragbackend.evaluation.dto.EvaluationRunRequest;
import com.example.ragbackend.evaluation.metric.QuestionMetricResult;
import com.example.ragbackend.evaluation.metric.RetrievalMetricResult;
import com.example.ragbackend.evaluation.metric.RetrievalMetricsCalculator;
import com.example.ragbackend.evaluation.service.EvaluationDatasetService;
import com.example.ragbackend.evaluation.service.EvaluationQuestionService;
import com.example.ragbackend.evaluation.service.EvaluationReportService;
import com.example.ragbackend.evaluation.service.EvaluationRunService;
import com.example.ragbackend.retrieval.dto.RetrieveRequest;
import com.example.ragbackend.retrieval.dto.RetrieveResponse;
import com.example.ragbackend.retrieval.dto.RetrievedChunk;
import com.example.ragbackend.retrieval.service.RetrievalService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EvaluationRunServiceImpl implements EvaluationRunService {

    static final int DEFAULT_TOP_K = 5;
    static final int MAX_TOP_K = 20;

    private static final String EVALUATION_RUN_REQUEST_INVALID_CODE = "EVALUATION_RUN_REQUEST_INVALID";
    private static final String EVALUATION_DATASET_EMPTY_CODE = "EVALUATION_DATASET_EMPTY";

    private final EvaluationDatasetService evaluationDatasetService;
    private final EvaluationQuestionService evaluationQuestionService;
    private final EvaluationReportService evaluationReportService;
    private final RetrievalService retrievalService;
    private final RetrievalMetricsCalculator retrievalMetricsCalculator;

    @Override
    public EvaluationReportResponse runRetrievalEvaluation(Long datasetId, EvaluationRunRequest request) {
        int topK = resolveTopK(request);
        EvaluationDatasetResponse dataset = evaluationDatasetService.getById(datasetId);
        List<EvaluationQuestionResponse> questions = evaluationQuestionService.findByDatasetId(datasetId);
        if (questions.isEmpty()) {
            throw new BusinessException(EVALUATION_DATASET_EMPTY_CODE, "Evaluation dataset has no questions: " + datasetId);
        }

        EvaluationReportResponse runningReport = evaluationReportService.createRunningReport(
                dataset.id(),
                dataset.knowledgeBaseId(),
                topK,
                questions.size()
        );

        try {
            List<QuestionMetricResult> questionMetricResults = new ArrayList<>();
            List<EvaluationQuestionResultCreateRequest> questionResultRequests = new ArrayList<>();
            for (EvaluationQuestionResponse question : questions) {
                RetrieveResponse retrieveResponse = retrievalService.retrieve(
                        new RetrieveRequest(dataset.knowledgeBaseId(), question.question(), topK)
                );
                List<Long> retrievedChunkIds = retrieveResponse.chunks()
                        .stream()
                        .map(RetrievedChunk::chunkId)
                        .toList();
                QuestionMetricResult questionMetricResult = retrievalMetricsCalculator.calculateQuestion(
                        question.id(),
                        question.relevantChunkIds(),
                        retrievedChunkIds
                );
                questionMetricResults.add(questionMetricResult);
                questionResultRequests.add(toCreateRequest(question, questionMetricResult));
            }

            RetrievalMetricResult metricResult = retrievalMetricsCalculator.summarize(questionMetricResults);
            evaluationReportService.saveQuestionResults(runningReport.id(), questionResultRequests);
            return evaluationReportService.markSuccess(runningReport.id(), metricResult);
        } catch (RuntimeException ex) {
            return evaluationReportService.markFailed(runningReport.id(), ex.getMessage());
        }
    }

    private int resolveTopK(EvaluationRunRequest request) {
        if (request == null || request.topK() == null) {
            return DEFAULT_TOP_K;
        }
        if (request.topK() < 1 || request.topK() > MAX_TOP_K) {
            throw new BusinessException(
                    EVALUATION_RUN_REQUEST_INVALID_CODE,
                    "Evaluation topK must be between 1 and " + MAX_TOP_K
            );
        }
        return request.topK();
    }

    private EvaluationQuestionResultCreateRequest toCreateRequest(
            EvaluationQuestionResponse question,
            QuestionMetricResult metricResult
    ) {
        return new EvaluationQuestionResultCreateRequest(
                question.id(),
                question.question(),
                metricResult.expectedChunkIds(),
                metricResult.retrievedChunkIds(),
                metricResult.hit(),
                metricResult.reciprocalRank(),
                metricResult.recallAtK(),
                metricResult.rankedHitPosition()
        );
    }
}
