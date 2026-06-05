package com.example.ragbackend.embedding.service;

import java.util.List;

public interface EmbeddingService {

    List<Float> embed(String text);

    List<List<Float>> embedBatch(List<String> texts);
}
