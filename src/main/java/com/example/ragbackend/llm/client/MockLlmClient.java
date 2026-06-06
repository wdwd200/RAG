package com.example.ragbackend.llm.client;

import com.example.ragbackend.llm.model.LlmRequest;
import com.example.ragbackend.llm.model.LlmResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.llm", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmClient implements LlmClient {

    @Override
    public LlmResponse complete(LlmRequest request) {
        return new LlmResponse(
                "Mock answer for prompt: " + request.prompt(),
                request.model(),
                true,
                null
        );
    }
}
