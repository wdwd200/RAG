package com.example.ragbackend.audit.controller;

import com.example.ragbackend.audit.dto.LlmCallLogResponse;
import com.example.ragbackend.audit.service.LlmCallLogService;
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
public class LlmCallLogController {

    private final LlmCallLogService llmCallLogService;

    @Operation(summary = "List LLM call logs for one chat request")
    @GetMapping("/llm-call-logs/{requestId}")
    public ApiResponse<List<LlmCallLogResponse>> findByRequestId(
            @PathVariable String requestId) {
        return ApiResponse.success(llmCallLogService.findByRequestId(requestId));
    }
}
