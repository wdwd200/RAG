package com.example.ragbackend.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record EvaluationQuestionCreateRequest(
        @NotBlank
        String question,

        String groundTruthAnswer,

        @NotEmpty
        List<@NotNull @Positive Long> relevantChunkIds,

        @Size(max = 30)
        String questionType
) {
}
