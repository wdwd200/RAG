package com.example.ragbackend.evaluation.service;

import com.example.ragbackend.evaluation.dto.EvaluationReportResponse;
import com.example.ragbackend.evaluation.dto.EvaluationRunRequest;

public interface EvaluationRunService {

    EvaluationReportResponse runRetrievalEvaluation(Long datasetId, EvaluationRunRequest request);
}
