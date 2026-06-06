package com.example.ragbackend.chat.controller;

import com.example.ragbackend.chat.dto.ChatOnceRequest;
import com.example.ragbackend.chat.dto.ChatOnceResponse;
import com.example.ragbackend.chat.service.ChatService;
import com.example.ragbackend.chat.service.ChatStreamService;
import com.example.ragbackend.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Chat", description = "RAG chat API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatStreamService chatStreamService;

    @Operation(summary = "Complete one RAG question and return a JSON response")
    @PostMapping("/once")
    public ApiResponse<ChatOnceResponse> once(@Valid @RequestBody ChatOnceRequest request) {
        return ApiResponse.success(chatService.once(request));
    }

    @Operation(summary = "Stream one RAG answer as server-sent events")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody ChatOnceRequest request) {
        return chatStreamService.stream(request);
    }
}
