package com.example.ragbackend.llm.service.impl;

import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.llm.client.LlmClient;
import com.example.ragbackend.llm.config.LlmProperties;
import com.example.ragbackend.llm.model.LlmRequest;
import com.example.ragbackend.llm.model.LlmResponse;
import com.example.ragbackend.llm.service.LlmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LlmServiceImpl implements LlmService {

    private static final String LLM_REQUEST_INVALID_CODE = "LLM_REQUEST_INVALID";
    private static final String LLM_RESPONSE_INVALID_CODE = "LLM_RESPONSE_INVALID";

    private final LlmClient llmClient;
    private final LlmProperties llmProperties;

    @Override
    public LlmResponse complete(LlmRequest request) {
        if (request == null || request.prompt() == null || request.prompt().isBlank()) {
            throw new BusinessException(LLM_REQUEST_INVALID_CODE, "LLM prompt must not be empty");
        }

        String model = request.model() == null || request.model().isBlank()
                ? llmProperties.getModel()
                : request.model();
        Double temperature = request.temperature() == null
                ? llmProperties.getTemperature()
                : request.temperature();
        LlmResponse response = llmClient.complete(
                new LlmRequest(request.prompt(), model, temperature)
        );
        if (response == null) {
            throw new BusinessException(LLM_RESPONSE_INVALID_CODE, "LLM response must not be null");
        }
        return response;
    }
}
