package com.example.ragbackend.parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.example.ragbackend.common.exception.BusinessException;

abstract class AbstractTextDocumentParser implements DocumentParser {

    private static final String PARSER_FILE_NOT_FOUND_CODE = "PARSER_FILE_NOT_FOUND";
    private static final String PARSER_EMPTY_FILE_CODE = "PARSER_EMPTY_FILE";
    private static final String PARSER_READ_FAILED_CODE = "PARSER_READ_FAILED";

    @Override
    public ParsedDocument parse(Path filePath) {
        validateReadableFile(filePath);

        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            if (content.isBlank()) {
                throw new BusinessException(PARSER_EMPTY_FILE_CODE, "Parsed file must not be empty");
            }
            return new ParsedDocument(
                    filePath.getFileName().toString(),
                    fileType(),
                    content,
                    Map.of("parser", parserName())
            );
        } catch (IOException ex) {
            throw new BusinessException(PARSER_READ_FAILED_CODE, "Failed to read parsed file");
        }
    }

    private void validateReadableFile(Path filePath) {
        if (filePath == null || !Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new BusinessException(PARSER_FILE_NOT_FOUND_CODE, "Parsed file does not exist");
        }
    }

    protected abstract String fileType();

    protected abstract String parserName();
}
