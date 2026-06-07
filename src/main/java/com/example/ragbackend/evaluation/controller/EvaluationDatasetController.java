package com.example.ragbackend.evaluation.controller;

import com.example.ragbackend.common.response.ApiResponse;
import com.example.ragbackend.evaluation.dto.EvaluationDatasetCreateRequest;
import com.example.ragbackend.evaluation.dto.EvaluationDatasetResponse;
import com.example.ragbackend.evaluation.service.EvaluationDatasetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Evaluation Dataset", description = "Evaluation dataset API")
@RestController
@RequestMapping("/api/evaluation/datasets")
@RequiredArgsConstructor
public class EvaluationDatasetController {

    private final EvaluationDatasetService evaluationDatasetService;

    @Operation(summary = "Create evaluation dataset")
    @PostMapping
    public ApiResponse<EvaluationDatasetResponse> create(
            @Valid @RequestBody EvaluationDatasetCreateRequest request
    ) {
        return ApiResponse.success(evaluationDatasetService.create(request));
    }

    @Operation(summary = "List evaluation datasets")
    @GetMapping
    public ApiResponse<List<EvaluationDatasetResponse>> findAll() {
        return ApiResponse.success(evaluationDatasetService.findAll());
    }

    @Operation(summary = "Get evaluation dataset by id")
    @GetMapping("/{id}")
    public ApiResponse<EvaluationDatasetResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(evaluationDatasetService.getById(id));
    }
}
