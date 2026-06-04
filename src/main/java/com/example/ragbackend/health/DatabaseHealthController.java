package com.example.ragbackend.health;

import com.example.ragbackend.common.response.ApiResponse;
import com.example.ragbackend.infrastructure.database.DatabaseHealthResponse;
import com.example.ragbackend.infrastructure.database.DatabaseHealthService;

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
public class DatabaseHealthController {

    private final DatabaseHealthService databaseHealthService;

    @Operation(summary = "Database health check", description = "Checks whether the application can query the configured database.")
    @GetMapping("/database")
    public ApiResponse<DatabaseHealthResponse> databaseHealth() {
        return ApiResponse.success(databaseHealthService.checkConnection());
    }
}
