package com.example.ragbackend.vector.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class VectorStoreModelTest {

    @Test
    void chunkVectorCarriesPayloadMetadataAndVector() {
        ChunkVector chunkVector = new ChunkVector(
                1L,
                2L,
                3L,
                4,
                "hash",
                5,
                List.of(0.1f, 0.2f)
        );

        assertThat(chunkVector.chunkId()).isEqualTo(1L);
        assertThat(chunkVector.documentId()).isEqualTo(2L);
        assertThat(chunkVector.knowledgeBaseId()).isEqualTo(3L);
        assertThat(chunkVector.chunkIndex()).isEqualTo(4);
        assertThat(chunkVector.contentHash()).isEqualTo("hash");
        assertThat(chunkVector.processingVersion()).isEqualTo(5);
        assertThat(chunkVector.vector()).containsExactly(0.1f, 0.2f);
    }

    @Test
    void searchRequestAndResultExpressVectorSearch() {
        VectorSearchRequest request = new VectorSearchRequest(List.of(0.1f, 0.2f), 10, 3L, 0.8d);
        VectorSearchResult result = new VectorSearchResult("1", 1L, 2L, 3L, 4, "hash", 5, 0.91d);

        assertThat(request.queryVector()).containsExactly(0.1f, 0.2f);
        assertThat(request.limit()).isEqualTo(10);
        assertThat(request.knowledgeBaseId()).isEqualTo(3L);
        assertThat(request.scoreThreshold()).isEqualTo(0.8d);
        assertThat(result.pointId()).isEqualTo("1");
        assertThat(result.score()).isEqualTo(0.91d);
    }
}
