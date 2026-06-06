package com.example.ragbackend.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.llm.client.LlmClient;
import com.example.ragbackend.llm.config.LlmProperties;
import com.example.ragbackend.llm.model.LlmRequest;
import com.example.ragbackend.llm.model.LlmResponse;
import com.example.ragbackend.llm.service.LlmService;
import com.example.ragbackend.llm.service.impl.LlmServiceImpl;
import org.junit.jupiter.api.Test;

class LlmServiceTest {

    @Test
    void callsLlmClientWithConfiguredDefaultModel() {
        LlmClient client = mock(LlmClient.class);
        LlmProperties properties = new LlmProperties();
        properties.setModel("configured-model");
        properties.setTemperature(0.35d);
        LlmService service = new LlmServiceImpl(client, properties);
        LlmRequest expectedRequest = new LlmRequest("answer this", "configured-model", 0.35d);
        LlmResponse expectedResponse = new LlmResponse("answer", "configured-model", true, null);
        when(client.complete(expectedRequest)).thenReturn(expectedResponse);

        LlmResponse response = service.complete(new LlmRequest("answer this", null, null));

        assertThat(response).isEqualTo(expectedResponse);
        verify(client).complete(expectedRequest);
    }

    @Test
    void rejectsBlankPrompt() {
        LlmClient client = mock(LlmClient.class);
        LlmService service = new LlmServiceImpl(client, new LlmProperties());

        assertThatThrownBy(() -> service.complete(new LlmRequest("  ", null, null)))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("LLM_REQUEST_INVALID"));
    }
}
