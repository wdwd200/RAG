package com.example.ragbackend.chat.service.impl;

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
import com.example.ragbackend.chat.service.ChatService;
import com.example.ragbackend.chat.service.ChatSessionService;
import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.knowledge.service.KnowledgeBaseService;
import com.example.ragbackend.llm.model.LlmRequest;
import com.example.ragbackend.llm.model.LlmResponse;
import com.example.ragbackend.llm.service.LlmService;
import com.example.ragbackend.retrieval.dto.RetrieveRequest;
import com.example.ragbackend.retrieval.dto.RetrieveResponse;
import com.example.ragbackend.retrieval.dto.RetrievedChunk;
import com.example.ragbackend.retrieval.service.RetrievalService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
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

    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final RetrievalService retrievalService;
    private final PromptBuilder promptBuilder;
    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ChatOnceResponse once(ChatOnceRequest request) {
        validateRequest(request);
        knowledgeBaseService.getById(request.knowledgeBaseId());

        ChatSession session = resolveSession(request);
        ChatMessage userMessage = chatMessageService.create(
                session.getId(),
                ChatMessageRole.USER,
                request.question().trim(),
                null
        );

        int topK = request.topK() == null ? DEFAULT_TOP_K : request.topK();
        RetrieveResponse retrieval = retrievalService.retrieve(
                new RetrieveRequest(request.knowledgeBaseId(), request.question().trim(), topK)
        );
        List<ChatReferenceResponse> references = toReferences(retrieval.chunks());
        String prompt = promptBuilder.build(
                new RagPromptRequest(request.question().trim(), toPromptChunks(retrieval.chunks()))
        );
        LlmResponse llmResponse = llmService.complete(new LlmRequest(prompt, null, null));
        validateLlmResponse(llmResponse);

        ChatMessage assistantMessage = chatMessageService.create(
                session.getId(),
                ChatMessageRole.ASSISTANT,
                llmResponse.content(),
                serializeReferences(references)
        );

        return new ChatOnceResponse(
                session.getId(),
                userMessage.getId(),
                assistantMessage.getId(),
                llmResponse.content(),
                references
        );
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
