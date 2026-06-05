package com.example.ragbackend.vector.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class QdrantPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(Config.class);

    @Test
    void loadsDefaultQdrantProperties() {
        contextRunner.run(context -> {
            QdrantProperties properties = context.getBean(QdrantProperties.class);

            assertThat(properties.getHost()).isEqualTo("localhost");
            assertThat(properties.getHttpPort()).isEqualTo(6333);
            assertThat(properties.getCollectionName()).isEqualTo("rag_chunks");
            assertThat(properties.getVectorSize()).isEqualTo(384);
            assertThat(properties.getDistance()).isEqualTo("COSINE");
            assertThat(properties.resolveBaseUrl()).isEqualTo("http://localhost:6333");
            assertThat(properties.resolveDistance()).isEqualTo("Cosine");
        });
    }

    @Test
    void bindsConfiguredQdrantProperties() {
        contextRunner
                .withPropertyValues(
                        "app.qdrant.host=https://qdrant.example.com",
                        "app.qdrant.http-port=7443",
                        "app.qdrant.collection-name=test_chunks",
                        "app.qdrant.vector-size=1024",
                        "app.qdrant.distance=dot"
                )
                .run(context -> {
                    QdrantProperties properties = context.getBean(QdrantProperties.class);

                    assertThat(properties.getCollectionName()).isEqualTo("test_chunks");
                    assertThat(properties.getVectorSize()).isEqualTo(1024);
                    assertThat(properties.resolveBaseUrl()).isEqualTo("https://qdrant.example.com:7443");
                    assertThat(properties.resolveDistance()).isEqualTo("Dot");
                });
    }

    @Configuration
    @EnableConfigurationProperties(QdrantProperties.class)
    static class Config {
    }
}
