package com.example.ragbackend.chat.dto;

import java.util.List;

public record ChatOnceResponse(
        String requestId,
        Long sessionId,
        Long userMessageId,
        Long assistantMessageId,
        String answer,
        List<ChatReferenceResponse> references
) {
}
