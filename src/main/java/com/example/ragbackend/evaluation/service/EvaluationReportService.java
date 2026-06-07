package com.example.ragbackend.evaluation.service;

import com.example.ragbackend.evaluation.dto.EvaluationQuestionResultCreateRequest;
import com.example.ragbackend.evaluation.dto.EvaluationQuestionResultResponse;
import com.example.ragbackend.evaluation.dto.EvaluationReportResponse;
import com.example.ragbackend.evaluation.metric.RetrievalMetricResult;
import java.util.List;

public interface EvaluationReportService {

    EvaluationReportResponse createRunningReport(
            Long datasetId,
            Long knowledgeBaseId,
            Integer topK,
            Integer questionCount
    );

    EvaluationReportResponse markSuccess(Long reportId, RetrievalMetricResult metricResult);

    EvaluationReportResponse markFailed(Long reportId, String errorMessage);

    void saveQuestionResults(Long reportId, List<EvaluationQuestionResultCreateRequest> requests);

    EvaluationReportResponse getById(Long id);

    List<EvaluationQuestionResultResponse> findQuestionResults(Long reportId);
}
