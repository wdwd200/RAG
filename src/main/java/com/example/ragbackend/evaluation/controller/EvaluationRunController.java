package com.example.ragbackend.evaluation.controller;

import com.example.ragbackend.common.response.ApiResponse;
import com.example.ragbackend.evaluation.dto.EvaluationQuestionResultResponse;
import com.example.ragbackend.evaluation.dto.EvaluationReportResponse;
import com.example.ragbackend.evaluation.dto.EvaluationRunRequest;
import com.example.ragbackend.evaluation.service.EvaluationReportService;
import com.example.ragbackend.evaluation.service.EvaluationRunService;
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

@Tag(name = "Evaluation Run", description = "Retrieval evaluation run API")
@RestController
@RequiredArgsConstructor
public class EvaluationRunController {

    private final EvaluationRunService evaluationRunService;
    private final EvaluationReportService evaluationReportService;

    @Operation(summary = "Run retrieval evaluation for a dataset")
    @PostMapping("/api/evaluation/datasets/{datasetId}/run")
    public ApiResponse<EvaluationReportResponse> run(
            @PathVariable Long datasetId,
            @Valid @RequestBody(required = false) EvaluationRunRequest request
    ) {
        return ApiResponse.success(evaluationRunService.runRetrievalEvaluation(datasetId, request));
    }

    @Operation(summary = "Get evaluation report by id")
    @GetMapping("/api/evaluation/reports/{reportId}")
    public ApiResponse<EvaluationReportResponse> getReport(@PathVariable Long reportId) {
        return ApiResponse.success(evaluationReportService.getById(reportId));
    }

    @Operation(summary = "List question results by report id")
    @GetMapping("/api/evaluation/reports/{reportId}/question-results")
    public ApiResponse<List<EvaluationQuestionResultResponse>> getQuestionResults(@PathVariable Long reportId) {
        return ApiResponse.success(evaluationReportService.findQuestionResults(reportId));
    }
}
