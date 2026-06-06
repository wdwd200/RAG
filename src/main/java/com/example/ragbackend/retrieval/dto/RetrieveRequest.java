package com.example.ragbackend.retrieval.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RetrieveRequest(
        @NotNull(message = "knowledgeBaseId must not be null")
        @Positive(message = "knowledgeBaseId must be greater than 0")
        Long knowledgeBaseId,

        @NotBlank(message = "question must not be blank")
        String question,

        @Min(value = 1, message = "topK must be at least 1")
        @Max(value = 20, message = "topK must not exceed 20")
        Integer topK
) {
}
