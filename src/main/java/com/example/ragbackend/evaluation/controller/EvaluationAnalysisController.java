package com.example.ragbackend.evaluation.controller;

import com.example.ragbackend.common.response.ApiResponse;
import com.example.ragbackend.evaluation.dto.EvaluationBadCaseResponse;
import com.example.ragbackend.evaluation.dto.EvaluationReportSummaryResponse;
import com.example.ragbackend.evaluation.service.EvaluationAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Evaluation Analysis", description = "Evaluation report analysis API")
@RestController
@RequiredArgsConstructor
public class EvaluationAnalysisController {

    private final EvaluationAnalysisService evaluationAnalysisService;

    @Operation(summary = "Get evaluation report summary")
    @GetMapping("/api/evaluation/reports/{reportId}/summary")
    public ApiResponse<EvaluationReportSummaryResponse> getSummary(@PathVariable Long reportId) {
        return ApiResponse.success(evaluationAnalysisService.getSummary(reportId));
    }

    @Operation(summary = "List evaluation bad cases by report id")
    @GetMapping("/api/evaluation/reports/{reportId}/bad-cases")
    public ApiResponse<List<EvaluationBadCaseResponse>> getBadCases(@PathVariable Long reportId) {
        return ApiResponse.success(evaluationAnalysisService.findBadCases(reportId));
    }
}
