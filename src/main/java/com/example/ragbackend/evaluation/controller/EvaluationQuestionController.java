package com.example.ragbackend.evaluation.controller;

import com.example.ragbackend.common.response.ApiResponse;
import com.example.ragbackend.evaluation.dto.EvaluationQuestionCreateRequest;
import com.example.ragbackend.evaluation.dto.EvaluationQuestionImportRequest;
import com.example.ragbackend.evaluation.dto.EvaluationQuestionResponse;
import com.example.ragbackend.evaluation.service.EvaluationQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Evaluation Question", description = "Evaluation question API")
@RestController
@RequiredArgsConstructor
public class EvaluationQuestionController {

    private final EvaluationQuestionService evaluationQuestionService;

    @Operation(summary = "Create evaluation question")
    @PostMapping("/api/evaluation/datasets/{datasetId}/questions")
    public ApiResponse<EvaluationQuestionResponse> create(
            @PathVariable Long datasetId,
            @Valid @RequestBody EvaluationQuestionCreateRequest request
    ) {
        return ApiResponse.success(evaluationQuestionService.create(datasetId, request));
    }

    @Operation(summary = "Import evaluation questions")
    @PostMapping("/api/evaluation/datasets/{datasetId}/questions/import")
    public ApiResponse<List<EvaluationQuestionResponse>> importQuestions(
            @PathVariable Long datasetId,
            @Valid @RequestBody EvaluationQuestionImportRequest request
    ) {
        return ApiResponse.success(evaluationQuestionService.importQuestions(datasetId, request));
    }

    @Operation(summary = "List evaluation questions by dataset id")
    @GetMapping("/api/evaluation/datasets/{datasetId}/questions")
    public ApiResponse<List<EvaluationQuestionResponse>> findByDatasetId(@PathVariable Long datasetId) {
        return ApiResponse.success(evaluationQuestionService.findByDatasetId(datasetId));
    }

    @Operation(summary = "Get evaluation question by id")
    @GetMapping("/api/evaluation/questions/{id}")
    public ApiResponse<EvaluationQuestionResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(evaluationQuestionService.getById(id));
    }
}
