package com.example.ragbackend.splitter;

import com.example.ragbackend.common.exception.BusinessException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FixedWindowTextSplitter implements TextSplitter {

    private static final String INVALID_CHUNK_OPTIONS_CODE = "INVALID_CHUNK_OPTIONS";

    @Override
    public List<TextChunk> split(String text, SplitOptions options) {
        validateOptions(options);
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<TextChunk> chunks = new ArrayList<>();
        int step = options.chunkSize() - options.overlap();
        int start = 0;
        int chunkIndex = 0;

        while (start < text.length()) {
            int end = Math.min(start + options.chunkSize(), text.length());
            String content = text.substring(start, end);
            if (!content.isBlank()) {
                chunks.add(new TextChunk(chunkIndex, content, estimateTokenCount(content)));
                chunkIndex++;
            }
            if (end == text.length()) {
                break;
            }
            start += step;
        }

        return chunks;
    }

    private void validateOptions(SplitOptions options) {
        if (options == null) {
            throw new BusinessException(INVALID_CHUNK_OPTIONS_CODE, "Split options must not be null");
        }
        if (options.chunkSize() <= 0) {
            throw new BusinessException(INVALID_CHUNK_OPTIONS_CODE, "Chunk size must be greater than 0");
        }
        if (options.overlap() < 0) {
            throw new BusinessException(INVALID_CHUNK_OPTIONS_CODE, "Chunk overlap must not be negative");
        }
        if (options.overlap() >= options.chunkSize()) {
            throw new BusinessException(
                    INVALID_CHUNK_OPTIONS_CODE,
                    "Chunk overlap must be smaller than chunk size"
            );
        }
    }

    private int estimateTokenCount(String content) {
        String trimmed = content.trim();
        if (trimmed.isEmpty()) {
            return 0;
        }
        return trimmed.split("\\s+").length;
    }
}
