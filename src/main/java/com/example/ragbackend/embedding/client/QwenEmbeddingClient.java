package com.example.ragbackend.embedding.client;

import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.embedding.config.EmbeddingProperties;
import com.example.ragbackend.embedding.config.QwenEmbeddingProperties;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.embedding", name = "provider", havingValue = "qwen")
public class QwenEmbeddingClient implements EmbeddingClient {

    private static final String QWEN_API_KEY_MISSING_CODE = "QWEN_API_KEY_MISSING";
    private static final String QWEN_EMBEDDING_REQUEST_FAILED_CODE = "QWEN_EMBEDDING_REQUEST_FAILED";
    private static final String QWEN_EMBEDDING_RESPONSE_INVALID_CODE = "QWEN_EMBEDDING_RESPONSE_INVALID";
    private static final String QWEN_EMBEDDING_DIMENSION_MISMATCH_CODE =
            "QWEN_EMBEDDING_DIMENSION_MISMATCH";
    private static final String EMBEDDING_TEXT_EMPTY_CODE = "EMBEDDING_TEXT_EMPTY";
    private static final String EMBEDDING_TEXTS_NULL_CODE = "EMBEDDING_TEXTS_NULL";
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final EmbeddingProperties embeddingProperties;
    private final QwenEmbeddingProperties qwenProperties;
    private final RestClient restClient;
    private final QwenEmbeddingRequestFactory requestFactory = new QwenEmbeddingRequestFactory();

    public QwenEmbeddingClient(
            EmbeddingProperties embeddingProperties,
            QwenEmbeddingProperties qwenProperties,
            RestClient.Builder restClientBuilder) {
        this.embeddingProperties = embeddingProperties;
        this.qwenProperties = qwenProperties;
        this.restClient = restClientBuilder.baseUrl(qwenProperties.resolveBaseUrl()).build();
    }

    @Override
    public List<Float> embed(String text) {
        List<List<Float>> vectors = embedBatch(List.of(validateText(text)));
        return vectors.get(0);
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        validateApiKey();
        if (texts == null) {
            throw new BusinessException(EMBEDDING_TEXTS_NULL_CODE, "Embedding texts must not be null");
        }
        if (texts.isEmpty()) {
            return List.of();
        }

        List<String> validatedTexts = texts.stream()
                .map(this::validateText)
                .toList();
        Map<String, Object> response = requestEmbeddings(validatedTexts);
        return parseVectors(response, validatedTexts.size());
    }

    private Map<String, Object> requestEmbeddings(List<String> texts) {
        try {
            return restClient.post()
                    .uri("/embeddings")
                    .headers(headers -> headers.setBearerAuth(qwenProperties.getApiKey().trim()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestFactory.createBody(texts, embeddingProperties, qwenProperties))
                    .retrieve()
                    .body(MAP_RESPONSE_TYPE);
        } catch (RestClientResponseException ex) {
            log.warn("Qwen embedding request failed with status {}", ex.getStatusCode().value());
            throw new BusinessException(
                    QWEN_EMBEDDING_REQUEST_FAILED_CODE,
                    "Qwen embedding request failed with HTTP status " + ex.getStatusCode().value()
            );
        } catch (RestClientException ex) {
            log.warn("Qwen embedding request failed: {}", ex.getMessage());
            throw new BusinessException(
                    QWEN_EMBEDDING_REQUEST_FAILED_CODE,
                    "Qwen embedding service is unavailable"
            );
        }
    }

    private List<List<Float>> parseVectors(Map<String, Object> response, int expectedCount) {
        if (response == null || !(response.get("data") instanceof List<?> dataItems)) {
            throw invalidResponse("Qwen embedding response does not contain data");
        }

        List<IndexedVector> indexedVectors = new ArrayList<>(dataItems.size());
        for (Object dataItem : dataItems) {
            if (!(dataItem instanceof Map<?, ?> item)
                    || !(item.get("index") instanceof Number index)
                    || !(item.get("embedding") instanceof List<?> values)) {
                throw invalidResponse("Qwen embedding response item is invalid");
            }

            List<Float> vector = values.stream()
                    .map(this::toFloat)
                    .toList();
            validateDimension(vector);
            indexedVectors.add(new IndexedVector(index.intValue(), vector));
        }

        indexedVectors.sort(Comparator.comparingInt(IndexedVector::index));
        if (indexedVectors.size() != expectedCount) {
            throw invalidResponse(
                    "Qwen embedding response count mismatch: expected "
                            + expectedCount
                            + ", actual "
                            + indexedVectors.size()
            );
        }

        return indexedVectors.stream()
                .map(IndexedVector::vector)
                .toList();
    }

    private Float toFloat(Object value) {
        if (!(value instanceof Number number)) {
            throw invalidResponse("Qwen embedding vector contains a non-numeric value");
        }
        return number.floatValue();
    }

    private void validateApiKey() {
        String apiKey = qwenProperties.getApiKey();
        if (apiKey == null
                || apiKey.isBlank()
                || "replace-with-your-api-key".equals(apiKey.trim())) {
            throw new BusinessException(
                    QWEN_API_KEY_MISSING_CODE,
                    "DashScope API key is required when embedding provider is qwen"
            );
        }
    }

    private String validateText(String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(EMBEDDING_TEXT_EMPTY_CODE, "Embedding text must not be empty");
        }
        return text;
    }

    private void validateDimension(List<Float> vector) {
        if (vector.size() != embeddingProperties.getDimension()) {
            throw new BusinessException(
                    QWEN_EMBEDDING_DIMENSION_MISMATCH_CODE,
                    "Qwen embedding vector dimension mismatch: expected "
                            + embeddingProperties.getDimension()
                            + ", actual "
                            + vector.size()
            );
        }
    }

    private BusinessException invalidResponse(String message) {
        return new BusinessException(QWEN_EMBEDDING_RESPONSE_INVALID_CODE, message);
    }

    private record IndexedVector(int index, List<Float> vector) {
    }
}
