package com.example.ragbackend.knowledge.service;

import java.util.List;
import java.util.Optional;

import com.example.ragbackend.knowledge.dto.KnowledgeBaseCreateRequest;
import com.example.ragbackend.knowledge.dto.KnowledgeBaseResponse;
import com.example.ragbackend.knowledge.dto.KnowledgeBaseUpdateRequest;

public interface KnowledgeBaseService {

    KnowledgeBaseResponse create(KnowledgeBaseCreateRequest request);

    KnowledgeBaseResponse getById(Long id);

    Optional<KnowledgeBaseResponse> findById(Long id);

    List<KnowledgeBaseResponse> findAll();

    KnowledgeBaseResponse update(Long id, KnowledgeBaseUpdateRequest request);

    void deleteById(Long id);

    boolean existsById(Long id);
}
