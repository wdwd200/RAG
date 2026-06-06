package com.example.ragbackend.chat.service;

import com.example.ragbackend.chat.dto.ChatReferenceResponse;
import java.util.List;

public interface ChatProgressListener {

    default void onRetrievalStart(String requestId) {
    }

    default void onRetrievalResult(
            String requestId,
            List<ChatReferenceResponse> references) {
    }

    static ChatProgressListener noOp() {
        return new ChatProgressListener() {
        };
    }
}
