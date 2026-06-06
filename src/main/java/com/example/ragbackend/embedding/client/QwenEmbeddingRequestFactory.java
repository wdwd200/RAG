package com.example.ragbackend.embedding.client;

import com.example.ragbackend.embedding.config.EmbeddingProperties;
import com.example.ragbackend.embedding.config.QwenEmbeddingProperties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class QwenEmbeddingRequestFactory {

    Map<String, Object> createBody(
            List<String> texts,
            EmbeddingProperties embeddingProperties,
            QwenEmbeddingProperties qwenProperties) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", qwenProperties.getModel());
        body.put("input", texts);
        body.put("dimensions", embeddingProperties.getDimension());
        body.put("encoding_format", "float");
        return body;
    }
}
