package com.example.ragbackend.chat.service;

import com.example.ragbackend.chat.entity.ChatSession;

public interface ChatSessionService {

    ChatSession create(Long knowledgeBaseId, Long userId, String title);

    ChatSession getById(Long id);
}
