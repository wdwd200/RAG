package com.example.ragbackend.chat.service;

import com.example.ragbackend.chat.entity.ChatMessage;
import com.example.ragbackend.chat.enums.ChatMessageRole;
import java.util.List;

public interface ChatMessageService {

    ChatMessage create(Long sessionId, ChatMessageRole role, String content, String referencesJson);

    List<ChatMessage> findBySessionId(Long sessionId);
}
