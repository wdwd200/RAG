package com.example.ragbackend.retrieval.dto;

import java.util.List;

public record RetrieveResponse(
        Long knowledgeBaseId,
        String question,
        int topK,
        List<RetrievedChunk> chunks
) {
}
