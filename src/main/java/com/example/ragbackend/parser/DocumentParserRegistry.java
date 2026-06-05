package com.example.ragbackend.parser;

import java.nio.file.Path;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentParserRegistry {

    private final List<DocumentParser> parsers;

    public ParsedDocument parse(String fileType, Path filePath) {
        return findParser(fileType).parse(filePath);
    }

    public DocumentParser findParser(String fileType) {
        return parsers.stream()
                .filter(parser -> parser.supports(fileType))
                .findFirst()
                .orElseThrow(() -> new UnsupportedDocumentTypeException(fileType));
    }
}
