package com.example.ragbackend.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KnowledgeBaseCreateRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        String description,

        Long ownerId,

        String visibility
) {
}
