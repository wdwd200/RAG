package com.example.ragbackend.vector.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.vector.config.QdrantProperties;
import com.example.ragbackend.vector.model.ChunkVector;
import com.example.ragbackend.vector.model.VectorSearchRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class QdrantVectorStoreServiceTest {

    @Test
    void upsertValidatesVectorDimensionBeforeCallingQdrant() {
        QdrantVectorStoreService service = new QdrantVectorStoreService(properties(), RestClient.builder());
        ChunkVector chunkVector = new ChunkVector(1L, 2L, 3L, 0, "hash", 1, List.of(0.1f));

        assertThatThrownBy(() -> service.upsert(chunkVector))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("VECTOR_DIMENSION_MISMATCH"));
    }

    @Test
    void searchValidatesTopKAndKnowledgeBaseBeforeCallingQdrant() {
        QdrantVectorStoreService service = new QdrantVectorStoreService(properties(), RestClient.builder());
        VectorSearchRequest request = new VectorSearchRequest(List.of(0.1f, 0.2f, 0.3f), 0, null, null);

        assertThatThrownBy(() -> service.search(request))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("VECTOR_SEARCH_REQUEST_INVALID"));
    }

    private QdrantProperties properties() {
        QdrantProperties properties = new QdrantProperties();
        properties.setVectorSize(3);
        properties.setCollectionName("rag_chunks_test");
        return properties;
    }
}
