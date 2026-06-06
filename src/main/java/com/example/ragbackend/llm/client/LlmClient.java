package com.example.ragbackend.llm.client;

import com.example.ragbackend.llm.model.LlmRequest;
import com.example.ragbackend.llm.model.LlmResponse;

public interface LlmClient {

    LlmResponse complete(LlmRequest request);
}
