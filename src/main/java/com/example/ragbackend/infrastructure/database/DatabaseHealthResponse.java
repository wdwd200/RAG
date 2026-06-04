package com.example.ragbackend.infrastructure.database;

import java.time.LocalDateTime;

public record DatabaseHealthResponse(
        String status,
        String database,
        String validationQuery,
        LocalDateTime checkedAt
) {
}
