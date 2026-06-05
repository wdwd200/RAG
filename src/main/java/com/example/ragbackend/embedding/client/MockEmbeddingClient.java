package com.example.ragbackend.embedding.client;

import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.embedding.config.EmbeddingProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.embedding", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockEmbeddingClient implements EmbeddingClient {

    private static final String EMBEDDING_TEXT_EMPTY_CODE = "EMBEDDING_TEXT_EMPTY";
    private static final String EMBEDDING_TEXTS_NULL_CODE = "EMBEDDING_TEXTS_NULL";
    private static final String EMBEDDING_DIMENSION_INVALID_CODE = "EMBEDDING_DIMENSION_INVALID";

    private final EmbeddingProperties embeddingProperties;

    @Override
    public List<Float> embed(String text) {
        validateText(text);
        int dimension = embeddingProperties.getDimension();
        if (dimension <= 0) {
            throw new BusinessException(
                    EMBEDDING_DIMENSION_INVALID_CODE,
                    "Embedding dimension must be greater than 0"
            );
        }

        List<Float> vector = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; i++) {
            vector.add(valueAt(text, i));
        }
        return vector;
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        if (texts == null) {
            throw new BusinessException(EMBEDDING_TEXTS_NULL_CODE, "Embedding texts must not be null");
        }

        return texts.stream()
                .map(this::embed)
                .toList();
    }

    private void validateText(String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(EMBEDDING_TEXT_EMPTY_CODE, "Embedding text must not be empty");
        }
    }

    private float valueAt(String text, int index) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(text.getBytes(StandardCharsets.UTF_8));
            digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(index).array());
            byte[] hash = digest.digest();
            int value = ByteBuffer.wrap(hash).getInt() & Integer.MAX_VALUE;
            float normalized = value / (float) Integer.MAX_VALUE;
            return normalized * 2.0f - 1.0f;
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }
}
