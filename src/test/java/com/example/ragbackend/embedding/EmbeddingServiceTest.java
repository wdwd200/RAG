package com.example.ragbackend.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.embedding.client.EmbeddingClient;
import com.example.ragbackend.embedding.config.EmbeddingProperties;
import com.example.ragbackend.embedding.service.EmbeddingService;
import com.example.ragbackend.embedding.service.impl.EmbeddingServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EmbeddingServiceTest {

    @Autowired
    private EmbeddingClient embeddingClient;

    @Autowired
    private EmbeddingService embeddingService;

    @Test
    void mockEmbeddingReturnsConfiguredDimension() {
        List<Float> vector = embeddingClient.embed("hello embedding");

        assertThat(vector).hasSize(8);
    }

    @Test
    void mockEmbeddingIsStableForSameText() {
        List<Float> first = embeddingClient.embed("same text");
        List<Float> second = embeddingClient.embed("same text");

        assertThat(first).isEqualTo(second);
    }

    @Test
    void mockEmbeddingDiffersForDifferentText() {
        List<Float> first = embeddingClient.embed("first text");
        List<Float> second = embeddingClient.embed("second text");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void emptyTextEmbeddingFails() {
        assertThatThrownBy(() -> embeddingService.embed("  "))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("EMBEDDING_TEXT_EMPTY"));
    }

    @Test
    void batchEmbeddingReturnsVectorForEachText() {
        List<List<Float>> vectors = embeddingService.embedBatch(List.of("first", "second", "third"));

        assertThat(vectors).hasSize(3);
        assertThat(vectors).allSatisfy(vector -> assertThat(vector).hasSize(8));
    }

    @Test
    void embeddingServiceValidatesVectorDimension() {
        EmbeddingProperties properties = new EmbeddingProperties();
        properties.setDimension(8);
        EmbeddingClient wrongDimensionClient = new EmbeddingClient() {
            @Override
            public List<Float> embed(String text) {
                return List.of(1.0f, 2.0f);
            }

            @Override
            public List<List<Float>> embedBatch(List<String> texts) {
                return texts.stream()
                        .map(this::embed)
                        .toList();
            }
        };
        EmbeddingService service = new EmbeddingServiceImpl(wrongDimensionClient, properties);

        assertThatThrownBy(() -> service.embed("dimension check"))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("EMBEDDING_VECTOR_DIMENSION_MISMATCH"));
    }
}
