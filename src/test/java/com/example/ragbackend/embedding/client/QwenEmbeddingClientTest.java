package com.example.ragbackend.embedding.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.embedding.config.EmbeddingProperties;
import com.example.ragbackend.embedding.config.QwenEmbeddingProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class QwenEmbeddingClientTest {

    @Test
    void missingApiKeyReturnsClearError() {
        QwenEmbeddingProperties qwenProperties = qwenProperties("");
        QwenEmbeddingClient client = new QwenEmbeddingClient(
                embeddingProperties(3),
                qwenProperties,
                RestClient.builder()
        );

        assertThatThrownBy(() -> client.embed("hello"))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("QWEN_API_KEY_MISSING"));
    }

    @Test
    void batchRequestContainsModelInputsDimensionsAndReturnsVectorsInIndexOrder() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        QwenEmbeddingClient client = new QwenEmbeddingClient(
                embeddingProperties(3),
                qwenProperties("test-api-key"),
                builder
        );

        server.expect(requestTo("https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "model": "text-embedding-v4",
                          "input": ["first", "second"],
                          "dimensions": 3,
                          "encoding_format": "float"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "data": [
                            {"index": 1, "embedding": [0.4, 0.5, 0.6]},
                            {"index": 0, "embedding": [0.1, 0.2, 0.3]}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<List<Float>> vectors = client.embedBatch(List.of("first", "second"));

        assertThat(vectors).hasSize(2);
        assertThat(vectors.get(0)).containsExactly(0.1f, 0.2f, 0.3f);
        assertThat(vectors.get(1)).containsExactly(0.4f, 0.5f, 0.6f);
        server.verify();
    }

    private EmbeddingProperties embeddingProperties(int dimension) {
        EmbeddingProperties properties = new EmbeddingProperties();
        properties.setProvider("qwen");
        properties.setDimension(dimension);
        return properties;
    }

    private QwenEmbeddingProperties qwenProperties(String apiKey) {
        QwenEmbeddingProperties properties = new QwenEmbeddingProperties();
        properties.setModel("text-embedding-v4");
        properties.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
        properties.setApiKey(apiKey);
        return properties;
    }
}
