package com.example.ragbackend.chat.service.impl;

import com.example.ragbackend.audit.entity.LlmCallLog;
import com.example.ragbackend.audit.entity.RetrievalLog;
import com.example.ragbackend.audit.service.LlmCallLogService;
import com.example.ragbackend.audit.service.RetrievalLogService;
import com.example.ragbackend.chat.dto.ChatOnceRequest;
import com.example.ragbackend.chat.dto.ChatOnceResponse;
import com.example.ragbackend.chat.dto.ChatReferenceResponse;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    static final int DEFAULT_TOP_K = 5;

    private static final long DEFAULT_USER_ID = 1L;
    private static final int MAX_TITLE_LENGTH = 200;
    private static final String CHAT_REQUEST_INVALID_CODE = "CHAT_REQUEST_INVALID";
    private static final String CHAT_SESSION_KNOWLEDGE_BASE_MISMATCH_CODE =
            "CHAT_SESSION_KNOWLEDGE_BASE_MISMATCH";
    private static final String CHAT_REFERENCES_SERIALIZATION_FAILED_CODE =
            "CHAT_REFERENCES_SERIALIZATION_FAILED";
    private static final String LLM_COMPLETION_FAILED_CODE = "LLM_COMPLETION_FAILED";
    private static final int MAX_LLM_ERROR_MESSAGE_LENGTH = 2000;

    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final RetrievalLogService retrievalLogService;
    private final LlmCallLogService llmCallLogService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final RetrievalService retrievalService;
    private final PromptBuilder promptBuilder;
    private final LlmService llmService;
    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ChatOnceResponse once(ChatOnceRequest request) {
        return executeWorkflow(request, ChatProgressListener.noOp());
    }

    @Override
    @Transactional
    public ChatOnceResponse execute(
            ChatOnceRequest request,
            ChatProgressListener progressListener) {
        return executeWorkflow(
                request,
                progressListener == null ? ChatProgressListener.noOp() : progressListener
        );
    }

    private ChatOnceResponse executeWorkflow(
            ChatOnceRequest request,
            ChatProgressListener progressListener) {
        validateRequest(request);
        String requestId = UUID.randomUUID().toString();
        progressListener.onRetrievalStart(requestId);
        knowledgeBaseService.getById(request.knowledgeBaseId());

        ChatSession session = resolveSession(request);
        ChatMessage userMessage = chatMessageService.create(
                session.getId(),
                ChatMessageRole.USER,
                request.question().trim(),
                null,
                requestId
        );

        int topK = request.topK() == null ? DEFAULT_TOP_K : request.topK();
        RetrieveResponse retrieval = retrievalService.retrieve(
                new RetrieveRequest(request.knowledgeBaseId(), request.question().trim(), topK)
        );
        retrievalLogService.saveLogs(toRetrievalLogs(
                requestId,
                session.getId(),
                userMessage.getId(),
                request.knowledgeBaseId(),
                request.question().trim(),
                topK,
                retrieval.chunks()
        ));
        List<ChatReferenceResponse> references = toReferences(retrieval.chunks());
        progressListener.onRetrievalResult(requestId, references);
        String prompt = promptBuilder.build(
                new RagPromptRequest(request.question().trim(), toPromptChunks(retrieval.chunks()))
        );
        LlmResponse llmResponse = completeWithLog(
                requestId,
                session.getId(),
                userMessage.getId(),
                request.knowledgeBaseId(),
                prompt
        );

        ChatMessage assistantMessage = chatMessageService.create(
                session.getId(),
                ChatMessageRole.ASSISTANT,
                llmResponse.content(),
                serializeReferences(references),
                requestId
        );

        return new ChatOnceResponse(
                requestId,
                session.getId(),
                userMessage.getId(),
                assistantMessage.getId(),
                llmResponse.content(),
                references
        );
    }

    private LlmResponse completeWithLog(
            String requestId,
            Long sessionId,
            Long messageId,
            Long knowledgeBaseId,
            String prompt) {
        long startedAt = System.nanoTime();
        LlmResponse response;
        try {
            response = llmService.complete(new LlmRequest(prompt, null, null));
            validateLlmResponse(response);
        } catch (RuntimeException ex) {
            saveFailedLlmCallLog(
                    requestId,
                    sessionId,
                    messageId,
                    knowledgeBaseId,
                    elapsedMillis(startedAt),
                    ex
            );
            throw ex;
        }

        llmCallLogService.save(createLlmCallLog(
                requestId,
                sessionId,
                messageId,
                knowledgeBaseId,
                response.model(),
                elapsedMillis(startedAt),
                true,
                null
        ));
        return response;
    }

    private void saveFailedLlmCallLog(
            String requestId,
            Long sessionId,
            Long messageId,
            Long knowledgeBaseId,
            long latencyMs,
            RuntimeException failure) {
        try {
            llmCallLogService.saveInNewTransaction(createLlmCallLog(
                    requestId,
                    sessionId,
                    messageId,
                    knowledgeBaseId,
                    llmProperties.getModel(),
                    latencyMs,
                    false,
                    errorMessage(failure)
            ));
        } catch (RuntimeException logFailure) {
            log.warn("Failed to persist LLM failure log for requestId={}", requestId);
        }
    }

    private LlmCallLog createLlmCallLog(
            String requestId,
            Long sessionId,
            Long messageId,
            Long knowledgeBaseId,
            String modelName,
            long latencyMs,
            boolean success,
            String errorMessage) {
        LlmCallLog logEntry = new LlmCallLog();
        logEntry.setRequestId(requestId);
        logEntry.setSessionId(sessionId);
        logEntry.setMessageId(messageId);
        logEntry.setKnowledgeBaseId(knowledgeBaseId);
        logEntry.setProvider(llmProperties.getProvider());
        logEntry.setModelName(
                modelName == null || modelName.isBlank()
                        ? llmProperties.getModel()
                        : modelName
        );
        logEntry.setPromptTokens(null);
        logEntry.setCompletionTokens(null);
        logEntry.setLatencyMs(latencyMs);
        logEntry.setSuccess(success);
        logEntry.setErrorMessage(errorMessage);
        return logEntry;
    }

    private long elapsedMillis(long startedAt) {
        return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
    }

    private String errorMessage(RuntimeException failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            message = failure.getClass().getSimpleName();
        }
        return message.length() <= MAX_LLM_ERROR_MESSAGE_LENGTH
                ? message
                : message.substring(0, MAX_LLM_ERROR_MESSAGE_LENGTH);
    }

    private List<RetrievalLog> toRetrievalLogs(
            String requestId,
            Long sessionId,
            Long messageId,
            Long knowledgeBaseId,
            String question,
            int topK,
            List<RetrievedChunk> chunks) {
        List<RetrievalLog> logs = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk chunk = chunks.get(i);
            RetrievalLog log = new RetrievalLog();
            log.setRequestId(requestId);
            log.setSessionId(sessionId);
            log.setMessageId(messageId);
            log.setKnowledgeBaseId(knowledgeBaseId);
            log.setQuestion(question);
            log.setRetrieverType("VECTOR");
            log.setTopK(topK);
            log.setChunkId(chunk.chunkId());
            log.setDocumentId(chunk.documentId());
            log.setRankPosition(i + 1);
            log.setScore(chunk.score());
            logs.add(log);
        }
        return logs;
    }

    private void validateRequest(ChatOnceRequest request) {
        if (request == null
                || request.knowledgeBaseId() == null
                || request.knowledgeBaseId() <= 0
                || request.question() == null
                || request.question().isBlank()) {
            throw new BusinessException(CHAT_REQUEST_INVALID_CODE, "Chat request is invalid");
        }
        if (request.sessionId() != null && request.sessionId() <= 0) {
            throw new BusinessException(CHAT_REQUEST_INVALID_CODE, "Chat session id must be greater than 0");
        }
        if (request.topK() != null && (request.topK() < 1 || request.topK() > 20)) {
            throw new BusinessException(CHAT_REQUEST_INVALID_CODE, "Chat topK must be between 1 and 20");
        }
    }

    private ChatSession resolveSession(ChatOnceRequest request) {
        if (request.sessionId() == null) {
            return chatSessionService.create(
                    request.knowledgeBaseId(),
                    DEFAULT_USER_ID,
                    titleFromQuestion(request.question())
            );
        }

        ChatSession session = chatSessionService.getById(request.sessionId());
        if (!request.knowledgeBaseId().equals(session.getKnowledgeBaseId())) {
            throw new BusinessException(
                    CHAT_SESSION_KNOWLEDGE_BASE_MISMATCH_CODE,
                    "Chat session does not belong to knowledge base: " + request.knowledgeBaseId()
            );
        }
        return session;
    }

    private String titleFromQuestion(String question) {
        String title = question.trim();
        return title.length() <= MAX_TITLE_LENGTH ? title : title.substring(0, MAX_TITLE_LENGTH);
    }

    private List<PromptContextChunk> toPromptChunks(List<RetrievedChunk> chunks) {
        return chunks.stream()
                .map(chunk -> new PromptContextChunk(
                        chunk.chunkId(),
                        chunk.documentId(),
                        null,
                        chunk.score(),
                        chunk.content()
                ))
                .toList();
    }

    private List<ChatReferenceResponse> toReferences(List<RetrievedChunk> chunks) {
        return chunks.stream()
                .map(chunk -> new ChatReferenceResponse(
                        chunk.chunkId(),
                        chunk.documentId(),
                        chunk.knowledgeBaseId(),
                        chunk.chunkIndex(),
                        chunk.score(),
                        chunk.content()
                ))
                .toList();
    }

    private void validateLlmResponse(LlmResponse response) {
        if (response == null || !response.success() || response.content() == null || response.content().isBlank()) {
            String errorMessage = response == null ? "LLM returned no response" : response.errorMessage();
            throw new BusinessException(
                    LLM_COMPLETION_FAILED_CODE,
                    errorMessage == null || errorMessage.isBlank() ? "LLM completion failed" : errorMessage
            );
        }
    }

    private String serializeReferences(List<ChatReferenceResponse> references) {
        try {
            return objectMapper.writeValueAsString(references);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(
                    CHAT_REFERENCES_SERIALIZATION_FAILED_CODE,
                    "Failed to serialize chat references"
            );
        }
    }
}
