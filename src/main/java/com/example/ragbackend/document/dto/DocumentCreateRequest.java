package com.example.ragbackend.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record DocumentCreateRequest(
        @NotNull
        Long knowledgeBaseId,

        @NotBlank
        @Size(max = 255)
        String fileName,

        @NotBlank
        @Size(max = 20)
        String fileType,

        @NotNull
        @PositiveOrZero
        Long fileSize,

        @Size(max = 500)
        String storagePath,

        Long createdBy
) {
}
