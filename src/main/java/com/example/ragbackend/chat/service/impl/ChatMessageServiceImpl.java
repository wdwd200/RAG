package com.example.ragbackend.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ragbackend.chat.entity.ChatMessage;
import com.example.ragbackend.chat.enums.ChatMessageRole;
import com.example.ragbackend.chat.mapper.ChatMessageMapper;
import com.example.ragbackend.chat.service.ChatMessageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageMapper chatMessageMapper;

    @Override
    public ChatMessage create(
            Long sessionId,
            ChatMessageRole role,
            String content,
            String referencesJson) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role.name());
        message.setContent(content);
        message.setReferencesJson(referencesJson);
        chatMessageMapper.insert(message);
        return chatMessageMapper.selectById(message.getId());
    }

    @Override
    public List<ChatMessage> findBySessionId(Long sessionId) {
        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreatedAt)
                .orderByAsc(ChatMessage::getId);
        return chatMessageMapper.selectList(queryWrapper);
    }
}
