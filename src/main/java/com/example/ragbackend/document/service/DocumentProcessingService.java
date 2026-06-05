package com.example.ragbackend.document.service;

import com.example.ragbackend.document.dto.DocumentProcessingResponse;

public interface DocumentProcessingService {

    DocumentProcessingResponse process(Long documentId);
}
