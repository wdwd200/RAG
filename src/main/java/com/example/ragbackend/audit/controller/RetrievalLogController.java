package com.example.ragbackend.audit.controller;

import com.example.ragbackend.audit.dto.RetrievalLogResponse;
import com.example.ragbackend.audit.service.RetrievalLogService;
import com.example.ragbackend.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Audit", description = "Development audit and request tracing APIs")
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class RetrievalLogController {

    private final RetrievalLogService retrievalLogService;

    @Operation(summary = "List retrieval logs for one chat request")
    @GetMapping("/retrieval-logs/{requestId}")
    public ApiResponse<List<RetrievalLogResponse>> findByRequestId(
            @PathVariable String requestId) {
        return ApiResponse.success(retrievalLogService.findByRequestId(requestId));
    }
}
