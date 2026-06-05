package com.example.ragbackend.document.controller;

import java.util.List;
import com.example.ragbackend.common.response.ApiResponse;
import com.example.ragbackend.document.dto.DocumentCreateRequest;
import com.example.ragbackend.document.dto.DocumentResponse;
import com.example.ragbackend.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Document", description = "Document metadata API")
@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "Create document metadata")
    @PostMapping("/api/documents")
    public ApiResponse<DocumentResponse> createMetadata(@Valid @RequestBody DocumentCreateRequest request) {
        return ApiResponse.success(documentService.createMetadata(request));
    }

    @Operation(summary = "Upload document file")
    @PostMapping("/api/documents/upload")
    public ApiResponse<DocumentResponse> upload(
            @RequestParam Long knowledgeBaseId,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) Long createdBy
    ) {
        return ApiResponse.success(documentService.upload(knowledgeBaseId, file, createdBy));
    }

    @Operation(summary = "Get document metadata by id")
    @GetMapping("/api/documents/{id}")
    public ApiResponse<DocumentResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(documentService.findById(id));
    }

    @Operation(summary = "List documents by knowledge base id")
    @GetMapping("/api/knowledge-bases/{knowledgeBaseId}/documents")
    public ApiResponse<List<DocumentResponse>> findByKnowledgeBaseId(@PathVariable Long knowledgeBaseId) {
        return ApiResponse.success(documentService.findByKnowledgeBaseId(knowledgeBaseId));
    }

    @Operation(summary = "Delete document metadata")
    @DeleteMapping("/api/documents/{id}")
    public ApiResponse<Void> deleteById(@PathVariable Long id) {
        documentService.deleteById(id);
        return ApiResponse.success();
    }
}
