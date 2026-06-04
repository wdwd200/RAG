package com.example.ragbackend.knowledge.service;

import java.util.List;
import java.util.Optional;

import com.example.ragbackend.knowledge.dto.KnowledgeBaseCreateRequest;
import com.example.ragbackend.knowledge.dto.KnowledgeBaseResponse;

public interface KnowledgeBaseService {

    KnowledgeBaseResponse create(KnowledgeBaseCreateRequest request);

    Optional<KnowledgeBaseResponse> findById(Long id);

    List<KnowledgeBaseResponse> findAll();

    boolean existsById(Long id);
}
