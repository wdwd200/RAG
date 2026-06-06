package com.example.ragbackend.chat.service;

import com.example.ragbackend.chat.dto.ChatOnceRequest;
import com.example.ragbackend.chat.dto.ChatOnceResponse;

public interface ChatService {

    ChatOnceResponse once(ChatOnceRequest request);
}
