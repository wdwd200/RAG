package com.example.ragbackend.llm.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.llm.config.LlmProperties;
import com.example.ragbackend.llm.model.LlmRequest;
import com.example.ragbackend.llm.model.LlmResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class QwenLlmClientTest {

    @Test
    void missingApiKeyReturnsClearError() {
        QwenLlmClient client = new QwenLlmClient(
                llmProperties(""),
                RestClient.builder()
        );

        assertThatThrownBy(() -> client.complete(
                new LlmRequest("RAG prompt", "qwen-plus", 0.2d)
        )).isInstanceOfSatisfying(BusinessException.class, ex ->
                assertThat(ex.getCode()).isEqualTo("QWEN_LLM_API_KEY_MISSING"));
    }

    @Test
    void requestContainsConfiguredFieldsAndParsesAssistantContent() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        QwenLlmClient client = new QwenLlmClient(
                llmProperties("test-api-key"),
                builder
        );

        server.expect(requestTo(
                        "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
                ))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "model": "qwen-plus",
                          "messages": [
                            {
                              "role": "system",
                              "content": "你是一个严谨的知识库问答助手。"
                            },
                            {
                              "role": "user",
                              "content": "RAG prompt"
                            }
                          ],
                          "temperature": 0.2,
                          "max_tokens": 1000
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "model": "qwen-plus",
                          "choices": [
                            {
                              "message": {
                                "role": "assistant",
                                "content": "这是基于知识库上下文的回答。"
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        LlmResponse response = client.complete(
                new LlmRequest("RAG prompt", "qwen-plus", 0.2d)
        );

        assertThat(response.success()).isTrue();
        assertThat(response.model()).isEqualTo("qwen-plus");
        assertThat(response.content()).isEqualTo("这是基于知识库上下文的回答。");
        assertThat(response.errorMessage()).isNull();
        server.verify();
    }

    @Test
    void missingAssistantContentReturnsClearError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        QwenLlmClient client = new QwenLlmClient(
                llmProperties("test-api-key"),
                builder
        );

        server.expect(requestTo(
                        "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
                ))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            {
                              "message": {
                                "role": "assistant"
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.complete(
                new LlmRequest("RAG prompt", "qwen-plus", 0.2d)
        )).isInstanceOfSatisfying(BusinessException.class, ex ->
                assertThat(ex.getCode()).isEqualTo("QWEN_LLM_RESPONSE_INVALID"));
        server.verify();
    }

    @Test
    void httpFailureReturnsClearError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        QwenLlmClient client = new QwenLlmClient(
                llmProperties("test-api-key"),
                builder
        );

        server.expect(requestTo(
                        "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
                ))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> client.complete(
                new LlmRequest("RAG prompt", "qwen-plus", 0.2d)
        )).isInstanceOfSatisfying(BusinessException.class, ex ->
                assertThat(ex.getCode()).isEqualTo("QWEN_LLM_REQUEST_FAILED"));
        server.verify();
    }

    private LlmProperties llmProperties(String apiKey) {
        LlmProperties properties = new LlmProperties();
        properties.setProvider("qwen");
        properties.setModel("qwen-plus");
        properties.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
        properties.setApiKey(apiKey);
        properties.setTemperature(0.2d);
        properties.setMaxTokens(1000);
        return properties;
    }
}
