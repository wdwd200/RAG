package com.example.ragbackend.evaluation.service;

import com.example.ragbackend.evaluation.dto.EvaluationQuestionCreateRequest;
import com.example.ragbackend.evaluation.dto.EvaluationQuestionImportRequest;
import com.example.ragbackend.evaluation.dto.EvaluationQuestionResponse;
import java.util.List;

public interface EvaluationQuestionService {

    EvaluationQuestionResponse create(Long datasetId, EvaluationQuestionCreateRequest request);

    List<EvaluationQuestionResponse> importQuestions(Long datasetId, EvaluationQuestionImportRequest request);

    List<EvaluationQuestionResponse> findByDatasetId(Long datasetId);

    EvaluationQuestionResponse getById(Long id);
}
