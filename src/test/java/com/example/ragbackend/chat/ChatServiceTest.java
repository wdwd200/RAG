package com.example.ragbackend.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ragbackend.audit.entity.LlmCallLog;
import com.example.ragbackend.audit.entity.RetrievalLog;
import com.example.ragbackend.audit.service.LlmCallLogService;
import com.example.ragbackend.audit.service.RetrievalLogService;
import com.example.ragbackend.chat.dto.ChatOnceRequest;
import com.example.ragbackend.chat.dto.ChatOnceResponse;
import com.example.ragbackend.chat.entity.ChatMessage;
import com.example.ragbackend.chat.entity.ChatSession;
import com.example.ragbackend.chat.enums.ChatMessageRole;
import com.example.ragbackend.chat.prompt.PromptBuilder;
import com.example.ragbackend.chat.prompt.PromptContextChunk;
import com.example.ragbackend.chat.prompt.RagPromptRequest;
import com.example.ragbackend.chat.service.ChatMessageService;
import com.example.ragbackend.chat.service.ChatProgressListener;
import com.example.ragbackend.chat.service.ChatService;
import com.example.ragbackend.chat.service.ChatSessionService;
import com.example.ragbackend.chat.service.impl.ChatServiceImpl;
import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.knowledge.service.KnowledgeBaseService;
import com.example.ragbackend.llm.config.LlmProperties;
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

    @SuppressWarnings("unchecked")
    @Test
    void orchestratesRetrievalPromptLlmAndMessagePersistence() {
        ChatSessionService sessionService = mock(ChatSessionService.class);
        ChatMessageService messageService = mock(ChatMessageService.class);
        RetrievalLogService retrievalLogService = mock(RetrievalLogService.class);
        LlmCallLogService llmCallLogService = mock(LlmCallLogService.class);
        KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
        RetrievalService retrievalService = mock(RetrievalService.class);
        PromptBuilder promptBuilder = mock(PromptBuilder.class);
        LlmService llmService = mock(LlmService.class);
        LlmProperties llmProperties = new LlmProperties();
        ChatService chatService = new ChatServiceImpl(
                sessionService,
                messageService,
                retrievalLogService,
                llmCallLogService,
                knowledgeBaseService,
                retrievalService,
                promptBuilder,
                llmService,
                llmProperties,
                new ObjectMapper()
        );

        ChatSession session = session(30L, 7L);
        ChatMessage userMessage = message(31L, 30L, "USER", "question");
        ChatMessage assistantMessage = message(32L, 30L, "ASSISTANT", "mock answer");
        RetrievedChunk chunk = new RetrievedChunk(11L, 12L, 7L, 0, 0.91d, "database fact");
        RetrieveResponse retrieval = new RetrieveResponse(7L, "question", 5, List.of(chunk));

        when(sessionService.create(7L, 1L, "question")).thenReturn(session);
        when(messageService.create(
                eq(30L),
                eq(ChatMessageRole.USER),
                eq("question"),
                isNull(),
                anyString()
        )).thenReturn(userMessage);
        when(retrievalService.retrieve(new RetrieveRequest(7L, "question", 5))).thenReturn(retrieval);
        when(promptBuilder.build(new RagPromptRequest(
                "question",
                List.of(new PromptContextChunk(11L, 12L, null, 0.91d, "database fact"))
        ))).thenReturn("built prompt");
        when(llmService.complete(new LlmRequest("built prompt", null, null)))
                .thenReturn(new LlmResponse("mock answer", "mock-rag-assistant", true, null));
        when(messageService.create(
                eq(30L),
                eq(ChatMessageRole.ASSISTANT),
                eq("mock answer"),
                anyString(),
                anyString()
        )).thenReturn(assistantMessage);

        ChatOnceResponse response = chatService.once(new ChatOnceRequest(7L, null, "question", null));

        assertThat(response.requestId()).isNotBlank();
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

        ArgumentCaptor<LlmCallLog> llmLogCaptor = ArgumentCaptor.forClass(LlmCallLog.class);
        verify(llmCallLogService).save(llmLogCaptor.capture());
        LlmCallLog llmLog = llmLogCaptor.getValue();
        assertThat(llmLog.getRequestId()).isEqualTo(response.requestId());
        assertThat(llmLog.getSessionId()).isEqualTo(30L);
        assertThat(llmLog.getMessageId()).isEqualTo(31L);
        assertThat(llmLog.getKnowledgeBaseId()).isEqualTo(7L);
        assertThat(llmLog.getProvider()).isEqualTo("mock");
        assertThat(llmLog.getModelName()).isEqualTo("mock-rag-assistant");
        assertThat(llmLog.getLatencyMs()).isNotNegative();
        assertThat(llmLog.getSuccess()).isTrue();
        assertThat(llmLog.getErrorMessage()).isNull();

        ArgumentCaptor<List<RetrievalLog>> logsCaptor = ArgumentCaptor.forClass(List.class);
        verify(retrievalLogService).saveLogs(logsCaptor.capture());
        assertThat(logsCaptor.getValue()).hasSize(1);
        RetrievalLog log = logsCaptor.getValue().get(0);
        assertThat(log.getRequestId()).isEqualTo(response.requestId());
        assertThat(log.getSessionId()).isEqualTo(30L);
        assertThat(log.getMessageId()).isEqualTo(31L);
        assertThat(log.getKnowledgeBaseId()).isEqualTo(7L);
        assertThat(log.getQuestion()).isEqualTo("question");
        assertThat(log.getRetrieverType()).isEqualTo("VECTOR");
        assertThat(log.getTopK()).isEqualTo(5);
        assertThat(log.getChunkId()).isEqualTo(11L);
        assertThat(log.getDocumentId()).isEqualTo(12L);
        assertThat(log.getRankPosition()).isEqualTo(1);
        assertThat(log.getScore()).isEqualTo(0.91d);

        ArgumentCaptor<String> userRequestIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(messageService).create(
                eq(30L),
                eq(ChatMessageRole.USER),
                eq("question"),
                isNull(),
                userRequestIdCaptor.capture()
        );
        ArgumentCaptor<String> referencesCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> assistantRequestIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(messageService).create(
                eq(30L),
                eq(ChatMessageRole.ASSISTANT),
                eq("mock answer"),
                referencesCaptor.capture(),
                assistantRequestIdCaptor.capture()
        );
        assertThat(userRequestIdCaptor.getValue()).isEqualTo(response.requestId());
        assertThat(assistantRequestIdCaptor.getValue()).isEqualTo(response.requestId());
        assertThat(referencesCaptor.getValue())
                .contains("\"chunkId\":11")
                .contains("\"knowledgeBaseId\":7")
                .contains("\"content\":\"database fact\"");
    }

    @Test
    void writesFailureLogAndRethrowsLlmException() {
        ChatSessionService sessionService = mock(ChatSessionService.class);
        ChatMessageService messageService = mock(ChatMessageService.class);
        RetrievalLogService retrievalLogService = mock(RetrievalLogService.class);
        LlmCallLogService llmCallLogService = mock(LlmCallLogService.class);
        KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
        RetrievalService retrievalService = mock(RetrievalService.class);
        PromptBuilder promptBuilder = mock(PromptBuilder.class);
        LlmService llmService = mock(LlmService.class);
        LlmProperties llmProperties = new LlmProperties();
        ChatService chatService = new ChatServiceImpl(
                sessionService,
                messageService,
                retrievalLogService,
                llmCallLogService,
                knowledgeBaseService,
                retrievalService,
                promptBuilder,
                llmService,
                llmProperties,
                new ObjectMapper()
        );

        when(sessionService.create(7L, 1L, "question")).thenReturn(session(30L, 7L));
        when(messageService.create(
                eq(30L),
                eq(ChatMessageRole.USER),
                eq("question"),
                isNull(),
                anyString()
        )).thenReturn(message(31L, 30L, "USER", "question"));
        when(retrievalService.retrieve(new RetrieveRequest(7L, "question", 5)))
                .thenReturn(new RetrieveResponse(7L, "question", 5, List.of()));
        when(promptBuilder.build(new RagPromptRequest("question", List.of())))
                .thenReturn("built prompt");
        when(llmService.complete(new LlmRequest("built prompt", null, null)))
                .thenThrow(new BusinessException(
                        "QWEN_LLM_REQUEST_FAILED",
                        "Qwen LLM request failed"
                ));

        assertThatThrownBy(() -> chatService.once(
                new ChatOnceRequest(7L, null, "question", null)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Qwen LLM request failed");

        ArgumentCaptor<LlmCallLog> logCaptor = ArgumentCaptor.forClass(LlmCallLog.class);
        verify(llmCallLogService).saveInNewTransaction(logCaptor.capture());
        LlmCallLog log = logCaptor.getValue();
        assertThat(log.getRequestId()).isNotBlank();
        assertThat(log.getSessionId()).isEqualTo(30L);
        assertThat(log.getMessageId()).isEqualTo(31L);
        assertThat(log.getProvider()).isEqualTo("mock");
        assertThat(log.getModelName()).isEqualTo("mock-rag-assistant");
        assertThat(log.getLatencyMs()).isNotNegative();
        assertThat(log.getSuccess()).isFalse();
        assertThat(log.getErrorMessage()).isEqualTo("Qwen LLM request failed");
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
