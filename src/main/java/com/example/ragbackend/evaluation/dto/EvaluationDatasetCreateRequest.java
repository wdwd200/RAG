package com.example.ragbackend.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record EvaluationDatasetCreateRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        @Positive
        Long knowledgeBaseId,

        String description
) {
}
