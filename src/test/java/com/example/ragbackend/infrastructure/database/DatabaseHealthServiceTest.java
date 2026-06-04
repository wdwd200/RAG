package com.example.ragbackend.infrastructure.database;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseHealthServiceTest {

    @Autowired
    private DatabaseHealthService databaseHealthService;

    @Test
    void checkConnectionReturnsUpWhenDatabaseIsReachable() {
        DatabaseHealthResponse response = databaseHealthService.checkConnection();

        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.validationQuery()).isEqualTo("SELECT 1");
        assertThat(response.database()).isNotBlank();
        assertThat(response.checkedAt()).isNotNull();
    }
}
