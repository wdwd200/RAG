package com.example.ragbackend.vector.service;

import com.example.ragbackend.vector.config.QdrantProperties;
import com.example.ragbackend.vector.model.QdrantHealthResponse;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QdrantHealthService {

    private final VectorStoreService vectorStoreService;
    private final QdrantProperties qdrantProperties;

    public QdrantHealthResponse check() {
        try {
            vectorStoreService.ensureCollection();
            return response("UP", "Qdrant collection is available");
        } catch (RuntimeException ex) {
            log.warn("Qdrant health check failed: {}", ex.getMessage());
            return response("DOWN", "Qdrant collection is unavailable");
        }
    }

    private QdrantHealthResponse response(String status, String message) {
        return new QdrantHealthResponse(
                status,
                qdrantProperties.getCollectionName(),
                qdrantProperties.getVectorSize(),
                qdrantProperties.resolveBaseUrl(),
                LocalDateTime.now(),
                message
        );
    }
}
