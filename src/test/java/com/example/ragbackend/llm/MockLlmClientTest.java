package com.example.ragbackend.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ragbackend.llm.client.MockLlmClient;
import com.example.ragbackend.llm.model.LlmRequest;
import com.example.ragbackend.llm.model.LlmResponse;
import org.junit.jupiter.api.Test;

class MockLlmClientTest {

    @Test
    void returnsStableMockAnswer() {
        MockLlmClient client = new MockLlmClient();
        LlmRequest request = new LlmRequest("What is RAG?", "mock-rag-assistant", null);

        LlmResponse first = client.complete(request);
        LlmResponse second = client.complete(request);

        assertThat(first).isEqualTo(second);
        assertThat(first.success()).isTrue();
        assertThat(first.model()).isEqualTo("mock-rag-assistant");
        assertThat(first.content()).isEqualTo("Mock answer for prompt: What is RAG?");
        assertThat(first.errorMessage()).isNull();
    }
}
