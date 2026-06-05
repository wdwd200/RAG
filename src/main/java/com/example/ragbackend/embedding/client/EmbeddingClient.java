package com.example.ragbackend.embedding.client;

import java.util.List;

public interface EmbeddingClient {

    List<Float> embed(String text);

    List<List<Float>> embedBatch(List<String> texts);
}
