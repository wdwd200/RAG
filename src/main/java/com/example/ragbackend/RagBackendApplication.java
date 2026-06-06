package com.example.ragbackend;

import com.example.ragbackend.chunk.config.ChunkProperties;
import com.example.ragbackend.embedding.config.EmbeddingProperties;
import com.example.ragbackend.embedding.config.QwenEmbeddingProperties;
import com.example.ragbackend.infrastructure.storage.StorageProperties;
import com.example.ragbackend.vector.config.QdrantProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        StorageProperties.class,
        ChunkProperties.class,
        EmbeddingProperties.class,
        QwenEmbeddingProperties.class,
        QdrantProperties.class
})
public class RagBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagBackendApplication.class, args);
    }
}
