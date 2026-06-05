package com.example.ragbackend.vector.model;

import java.util.List;

public record VectorSearchRequest(
        List<Float> queryVector,
        int limit,
        Long knowledgeBaseId,
        Double scoreThreshold
) {
}
