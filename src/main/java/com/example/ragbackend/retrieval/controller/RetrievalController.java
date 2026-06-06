package com.example.ragbackend.retrieval.controller;

import com.example.ragbackend.common.response.ApiResponse;
import com.example.ragbackend.retrieval.dto.RetrieveRequest;
import com.example.ragbackend.retrieval.dto.RetrieveResponse;
import com.example.ragbackend.retrieval.service.RetrievalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Retrieval", description = "Vector retrieval API")
@RestController
@RequestMapping("/api/retrieval")
@RequiredArgsConstructor
public class RetrievalController {

    private final RetrievalService retrievalService;

    @Operation(summary = "Search relevant chunks in a knowledge base")
    @PostMapping("/search")
    public ApiResponse<RetrieveResponse> search(@Valid @RequestBody RetrieveRequest request) {
        return ApiResponse.success(retrievalService.retrieve(request));
    }
}
