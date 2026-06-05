package com.example.ragbackend.vector.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ragbackend.vector.config.QdrantProperties;
import com.example.ragbackend.vector.model.ChunkVector;
import com.example.ragbackend.vector.model.VectorSearchRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class QdrantRequestFactoryTest {

    private final QdrantRequestFactory requestFactory = new QdrantRequestFactory();

    @Test
    void createCollectionBodyUsesConfiguredVectorSizeAndDistance() {
        QdrantProperties properties = new QdrantProperties();
        properties.setVectorSize(8);
        properties.setDistance("COSINE");

        Map<String, Object> body = requestFactory.createCollectionBody(properties);
        Map<?, ?> vectors = (Map<?, ?>) body.get("vectors");

        assertThat(vectors.get("size")).isEqualTo(8);
        assertThat(vectors.get("distance")).isEqualTo("Cosine");
    }

    @Test
    void upsertBodyIncludesVectorAndRequiredPayload() {
        ChunkVector chunkVector = new ChunkVector(
                11L,
                22L,
                33L,
                2,
                "content-hash",
                4,
                List.of(0.1f, 0.2f, 0.3f)
        );

        Map<String, Object> body = requestFactory.upsertBody(chunkVector);
        List<?> points = (List<?>) body.get("points");
        Map<?, ?> point = (Map<?, ?>) points.get(0);
        Map<?, ?> payload = (Map<?, ?>) point.get("payload");

        assertThat(point.get("id")).isEqualTo(11L);
        assertThat(point.get("vector")).isEqualTo(List.of(0.1f, 0.2f, 0.3f));
        assertThat(payload.get("chunkId")).isEqualTo(11L);
        assertThat(payload.get("documentId")).isEqualTo(22L);
        assertThat(payload.get("knowledgeBaseId")).isEqualTo(33L);
        assertThat(payload.get("chunkIndex")).isEqualTo(2);
        assertThat(payload.get("contentHash")).isEqualTo("content-hash");
        assertThat(payload.get("processingVersion")).isEqualTo(4);
    }

    @Test
    void searchBodyIncludesFilterWhenKnowledgeBaseIdIsPresent() {
        VectorSearchRequest request = new VectorSearchRequest(List.of(0.1f, 0.2f), 5, 33L, 0.75d);

        Map<String, Object> body = requestFactory.searchBody(request);
        Map<?, ?> filter = (Map<?, ?>) body.get("filter");
        List<?> must = (List<?>) filter.get("must");
        Map<?, ?> condition = (Map<?, ?>) must.get(0);
        Map<?, ?> match = (Map<?, ?>) condition.get("match");

        assertThat(body.get("vector")).isEqualTo(List.of(0.1f, 0.2f));
        assertThat(body.get("limit")).isEqualTo(5);
        assertThat(body.get("with_payload")).isEqualTo(true);
        assertThat(body.get("score_threshold")).isEqualTo(0.75d);
        assertThat(condition.get("key")).isEqualTo("knowledgeBaseId");
        assertThat(match.get("value")).isEqualTo(33L);
    }

    @Test
    void deleteByDocumentIdBodyUsesDocumentIdPayloadFilter() {
        Map<String, Object> body = requestFactory.deleteByDocumentIdBody(22L);
        Map<?, ?> filter = (Map<?, ?>) body.get("filter");
        List<?> must = (List<?>) filter.get("must");
        Map<?, ?> condition = (Map<?, ?>) must.get(0);
        Map<?, ?> match = (Map<?, ?>) condition.get("match");

        assertThat(condition.get("key")).isEqualTo("documentId");
        assertThat(match.get("value")).isEqualTo(22L);
    }
}
