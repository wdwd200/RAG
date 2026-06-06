package com.example.ragbackend.chat.dto;

public record ChatReferenceResponse(
        Long chunkId,
        Long documentId,
        Long knowledgeBaseId,
        Integer chunkIndex,
        Double score,
        String content
) {
}
