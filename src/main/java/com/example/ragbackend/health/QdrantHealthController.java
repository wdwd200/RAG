package com.example.ragbackend.health;

import com.example.ragbackend.common.response.ApiResponse;
import com.example.ragbackend.vector.model.QdrantHealthResponse;
import com.example.ragbackend.vector.service.QdrantHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "Service health check")
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class QdrantHealthController {

    private final QdrantHealthService qdrantHealthService;

    @Operation(summary = "Qdrant health check", description = "Checks Qdrant collection availability and initializes it when missing.")
    @GetMapping("/qdrant")
    public ApiResponse<QdrantHealthResponse> qdrantHealth() {
        return ApiResponse.success(qdrantHealthService.check());
    }
}
