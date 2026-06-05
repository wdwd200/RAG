package com.example.ragbackend.parser;

import java.nio.file.Path;

public interface DocumentParser {

    boolean supports(String fileType);

    ParsedDocument parse(Path filePath);
}
