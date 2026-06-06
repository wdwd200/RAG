package com.example.ragbackend.llm.client;

import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.llm.config.LlmProperties;
import com.example.ragbackend.llm.model.LlmRequest;
import com.example.ragbackend.llm.model.LlmResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.llm", name = "provider", havingValue = "qwen")
public class QwenLlmClient implements LlmClient {

    private static final String QWEN_LLM_API_KEY_MISSING_CODE = "QWEN_LLM_API_KEY_MISSING";
    private static final String QWEN_LLM_REQUEST_FAILED_CODE = "QWEN_LLM_REQUEST_FAILED";
    private static final String QWEN_LLM_RESPONSE_INVALID_CODE = "QWEN_LLM_RESPONSE_INVALID";
    private static final String SYSTEM_PROMPT = "你是一个严谨的知识库问答助手。";
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final LlmProperties llmProperties;
    private final RestClient restClient;

    public QwenLlmClient(LlmProperties llmProperties, RestClient.Builder restClientBuilder) {
        this.llmProperties = llmProperties;
        this.restClient = restClientBuilder.baseUrl(llmProperties.resolveBaseUrl()).build();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        validateApiKey();
        validateRequest(request);

        String model = request.model() == null || request.model().isBlank()
                ? llmProperties.getModel()
                : request.model();
        double temperature = request.temperature() == null
                ? llmProperties.getTemperature()
                : request.temperature();
        Map<String, Object> response = requestCompletion(
                createRequestBody(request.prompt(), model, temperature)
        );
        return parseResponse(response, model);
    }

    private Map<String, Object> createRequestBody(
            String prompt,
            String model,
            double temperature) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", prompt)
        ));
        body.put("temperature", temperature);
        body.put("max_tokens", llmProperties.getMaxTokens());
        return body;
    }

    private Map<String, Object> requestCompletion(Map<String, Object> body) {
        try {
            return restClient.post()
                    .uri("/chat/completions")
                    .headers(headers -> headers.setBearerAuth(llmProperties.getApiKey().trim()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(MAP_RESPONSE_TYPE);
        } catch (RestClientResponseException ex) {
            log.warn("Qwen LLM request failed with status {}", ex.getStatusCode().value());
            throw new BusinessException(
                    QWEN_LLM_REQUEST_FAILED_CODE,
                    "Qwen LLM request failed with HTTP status " + ex.getStatusCode().value()
            );
        } catch (RestClientException ex) {
            log.warn("Qwen LLM request failed: {}", ex.getMessage());
            throw new BusinessException(
                    QWEN_LLM_REQUEST_FAILED_CODE,
                    "Qwen LLM service is unavailable"
            );
        }
    }

    private LlmResponse parseResponse(Map<String, Object> response, String requestedModel) {
        if (response == null || !(response.get("choices") instanceof List<?> choices)
                || choices.isEmpty()) {
            throw invalidResponse("Qwen LLM response does not contain choices");
        }
        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> choice)
                || !(choice.get("message") instanceof Map<?, ?> message)
                || !(message.get("content") instanceof String content)
                || content.isBlank()) {
            throw invalidResponse("Qwen LLM response does not contain assistant content");
        }

        String responseModel = response.get("model") instanceof String model && !model.isBlank()
                ? model
                : requestedModel;
        return new LlmResponse(content, responseModel, true, null);
    }

    private void validateApiKey() {
        String apiKey = llmProperties.getApiKey();
        if (apiKey == null
                || apiKey.isBlank()
                || "replace-with-your-api-key".equals(apiKey.trim())) {
            throw new BusinessException(
                    QWEN_LLM_API_KEY_MISSING_CODE,
                    "DashScope API key is required when LLM provider is qwen"
            );
        }
    }

    private void validateRequest(LlmRequest request) {
        if (request == null || request.prompt() == null || request.prompt().isBlank()) {
            throw new BusinessException(
                    QWEN_LLM_REQUEST_FAILED_CODE,
                    "Qwen LLM prompt must not be empty"
            );
        }
    }

    private BusinessException invalidResponse(String message) {
        return new BusinessException(QWEN_LLM_RESPONSE_INVALID_CODE, message);
    }
}
