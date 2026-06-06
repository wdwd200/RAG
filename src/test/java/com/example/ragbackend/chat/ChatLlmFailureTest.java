package com.example.ragbackend.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.knowledge.entity.KnowledgeBase;
import com.example.ragbackend.knowledge.mapper.KnowledgeBaseMapper;
import com.example.ragbackend.llm.model.LlmRequest;
import com.example.ragbackend.llm.service.LlmService;
import com.example.ragbackend.retrieval.dto.RetrieveRequest;
import com.example.ragbackend.retrieval.dto.RetrieveResponse;
import com.example.ragbackend.retrieval.service.RetrievalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatLlmFailureTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @MockBean
    private RetrievalService retrievalService;

    @MockBean
    private LlmService llmService;

    @BeforeEach
    void cleanData() {
        jdbcTemplate.execute("DELETE FROM llm_call_log");
        jdbcTemplate.execute("DELETE FROM retrieval_log");
        jdbcTemplate.execute("DELETE FROM chat_message");
        jdbcTemplate.execute("DELETE FROM chat_session");
        jdbcTemplate.execute("DELETE FROM document_chunk");
        jdbcTemplate.execute("DELETE FROM document");
        jdbcTemplate.execute("DELETE FROM knowledge_base");
    }

    @Test
    void persistsFailureLogWhenOnceLlmCallFails() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase();
        when(retrievalService.retrieve(any(RetrieveRequest.class))).thenReturn(
                new RetrieveResponse(knowledgeBaseId, "question", 5, java.util.List.of())
        );
        when(llmService.complete(any(LlmRequest.class))).thenThrow(
                new BusinessException(
                        "QWEN_LLM_REQUEST_FAILED",
                        "Qwen LLM request failed"
                )
        );

        mockMvc.perform(post("/api/chat/once")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledgeBaseId": %d,
                                  "question": "question"
                                }
                                """.formatted(knowledgeBaseId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("QWEN_LLM_REQUEST_FAILED"))
                .andExpect(jsonPath("$.message").value("Qwen LLM request failed"));

        Integer failureLogCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM llm_call_log
                WHERE knowledge_base_id = ? AND success = FALSE
                  AND error_message = 'Qwen LLM request failed'
                """,
                Integer.class,
                knowledgeBaseId
        );
        String requestId = jdbcTemplate.queryForObject(
                "SELECT request_id FROM llm_call_log WHERE knowledge_base_id = ?",
                String.class,
                knowledgeBaseId
        );
        Integer messageCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chat_message WHERE request_id = ?",
                Integer.class,
                requestId
        );

        assertThat(failureLogCount).isEqualTo(1);
        assertThat(requestId).isNotBlank();
        assertThat(messageCount).isZero();
    }

    @Test
    void sendsErrorEventAndPersistsFailureLogWhenStreamLlmCallFails() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase();
        when(retrievalService.retrieve(any(RetrieveRequest.class))).thenReturn(
                new RetrieveResponse(knowledgeBaseId, "question", 5, java.util.List.of())
        );
        when(llmService.complete(any(LlmRequest.class))).thenThrow(
                new BusinessException(
                        "QWEN_LLM_REQUEST_FAILED",
                        "Qwen LLM request failed"
                )
        );

        MvcResult pendingResult = mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledgeBaseId": %d,
                                  "question": "question"
                                }
                                """.formatted(knowledgeBaseId)))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult completedResult = mockMvc.perform(asyncDispatch(pendingResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn();

        String eventStream = completedResult.getResponse().getContentAsString();
        assertThat(eventStream)
                .contains("event:retrieval_start")
                .contains("event:retrieval_result")
                .contains("event:error")
                .contains("\"message\":\"Qwen LLM request failed\"")
                .doesNotContain("event:done")
                .doesNotContain("java.lang");

        Integer failureLogCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM llm_call_log
                WHERE knowledge_base_id = ? AND success = FALSE
                """,
                Integer.class,
                knowledgeBaseId
        );
        assertThat(failureLogCount).isEqualTo(1);
    }

    private Long createKnowledgeBase() {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setName("LLM failure knowledge base");
        knowledgeBase.setDescription("Created for LLM failure logging test");
        knowledgeBase.setOwnerId(1L);
        knowledgeBase.setVisibility("PRIVATE");
        knowledgeBaseMapper.insert(knowledgeBase);
        return knowledgeBase.getId();
    }
}
