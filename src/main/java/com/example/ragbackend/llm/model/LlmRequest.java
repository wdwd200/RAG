package com.example.ragbackend.llm.model;

public record LlmRequest(
        String prompt,
        String model,
        Double temperature
) {
}
