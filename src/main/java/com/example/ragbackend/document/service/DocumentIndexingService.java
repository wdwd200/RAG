package com.example.ragbackend.document.service;

import com.example.ragbackend.document.dto.DocumentProcessingResponse;

public interface DocumentIndexingService {

    DocumentProcessingResponse index(Long documentId);
}
