package com.example.ragbackend.llm.service;

import com.example.ragbackend.llm.model.LlmRequest;
import com.example.ragbackend.llm.model.LlmResponse;

public interface LlmService {

    LlmResponse complete(LlmRequest request);
}
