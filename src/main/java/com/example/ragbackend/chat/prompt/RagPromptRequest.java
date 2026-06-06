package com.example.ragbackend.chat.prompt;

import java.util.List;

public record RagPromptRequest(
        String question,
        List<PromptContextChunk> chunks
) {
}
