package com.example.ragbackend.evaluation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record EvaluationRunRequest(
        @Min(value = 1, message = "topK must be at least 1")
        @Max(value = 20, message = "topK must not exceed 20")
        Integer topK
) {
}
