package com.example.ragbackend.vector.model;

import java.util.List;

public record VectorSearchRequest(
        List<Float> queryVector,
        int topK,
        Long knowledgeBaseId,
        Double scoreThreshold
) {
}
