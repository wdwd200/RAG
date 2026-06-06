package com.example.ragbackend.chat.service.impl;

import com.example.ragbackend.chat.dto.ChatOnceRequest;
import com.example.ragbackend.chat.dto.ChatOnceResponse;
import com.example.ragbackend.chat.dto.ChatReferenceResponse;
import com.example.ragbackend.chat.dto.ChatStreamEvent;
import com.example.ragbackend.chat.service.ChatProgressListener;
import com.example.ragbackend.chat.service.ChatService;
import com.example.ragbackend.chat.service.ChatStreamService;
import com.example.ragbackend.common.exception.BusinessException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@Slf4j
public class ChatStreamServiceImpl implements ChatStreamService {

    static final long SSE_TIMEOUT_MS = 120_000L;
    static final int ANSWER_DELTA_SIZE = 64;

    private static final String CHAT_STREAM_SEND_FAILED_CODE = "CHAT_STREAM_SEND_FAILED";
    private static final int MAX_STREAM_ERROR_MESSAGE_LENGTH = 500;

    private final ChatService chatService;
    private final Executor taskExecutor;

    public ChatStreamServiceImpl(
            ChatService chatService,
            @Qualifier("chatStreamTaskExecutor") Executor taskExecutor) {
        this.chatService = chatService;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public SseEmitter stream(ChatOnceRequest request) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitter.onTimeout(emitter::complete);
        taskExecutor.execute(() -> executeStream(request, emitter));
        return emitter;
    }

    private void executeStream(ChatOnceRequest request, SseEmitter emitter) {
        AtomicReference<String> requestId = new AtomicReference<>();
        try {
            ChatOnceResponse response = chatService.execute(
                    request,
                    progressListener(emitter, requestId)
            );
            sendAnswerDeltas(emitter, response.requestId(), response.answer());
            send(emitter, "references", response.requestId(), response.references());
            send(emitter, "done", response.requestId(), Map.of(
                    "completed", true,
                    "sessionId", response.sessionId(),
                    "userMessageId", response.userMessageId(),
                    "assistantMessageId", response.assistantMessageId()
            ));
            emitter.complete();
        } catch (RuntimeException ex) {
            sendError(emitter, requestId.get(), ex);
        }
    }

    private ChatProgressListener progressListener(
            SseEmitter emitter,
            AtomicReference<String> requestId) {
        return new ChatProgressListener() {
            @Override
            public void onRetrievalStart(String currentRequestId) {
                requestId.set(currentRequestId);
                send(
                        emitter,
                        "retrieval_start",
                        currentRequestId,
                        Map.of("started", true)
                );
            }

            @Override
            public void onRetrievalResult(
                    String currentRequestId,
                    List<ChatReferenceResponse> references) {
                send(emitter, "retrieval_result", currentRequestId, references);
            }
        };
    }

    private void sendAnswerDeltas(SseEmitter emitter, String requestId, String answer) {
        for (int start = 0; start < answer.length(); start += ANSWER_DELTA_SIZE) {
            int end = Math.min(answer.length(), start + ANSWER_DELTA_SIZE);
            send(
                    emitter,
                    "answer_delta",
                    requestId,
                    Map.of("content", answer.substring(start, end))
            );
        }
    }

    private void send(
            SseEmitter emitter,
            String eventType,
            String requestId,
            Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventType)
                    .data(
                            new ChatStreamEvent(requestId, eventType, data),
                            MediaType.APPLICATION_JSON
                    ));
        } catch (IOException ex) {
            throw new BusinessException(
                    CHAT_STREAM_SEND_FAILED_CODE,
                    "Failed to send chat stream event"
            );
        }
    }

    private void sendError(SseEmitter emitter, String requestId, RuntimeException failure) {
        String message = errorMessage(failure);
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(
                            new ChatStreamEvent(
                                    requestId,
                                    "error",
                                    Map.of("message", message)
                            ),
                            MediaType.APPLICATION_JSON
                    ));
            emitter.complete();
        } catch (IOException | RuntimeException sendFailure) {
            log.debug("Chat stream connection closed before error event was sent");
            emitter.completeWithError(sendFailure);
        }
    }

    private String errorMessage(RuntimeException failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            message = "Chat stream failed";
        }
        return message.length() <= MAX_STREAM_ERROR_MESSAGE_LENGTH
                ? message
                : message.substring(0, MAX_STREAM_ERROR_MESSAGE_LENGTH);
    }
}
