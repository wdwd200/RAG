package com.example.ragbackend.evaluation.service;

import com.example.ragbackend.evaluation.dto.EvaluationDatasetCreateRequest;
import com.example.ragbackend.evaluation.dto.EvaluationDatasetResponse;
import java.util.List;

public interface EvaluationDatasetService {

    EvaluationDatasetResponse create(EvaluationDatasetCreateRequest request);

    List<EvaluationDatasetResponse> findAll();

    EvaluationDatasetResponse getById(Long id);

    void incrementQuestionCount(Long id, int delta);
}
