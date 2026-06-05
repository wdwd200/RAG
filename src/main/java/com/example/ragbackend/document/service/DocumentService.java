package com.example.ragbackend.document.service;

import java.util.List;

import com.example.ragbackend.document.dto.DocumentCreateRequest;
import com.example.ragbackend.document.dto.DocumentResponse;
import com.example.ragbackend.document.enums.DocumentStatus;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {

    DocumentResponse createMetadata(DocumentCreateRequest request);

    DocumentResponse upload(Long knowledgeBaseId, MultipartFile file, Long createdBy);

    DocumentResponse findById(Long id);

    List<DocumentResponse> findByKnowledgeBaseId(Long knowledgeBaseId);

    DocumentResponse updateStatus(Long id, DocumentStatus status);

    void deleteById(Long id);
}
