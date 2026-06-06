package com.example.ragbackend.chat.service;

import com.example.ragbackend.chat.dto.ChatOnceRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatStreamService {

    SseEmitter stream(ChatOnceRequest request);
}
