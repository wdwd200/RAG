package com.example.ragbackend.knowledge.controller;

import java.util.List;

import com.example.ragbackend.common.response.ApiResponse;
import com.example.ragbackend.knowledge.dto.KnowledgeBaseCreateRequest;
import com.example.ragbackend.knowledge.dto.KnowledgeBaseResponse;
import com.example.ragbackend.knowledge.dto.KnowledgeBaseUpdateRequest;
import com.example.ragbackend.knowledge.service.KnowledgeBaseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Knowledge Base", description = "Knowledge base CRUD API")
@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @Operation(summary = "Create knowledge base")
    @PostMapping
    public ApiResponse<KnowledgeBaseResponse> create(@Valid @RequestBody KnowledgeBaseCreateRequest request) {
        return ApiResponse.success(knowledgeBaseService.create(request));
    }

    @Operation(summary = "List knowledge bases")
    @GetMapping
    public ApiResponse<List<KnowledgeBaseResponse>> findAll() {
        return ApiResponse.success(knowledgeBaseService.findAll());
    }

    @Operation(summary = "Get knowledge base by id")
    @GetMapping("/{id}")
    public ApiResponse<KnowledgeBaseResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(knowledgeBaseService.getById(id));
    }

    @Operation(summary = "Update knowledge base")
    @PutMapping("/{id}")
    public ApiResponse<KnowledgeBaseResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody KnowledgeBaseUpdateRequest request
    ) {
        return ApiResponse.success(knowledgeBaseService.update(id, request));
    }

    @Operation(summary = "Delete knowledge base")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteById(@PathVariable Long id) {
        knowledgeBaseService.deleteById(id);
        return ApiResponse.success();
    }
}
