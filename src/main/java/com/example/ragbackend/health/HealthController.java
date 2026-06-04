package com.example.ragbackend.health;

import java.util.Map;

import com.example.ragbackend.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "Service health check")
@RestController
@RequestMapping("/api")
public class HealthController {

    @Operation(summary = "Health check", description = "Returns the current service status.")
    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of(
                "status", "UP",
                "service", "rag-backend"
        ));
    }
}
