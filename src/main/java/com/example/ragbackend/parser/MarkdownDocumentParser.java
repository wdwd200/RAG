package com.example.ragbackend.parser;

import org.springframework.stereotype.Component;

@Component
public class MarkdownDocumentParser extends AbstractTextDocumentParser {

    private static final String FILE_TYPE = "md";

    @Override
    public boolean supports(String fileType) {
        return FILE_TYPE.equalsIgnoreCase(fileType);
    }

    @Override
    protected String fileType() {
        return FILE_TYPE;
    }

    @Override
    protected String parserName() {
        return "markdown";
    }
}
