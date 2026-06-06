package com.example.ragbackend.vector.service.impl;

import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.vector.config.QdrantProperties;
import com.example.ragbackend.vector.model.ChunkVector;
import com.example.ragbackend.vector.model.VectorSearchRequest;
import com.example.ragbackend.vector.model.VectorSearchResult;
import com.example.ragbackend.vector.service.VectorStoreService;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
public class QdrantVectorStoreService implements VectorStoreService {

    private static final String VECTOR_CHUNK_INVALID_CODE = "VECTOR_CHUNK_INVALID";
    private static final String VECTOR_DOCUMENT_ID_REQUIRED_CODE = "VECTOR_DOCUMENT_ID_REQUIRED";
    private static final String VECTOR_SEARCH_REQUEST_INVALID_CODE = "VECTOR_SEARCH_REQUEST_INVALID";
    private static final String VECTOR_DIMENSION_MISMATCH_CODE = "VECTOR_DIMENSION_MISMATCH";
    private static final String QDRANT_UNAVAILABLE_CODE = "QDRANT_UNAVAILABLE";
    private static final String QDRANT_OPERATION_FAILED_CODE = "QDRANT_OPERATION_FAILED";
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final QdrantProperties qdrantProperties;
    private final RestClient restClient;
    private final QdrantRequestFactory requestFactory = new QdrantRequestFactory();

    public QdrantVectorStoreService(QdrantProperties qdrantProperties, RestClient.Builder restClientBuilder) {
        this.qdrantProperties = qdrantProperties;
        this.restClient = restClientBuilder.baseUrl(qdrantProperties.resolveBaseUrl()).build();
    }

    @Override
    public void ensureCollection() {
        try {
            restClient.get()
                    .uri("/collections/{collectionName}", qdrantProperties.getCollectionName())
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound ex) {
            createCollection();
        } catch (RestClientException ex) {
            log.warn("Qdrant collection check failed: {}", ex.getMessage());
            throw new BusinessException(QDRANT_UNAVAILABLE_CODE, "Qdrant is unavailable");
        }
    }

    @Override
    public void upsert(ChunkVector chunkVector) {
        validateChunkVector(chunkVector);
        try {
            restClient.put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/collections/{collectionName}/points")
                            .queryParam("wait", true)
                            .build(qdrantProperties.getCollectionName()))
                    .body(requestFactory.upsertBody(chunkVector))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("Qdrant vector upsert failed: {}", ex.getMessage());
            throw new BusinessException(QDRANT_OPERATION_FAILED_CODE, "Failed to upsert vector to Qdrant");
        }
    }

    @Override
    public List<VectorSearchResult> search(VectorSearchRequest request) {
        validateSearchRequest(request);
        try {
            Map<String, Object> response = restClient.post()
                    .uri("/collections/{collectionName}/points/search", qdrantProperties.getCollectionName())
                    .body(requestFactory.searchBody(request))
                    .retrieve()
                    .body(MAP_RESPONSE_TYPE);
            return toSearchResults(response);
        } catch (RestClientException ex) {
            log.warn("Qdrant vector search failed: {}", ex.getMessage());
            throw new BusinessException(QDRANT_OPERATION_FAILED_CODE, "Failed to search vectors from Qdrant");
        }
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        if (documentId == null) {
            throw new BusinessException(VECTOR_DOCUMENT_ID_REQUIRED_CODE, "Document id must not be null");
        }
        try {
            restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/collections/{collectionName}/points/delete")
                            .queryParam("wait", true)
                            .build(qdrantProperties.getCollectionName()))
                    .body(requestFactory.deleteByDocumentIdBody(documentId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("Qdrant vector delete failed: {}", ex.getMessage());
            throw new BusinessException(QDRANT_OPERATION_FAILED_CODE, "Failed to delete vectors from Qdrant");
        }
    }

    private void createCollection() {
        try {
            restClient.put()
                    .uri("/collections/{collectionName}", qdrantProperties.getCollectionName())
                    .body(requestFactory.createCollectionBody(qdrantProperties))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("Qdrant collection creation failed: {}", ex.getMessage());
            throw new BusinessException(QDRANT_OPERATION_FAILED_CODE, "Failed to create Qdrant collection");
        }
    }

    private void validateChunkVector(ChunkVector chunkVector) {
        if (chunkVector == null
                || chunkVector.chunkId() == null
                || chunkVector.documentId() == null
                || chunkVector.knowledgeBaseId() == null
                || chunkVector.chunkIndex() == null
                || chunkVector.processingVersion() == null) {
            throw new BusinessException(VECTOR_CHUNK_INVALID_CODE, "Chunk vector metadata is incomplete");
        }
        validateVectorDimension(chunkVector.vector());
    }

    private void validateSearchRequest(VectorSearchRequest request) {
        if (request == null || request.topK() <= 0 || request.knowledgeBaseId() == null) {
            throw new BusinessException(VECTOR_SEARCH_REQUEST_INVALID_CODE, "Vector search request is invalid");
        }
        validateVectorDimension(request.queryVector());
    }

    private void validateVectorDimension(List<Float> vector) {
        int actualDimension = vector == null ? -1 : vector.size();
        if (actualDimension != qdrantProperties.getVectorSize()) {
            throw new BusinessException(
                    VECTOR_DIMENSION_MISMATCH_CODE,
                    "Vector dimension mismatch: expected "
                            + qdrantProperties.getVectorSize()
                            + ", actual "
                            + actualDimension
            );
        }
    }

    private List<VectorSearchResult> toSearchResults(Map<String, Object> response) {
        if (response == null || !(response.get("result") instanceof List<?> resultItems)) {
            return List.of();
        }

        return resultItems.stream()
                .filter(Map.class::isInstance)
                .map(item -> toSearchResult((Map<?, ?>) item))
                .toList();
    }

    private VectorSearchResult toSearchResult(Map<?, ?> item) {
        Map<?, ?> payload = item.get("payload") instanceof Map<?, ?> payloadMap
                ? payloadMap
                : Map.of();
        return new VectorSearchResult(
                toStringValue(item.get("id")),
                toLong(payload.get("chunkId")),
                toLong(payload.get("documentId")),
                toLong(payload.get("knowledgeBaseId")),
                toInteger(payload.get("chunkIndex")),
                toStringValue(payload.get("contentHash")),
                toInteger(payload.get("processingVersion")),
                toDouble(item.get("score"))
        );
    }

    private Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private Integer toInteger(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private Double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
