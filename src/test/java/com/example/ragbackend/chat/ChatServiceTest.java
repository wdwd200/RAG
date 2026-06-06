package com.example.ragbackend.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ragbackend.chat.dto.ChatOnceRequest;
import com.example.ragbackend.chat.dto.ChatOnceResponse;
import com.example.ragbackend.chat.entity.ChatMessage;
import com.example.ragbackend.chat.entity.ChatSession;
import com.example.ragbackend.chat.enums.ChatMessageRole;
import com.example.ragbackend.chat.prompt.PromptBuilder;
import com.example.ragbackend.chat.prompt.PromptContextChunk;
import com.example.ragbackend.chat.prompt.RagPromptRequest;
import com.example.ragbackend.chat.service.ChatMessageService;
import com.example.ragbackend.chat.service.ChatService;
import com.example.ragbackend.chat.service.ChatSessionService;
import com.example.ragbackend.chat.service.impl.ChatServiceImpl;
import com.example.ragbackend.knowledge.service.KnowledgeBaseService;
import com.example.ragbackend.llm.model.LlmRequest;
import com.example.ragbackend.llm.model.LlmResponse;
import com.example.ragbackend.llm.service.LlmService;
import com.example.ragbackend.retrieval.dto.RetrieveRequest;
import com.example.ragbackend.retrieval.dto.RetrieveResponse;
import com.example.ragbackend.retrieval.dto.RetrievedChunk;
import com.example.ragbackend.retrieval.service.RetrievalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChatServiceTest {

    @Test
    void orchestratesRetrievalPromptLlmAndMessagePersistence() {
        ChatSessionService sessionService = mock(ChatSessionService.class);
        ChatMessageService messageService = mock(ChatMessageService.class);
        KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
        RetrievalService retrievalService = mock(RetrievalService.class);
        PromptBuilder promptBuilder = mock(PromptBuilder.class);
        LlmService llmService = mock(LlmService.class);
        ChatService chatService = new ChatServiceImpl(
                sessionService,
                messageService,
                knowledgeBaseService,
                retrievalService,
                promptBuilder,
                llmService,
                new ObjectMapper()
        );

        ChatSession session = session(30L, 7L);
        ChatMessage userMessage = message(31L, 30L, "USER", "question");
        ChatMessage assistantMessage = message(32L, 30L, "ASSISTANT", "mock answer");
        RetrievedChunk chunk = new RetrievedChunk(11L, 12L, 7L, 0, 0.91d, "database fact");
        RetrieveResponse retrieval = new RetrieveResponse(7L, "question", 5, List.of(chunk));

        when(sessionService.create(7L, 1L, "question")).thenReturn(session);
        when(messageService.create(30L, ChatMessageRole.USER, "question", null)).thenReturn(userMessage);
        when(retrievalService.retrieve(new RetrieveRequest(7L, "question", 5))).thenReturn(retrieval);
        when(promptBuilder.build(new RagPromptRequest(
                "question",
                List.of(new PromptContextChunk(11L, 12L, null, 0.91d, "database fact"))
        ))).thenReturn("built prompt");
        when(llmService.complete(new LlmRequest("built prompt", null, null)))
                .thenReturn(new LlmResponse("mock answer", "mock-rag-assistant", true, null));
        when(messageService.create(
                org.mockito.ArgumentMatchers.eq(30L),
                org.mockito.ArgumentMatchers.eq(ChatMessageRole.ASSISTANT),
                org.mockito.ArgumentMatchers.eq("mock answer"),
                anyString()
        )).thenReturn(assistantMessage);

        ChatOnceResponse response = chatService.once(new ChatOnceRequest(7L, null, "question", null));

        assertThat(response.sessionId()).isEqualTo(30L);
        assertThat(response.userMessageId()).isEqualTo(31L);
        assertThat(response.assistantMessageId()).isEqualTo(32L);
        assertThat(response.answer()).isEqualTo("mock answer");
        assertThat(response.references()).hasSize(1);
        assertThat(response.references().get(0).content()).isEqualTo("database fact");

        verify(knowledgeBaseService).getById(7L);
        verify(retrievalService).retrieve(new RetrieveRequest(7L, "question", 5));
        verify(promptBuilder).build(new RagPromptRequest(
                "question",
                List.of(new PromptContextChunk(11L, 12L, null, 0.91d, "database fact"))
        ));
        verify(llmService).complete(new LlmRequest("built prompt", null, null));

        ArgumentCaptor<String> referencesCaptor = ArgumentCaptor.forClass(String.class);
        verify(messageService).create(
                org.mockito.ArgumentMatchers.eq(30L),
                org.mockito.ArgumentMatchers.eq(ChatMessageRole.ASSISTANT),
                org.mockito.ArgumentMatchers.eq("mock answer"),
                referencesCaptor.capture()
        );
        assertThat(referencesCaptor.getValue())
                .contains("\"chunkId\":11")
                .contains("\"knowledgeBaseId\":7")
                .contains("\"content\":\"database fact\"");
    }

    private ChatSession session(Long id, Long knowledgeBaseId) {
        ChatSession session = new ChatSession();
        session.setId(id);
        session.setKnowledgeBaseId(knowledgeBaseId);
        session.setUserId(1L);
        session.setTitle("question");
        return session;
    }

    private ChatMessage message(Long id, Long sessionId, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setId(id);
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        return message;
    }
}
