package com.example.ragbackend.parser;

import java.util.Map;

public record ParsedDocument(
        String fileName,
        String fileType,
        String content,
        Map<String, String> metadata
) {
}
