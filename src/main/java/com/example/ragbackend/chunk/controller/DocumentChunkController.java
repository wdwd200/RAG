package com.example.ragbackend.chunk.controller;

import com.example.ragbackend.chunk.dto.DocumentChunkResponse;
import com.example.ragbackend.chunk.service.DocumentChunkService;
import com.example.ragbackend.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Document Chunk", description = "Document chunk query API")
@RestController
@RequiredArgsConstructor
public class DocumentChunkController {

    private final DocumentChunkService documentChunkService;

    @Operation(summary = "List active chunks by document id")
    @GetMapping("/api/documents/{documentId}/chunks")
    public ApiResponse<List<DocumentChunkResponse>> findByDocumentId(@PathVariable Long documentId) {
        return ApiResponse.success(documentChunkService.findActiveByDocumentId(documentId));
    }

    @Operation(summary = "Get active chunk by id")
    @GetMapping("/api/chunks/{id}")
    public ApiResponse<DocumentChunkResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(documentChunkService.findById(id));
    }
}
