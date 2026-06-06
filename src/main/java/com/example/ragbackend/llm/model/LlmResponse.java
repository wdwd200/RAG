package com.example.ragbackend.llm.model;

public record LlmResponse(
        String content,
        String model,
        boolean success,
        String errorMessage
) {
}
