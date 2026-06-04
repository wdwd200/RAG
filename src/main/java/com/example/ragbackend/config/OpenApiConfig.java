package com.example.ragbackend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ragBackendOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("RAG Backend API")
                        .description("RAG backend knowledge base service API")
                        .version("0.0.1"));
    }
}
