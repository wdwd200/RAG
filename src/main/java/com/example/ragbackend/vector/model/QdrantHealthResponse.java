package com.example.ragbackend.vector.model;

import java.time.LocalDateTime;

public record QdrantHealthResponse(
        String status,
        String collectionName,
        int vectorSize,
        String baseUrl,
        LocalDateTime checkedAt,
        String message
) {
}
