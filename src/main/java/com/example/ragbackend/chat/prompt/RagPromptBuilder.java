package com.example.ragbackend.chat.prompt;

import com.example.ragbackend.common.exception.BusinessException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RagPromptBuilder implements PromptBuilder {

    private static final String PROMPT_REQUEST_INVALID_CODE = "PROMPT_REQUEST_INVALID";

    @Override
    public String build(RagPromptRequest request) {
        validateRequest(request);
        List<PromptContextChunk> chunks = request.chunks() == null ? List.of() : request.chunks();

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是知识库问答助手。\n\n");
        prompt.append("回答约束：\n");
        prompt.append("1. 你只能基于给定上下文回答。\n");
        prompt.append("2. 如果上下文不足以回答，请说明“根据当前知识库内容无法确定”。\n");
        prompt.append("3. 不要编造知识库外的信息。\n");
        prompt.append("4. 回答时使用 [片段N] 标注引用来源。\n\n");
        prompt.append("用户问题：\n");
        prompt.append(request.question().trim()).append("\n\n");
        prompt.append("检索到的上下文片段：\n");

        if (chunks.isEmpty()) {
            prompt.append("（未检索到可用上下文片段）\n");
        } else {
            appendChunks(prompt, chunks);
        }

        prompt.append("\n请基于以上上下文回答用户问题。");
        return prompt.toString();
    }

    private void validateRequest(RagPromptRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new BusinessException(PROMPT_REQUEST_INVALID_CODE, "Prompt question must not be empty");
        }
    }

    private void appendChunks(StringBuilder prompt, List<PromptContextChunk> chunks) {
        for (int i = 0; i < chunks.size(); i++) {
            PromptContextChunk chunk = chunks.get(i);
            prompt.append("[片段").append(i + 1).append("]\n");
            prompt.append("chunkId: ").append(chunk.chunkId()).append("\n");
            prompt.append("documentId: ").append(chunk.documentId()).append("\n");
            if (chunk.fileName() != null && !chunk.fileName().isBlank()) {
                prompt.append("fileName: ").append(chunk.fileName()).append("\n");
            }
            if (chunk.score() != null) {
                prompt.append("score: ").append(chunk.score()).append("\n");
            }
            prompt.append("content: ").append(chunk.content() == null ? "" : chunk.content()).append("\n\n");
        }
    }
}
