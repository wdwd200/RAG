package com.example.ragbackend.vector.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.vector.config.QdrantProperties;
import com.example.ragbackend.vector.model.ChunkVector;
import com.example.ragbackend.vector.model.QdrantHealthResponse;
import com.example.ragbackend.vector.model.VectorSearchRequest;
import com.example.ragbackend.vector.model.VectorSearchResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class QdrantHealthServiceTest {

    @Test
    void checkReturnsUpWhenCollectionCanBeEnsured() {
        QdrantHealthService service = new QdrantHealthService(new NoopVectorStoreService(), properties());

        QdrantHealthResponse response = service.check();

        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.collectionName()).isEqualTo("rag_chunks_test");
        assertThat(response.vectorSize()).isEqualTo(8);
        assertThat(response.baseUrl()).isEqualTo("http://localhost:6333");
        assertThat(response.checkedAt()).isNotNull();
    }

    @Test
    void checkReturnsDownWhenCollectionCannotBeEnsured() {
        QdrantHealthService service = new QdrantHealthService(new FailingVectorStoreService(), properties());

        QdrantHealthResponse response = service.check();

        assertThat(response.status()).isEqualTo("DOWN");
        assertThat(response.message()).isEqualTo("Qdrant collection is unavailable");
    }

    private QdrantProperties properties() {
        QdrantProperties properties = new QdrantProperties();
        properties.setCollectionName("rag_chunks_test");
        properties.setVectorSize(8);
        return properties;
    }

    private static class NoopVectorStoreService implements VectorStoreService {

        @Override
        public void ensureCollection() {
        }

        @Override
        public void upsert(ChunkVector chunkVector) {
        }

        @Override
        public List<VectorSearchResult> search(VectorSearchRequest request) {
            return List.of();
        }

        @Override
        public void deleteByDocumentId(Long documentId) {
        }
    }

    private static class FailingVectorStoreService extends NoopVectorStoreService {

        @Override
        public void ensureCollection() {
            throw new BusinessException("QDRANT_UNAVAILABLE", "Qdrant is unavailable");
        }
    }
}
