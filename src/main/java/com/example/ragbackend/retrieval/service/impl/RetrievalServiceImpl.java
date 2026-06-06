package com.example.ragbackend.retrieval.service.impl;

import com.example.ragbackend.chunk.dto.DocumentChunkResponse;
import com.example.ragbackend.chunk.service.DocumentChunkService;
import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.embedding.service.EmbeddingService;
import com.example.ragbackend.retrieval.dto.RetrieveRequest;
import com.example.ragbackend.retrieval.dto.RetrieveResponse;
import com.example.ragbackend.retrieval.dto.RetrievedChunk;
import com.example.ragbackend.retrieval.service.RetrievalService;
import com.example.ragbackend.vector.model.VectorSearchRequest;
import com.example.ragbackend.vector.model.VectorSearchResult;
import com.example.ragbackend.vector.service.VectorStoreService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RetrievalServiceImpl implements RetrievalService {

    static final int DEFAULT_TOP_K = 5;
    static final int MAX_TOP_K = 20;

    private static final String RETRIEVAL_REQUEST_INVALID_CODE = "RETRIEVAL_REQUEST_INVALID";
    private static final String RETRIEVAL_TOP_K_INVALID_CODE = "RETRIEVAL_TOP_K_INVALID";

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final DocumentChunkService documentChunkService;

    @Override
    public RetrieveResponse retrieve(RetrieveRequest request) {
        validateRequest(request);
        int topK = request.topK() == null ? DEFAULT_TOP_K : request.topK();

        List<Float> queryVector = embeddingService.embed(request.question());
        List<VectorSearchResult> searchResults = vectorStoreService.search(
                new VectorSearchRequest(queryVector, topK, request.knowledgeBaseId(), null)
        );

        Map<Long, DocumentChunkResponse> activeChunks = activeChunksById(searchResults);
        List<RetrievedChunk> chunks = searchResults.stream()
                .map(result -> toRetrievedChunk(result, activeChunks, request.knowledgeBaseId()))
                .filter(Objects::nonNull)
                .toList();

        return new RetrieveResponse(
                request.knowledgeBaseId(),
                request.question(),
                topK,
                chunks
        );
    }

    private void validateRequest(RetrieveRequest request) {
        if (request == null
                || request.knowledgeBaseId() == null
                || request.knowledgeBaseId() <= 0
                || request.question() == null
                || request.question().isBlank()) {
            throw new BusinessException(RETRIEVAL_REQUEST_INVALID_CODE, "Retrieval request is invalid");
        }
        if (request.topK() != null && (request.topK() < 1 || request.topK() > MAX_TOP_K)) {
            throw new BusinessException(
                    RETRIEVAL_TOP_K_INVALID_CODE,
                    "Retrieval topK must be between 1 and " + MAX_TOP_K
            );
        }
    }

    private Map<Long, DocumentChunkResponse> activeChunksById(List<VectorSearchResult> searchResults) {
        List<Long> chunkIds = searchResults.stream()
                .map(VectorSearchResult::chunkId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<DocumentChunkResponse> chunks = documentChunkService.findActiveByIds(chunkIds);

        Map<Long, DocumentChunkResponse> chunksById = new LinkedHashMap<>();
        for (DocumentChunkResponse chunk : chunks) {
            chunksById.put(chunk.id(), chunk);
        }
        return chunksById;
    }

    private RetrievedChunk toRetrievedChunk(
            VectorSearchResult result,
            Map<Long, DocumentChunkResponse> activeChunks,
            Long requestedKnowledgeBaseId) {
        DocumentChunkResponse chunk = activeChunks.get(result.chunkId());
        if (chunk == null || !requestedKnowledgeBaseId.equals(chunk.knowledgeBaseId())) {
            return null;
        }
        return new RetrievedChunk(
                chunk.id(),
                chunk.documentId(),
                chunk.knowledgeBaseId(),
                chunk.chunkIndex(),
                result.score(),
                chunk.content()
        );
    }
}
