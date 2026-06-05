package com.example.ragbackend.vector.service.impl;

import com.example.ragbackend.vector.config.QdrantProperties;
import com.example.ragbackend.vector.model.ChunkVector;
import com.example.ragbackend.vector.model.VectorSearchRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class QdrantRequestFactory {

    Map<String, Object> createCollectionBody(QdrantProperties properties) {
        return Map.of(
                "vectors",
                Map.of(
                        "size", properties.getVectorSize(),
                        "distance", properties.resolveDistance()
                )
        );
    }

    Map<String, Object> upsertBody(ChunkVector chunkVector) {
        Map<String, Object> point = new LinkedHashMap<>();
        point.put("id", chunkVector.chunkId());
        point.put("vector", chunkVector.vector());
        point.put("payload", payload(chunkVector));

        return Map.of("points", List.of(point));
    }

    Map<String, Object> searchBody(VectorSearchRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vector", request.queryVector());
        body.put("limit", request.limit());
        body.put("with_payload", true);

        if (request.knowledgeBaseId() != null) {
            body.put("filter", matchFilter("knowledgeBaseId", request.knowledgeBaseId()));
        }
        if (request.scoreThreshold() != null) {
            body.put("score_threshold", request.scoreThreshold());
        }

        return body;
    }

    Map<String, Object> deleteByDocumentIdBody(Long documentId) {
        return Map.of("filter", matchFilter("documentId", documentId));
    }

    private Map<String, Object> payload(ChunkVector chunkVector) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("chunkId", chunkVector.chunkId());
        payload.put("documentId", chunkVector.documentId());
        payload.put("knowledgeBaseId", chunkVector.knowledgeBaseId());
        payload.put("chunkIndex", chunkVector.chunkIndex());
        payload.put("contentHash", chunkVector.contentHash());
        payload.put("processingVersion", chunkVector.processingVersion());
        return payload;
    }

    private Map<String, Object> matchFilter(String key, Object value) {
        return Map.of(
                "must",
                List.of(Map.of(
                        "key", key,
                        "match", Map.of("value", value)
                ))
        );
    }
}
