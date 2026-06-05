package com.example.ragbackend.vector.service;

import com.example.ragbackend.vector.model.ChunkVector;
import com.example.ragbackend.vector.model.VectorSearchRequest;
import com.example.ragbackend.vector.model.VectorSearchResult;
import java.util.List;

public interface VectorStoreService {

    void ensureCollection();

    void upsert(ChunkVector chunkVector);

    List<VectorSearchResult> search(VectorSearchRequest request);

    void deleteByDocumentId(Long documentId);
}
