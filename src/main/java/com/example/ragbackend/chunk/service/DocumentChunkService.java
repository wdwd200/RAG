package com.example.ragbackend.chunk.service;

import java.util.List;

import com.example.ragbackend.chunk.dto.DocumentChunkResponse;
import com.example.ragbackend.chunk.entity.DocumentChunk;

public interface DocumentChunkService {

    DocumentChunkResponse create(DocumentChunk documentChunk);

    DocumentChunkResponse findById(Long id);

    List<DocumentChunkResponse> findByDocumentId(Long documentId);

    List<DocumentChunkResponse> findActiveByDocumentId(Long documentId);

    void deactivateByDocumentId(Long documentId);

    void deleteByDocumentId(Long documentId);
}
