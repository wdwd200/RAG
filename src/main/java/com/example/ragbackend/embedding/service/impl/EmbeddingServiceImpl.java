package com.example.ragbackend.embedding.service.impl;

import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.embedding.client.EmbeddingClient;
import com.example.ragbackend.embedding.config.EmbeddingProperties;
import com.example.ragbackend.embedding.service.EmbeddingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    private static final String EMBEDDING_TEXT_EMPTY_CODE = "EMBEDDING_TEXT_EMPTY";
    private static final String EMBEDDING_TEXTS_NULL_CODE = "EMBEDDING_TEXTS_NULL";
    private static final String EMBEDDING_VECTOR_DIMENSION_MISMATCH_CODE =
            "EMBEDDING_VECTOR_DIMENSION_MISMATCH";

    private final EmbeddingClient embeddingClient;
    private final EmbeddingProperties embeddingProperties;

    @Override
    public List<Float> embed(String text) {
        validateText(text);
        List<Float> vector = embeddingClient.embed(text);
        validateVectorDimension(vector);
        return vector;
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        if (texts == null) {
            throw new BusinessException(EMBEDDING_TEXTS_NULL_CODE, "Embedding texts must not be null");
        }
        texts.forEach(this::validateText);

        List<List<Float>> vectors = embeddingClient.embedBatch(texts);
        vectors.forEach(this::validateVectorDimension);
        return vectors;
    }

    private void validateText(String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(EMBEDDING_TEXT_EMPTY_CODE, "Embedding text must not be empty");
        }
    }

    private void validateVectorDimension(List<Float> vector) {
        int expectedDimension = embeddingProperties.getDimension();
        int actualDimension = vector == null ? -1 : vector.size();
        if (actualDimension != expectedDimension) {
            throw new BusinessException(
                    EMBEDDING_VECTOR_DIMENSION_MISMATCH_CODE,
                    "Embedding vector dimension mismatch: expected "
                            + expectedDimension
                            + ", actual "
                            + actualDimension
            );
        }
    }
}
