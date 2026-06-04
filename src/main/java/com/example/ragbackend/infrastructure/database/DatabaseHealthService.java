package com.example.ragbackend.infrastructure.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

import javax.sql.DataSource;

import com.example.ragbackend.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseHealthService {

    private static final String VALIDATION_QUERY = "SELECT 1";

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public DatabaseHealthResponse checkConnection() {
        try {
            Integer result = jdbcTemplate.queryForObject(VALIDATION_QUERY, Integer.class);
            if (result == null || result != 1) {
                throw new BusinessException("DATABASE_UNAVAILABLE", "Database validation query returned an unexpected result");
            }
            return new DatabaseHealthResponse("UP", resolveDatabaseProductName(), VALIDATION_QUERY, LocalDateTime.now());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Database health check failed: {}", ex.getMessage());
            throw new BusinessException("DATABASE_UNAVAILABLE", "Database connection is unavailable");
        }
    }

    private String resolveDatabaseProductName() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName();
        } catch (SQLException ex) {
            log.warn("Failed to resolve database product name: {}", ex.getMessage());
            return "Unknown";
        }
    }
}
