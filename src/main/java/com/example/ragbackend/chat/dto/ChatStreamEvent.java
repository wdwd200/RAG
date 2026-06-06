package com.example.ragbackend.chat.dto;

public record ChatStreamEvent(
        String requestId,
        String eventType,
        Object data
) {
}
