package com.example.ragbackend.chat.service.impl;

import com.example.ragbackend.chat.entity.ChatSession;
import com.example.ragbackend.chat.mapper.ChatSessionMapper;
import com.example.ragbackend.chat.service.ChatSessionService;
import com.example.ragbackend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService {

    private static final String CHAT_SESSION_NOT_FOUND_CODE = "CHAT_SESSION_NOT_FOUND";

    private final ChatSessionMapper chatSessionMapper;

    @Override
    public ChatSession create(Long knowledgeBaseId, Long userId, String title) {
        ChatSession session = new ChatSession();
        session.setKnowledgeBaseId(knowledgeBaseId);
        session.setUserId(userId);
        session.setTitle(title);
        chatSessionMapper.insert(session);
        return chatSessionMapper.selectById(session.getId());
    }

    @Override
    public ChatSession getById(Long id) {
        ChatSession session = chatSessionMapper.selectById(id);
        if (session == null) {
            throw new BusinessException(CHAT_SESSION_NOT_FOUND_CODE, "Chat session not found: " + id);
        }
        return session;
    }
}
