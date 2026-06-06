package com.example.ragbackend.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ragbackend.chat.entity.ChatMessage;
import com.example.ragbackend.chat.mapper.ChatSessionMapper;
import com.example.ragbackend.chat.service.ChatMessageService;
import com.example.ragbackend.knowledge.entity.KnowledgeBase;
import com.example.ragbackend.knowledge.mapper.KnowledgeBaseMapper;
import com.example.ragbackend.retrieval.dto.RetrieveRequest;
import com.example.ragbackend.retrieval.dto.RetrieveResponse;
import com.example.ragbackend.retrieval.dto.RetrievedChunk;
import com.example.ragbackend.retrieval.service.RetrievalService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Autowired
    private ChatMessageService chatMessageService;

    @MockBean
    private RetrievalService retrievalService;

    @BeforeEach
    void cleanData() {
        jdbcTemplate.execute("DELETE FROM chat_message");
        jdbcTemplate.execute("DELETE FROM chat_session");
        jdbcTemplate.execute("DELETE FROM document_chunk");
        jdbcTemplate.execute("DELETE FROM document");
        jdbcTemplate.execute("DELETE FROM knowledge_base");
    }

    @Test
    void createsSessionSavesMessagesAndReturnsMockAnswerWithReferences() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase();
        RetrievedChunk chunk = new RetrievedChunk(
                101L,
                202L,
                knowledgeBaseId,
                0,
                0.93d,
                "The handbook says annual leave carries over for one year."
        );
        when(retrievalService.retrieve(any(RetrieveRequest.class))).thenReturn(
                new RetrieveResponse(knowledgeBaseId, "How long can leave carry over?", 5, List.of(chunk))
        );

        MvcResult result = mockMvc.perform(post("/api/chat/once")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledgeBaseId": %d,
                                  "question": "How long can leave carry over?"
                                }
                                """.formatted(knowledgeBaseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").isNumber())
                .andExpect(jsonPath("$.data.userMessageId").isNumber())
                .andExpect(jsonPath("$.data.assistantMessageId").isNumber())
                .andExpect(jsonPath("$.data.answer").value(
                        org.hamcrest.Matchers.containsString("Mock answer for prompt:")
                ))
                .andExpect(jsonPath("$.data.answer").value(
                        org.hamcrest.Matchers.containsString("How long can leave carry over?")
                ))
                .andExpect(jsonPath("$.data.references.length()").value(1))
                .andExpect(jsonPath("$.data.references[0].chunkId").value(101))
                .andExpect(jsonPath("$.data.references[0].knowledgeBaseId").value(knowledgeBaseId))
                .andExpect(jsonPath("$.data.references[0].content").value(
                        "The handbook says annual leave carries over for one year."
                ))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        Long sessionId = response.at("/data/sessionId").asLong();
        List<ChatMessage> messages = chatMessageService.findBySessionId(sessionId);

        assertThat(chatSessionMapper.selectById(sessionId)).isNotNull();
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getRole()).isEqualTo("USER");
        assertThat(messages.get(0).getContent()).isEqualTo("How long can leave carry over?");
        assertThat(messages.get(1).getRole()).isEqualTo("ASSISTANT");
        assertThat(messages.get(1).getContent()).contains("Mock answer for prompt:");
        assertThat(messages.get(1).getReferencesJson())
                .contains("\"chunkId\":101")
                .contains("\"knowledgeBaseId\":" + knowledgeBaseId);

        ArgumentCaptor<RetrieveRequest> requestCaptor = ArgumentCaptor.forClass(RetrieveRequest.class);
        verify(retrievalService).retrieve(requestCaptor.capture());
        assertThat(requestCaptor.getValue().knowledgeBaseId()).isEqualTo(knowledgeBaseId);
        assertThat(requestCaptor.getValue().topK()).isEqualTo(5);
    }

    @Test
    void rejectsBlankQuestion() throws Exception {
        mockMvc.perform(post("/api/chat/once")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledgeBaseId": 1,
                                  "question": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void returnsClearErrorWhenSessionDoesNotExist() throws Exception {
        Long knowledgeBaseId = createKnowledgeBase();

        mockMvc.perform(post("/api/chat/once")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledgeBaseId": %d,
                                  "sessionId": 999999,
                                  "question": "question"
                                }
                                """.formatted(knowledgeBaseId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAT_SESSION_NOT_FOUND"));
    }

    private Long createKnowledgeBase() {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setName("Chat knowledge base");
        knowledgeBase.setDescription("Created for chat tests");
        knowledgeBase.setOwnerId(1L);
        knowledgeBase.setVisibility("PRIVATE");
        knowledgeBaseMapper.insert(knowledgeBase);
        return knowledgeBase.getId();
    }
}
