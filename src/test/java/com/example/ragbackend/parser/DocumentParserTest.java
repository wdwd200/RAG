package com.example.ragbackend.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ragbackend.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DocumentParserTest {

    @Autowired
    private DocumentParserRegistry documentParserRegistry;

    @TempDir
    private Path tempDir;

    @Test
    void parsesTxtFileAsUtf8Text() throws Exception {
        Path filePath = tempDir.resolve("note.txt");
        Files.writeString(filePath, "hello txt", StandardCharsets.UTF_8);

        ParsedDocument parsedDocument = documentParserRegistry.parse("txt", filePath);

        assertThat(parsedDocument.fileName()).isEqualTo("note.txt");
        assertThat(parsedDocument.fileType()).isEqualTo("txt");
        assertThat(parsedDocument.content()).isEqualTo("hello txt");
        assertThat(parsedDocument.metadata()).containsEntry("parser", "txt");
    }

    @Test
    void parsesMarkdownFileAsPlainUtf8Text() throws Exception {
        Path filePath = tempDir.resolve("guide.md");
        Files.writeString(filePath, "# Title\n\nmarkdown body", StandardCharsets.UTF_8);

        ParsedDocument parsedDocument = documentParserRegistry.parse("md", filePath);

        assertThat(parsedDocument.fileName()).isEqualTo("guide.md");
        assertThat(parsedDocument.fileType()).isEqualTo("md");
        assertThat(parsedDocument.content()).isEqualTo("# Title\n\nmarkdown body");
        assertThat(parsedDocument.metadata()).containsEntry("parser", "markdown");
    }

    @Test
    void unsupportedFileTypeFailsWithClearBusinessError() {
        Path filePath = tempDir.resolve("guide.pdf");

        assertThatThrownBy(() -> documentParserRegistry.parse("pdf", filePath))
                .isInstanceOfSatisfying(UnsupportedDocumentTypeException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("UNSUPPORTED_DOCUMENT_TYPE"));
    }

    @Test
    void missingFileFailsWithClearBusinessError() {
        Path missingFile = tempDir.resolve("missing.txt");

        assertThatThrownBy(() -> documentParserRegistry.parse("txt", missingFile))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("PARSER_FILE_NOT_FOUND"));
    }

    @Test
    void emptyFileFailsWithClearBusinessError() throws Exception {
        Path filePath = tempDir.resolve("empty.txt");
        Files.writeString(filePath, "", StandardCharsets.UTF_8);

        assertThatThrownBy(() -> documentParserRegistry.parse("txt", filePath))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("PARSER_EMPTY_FILE"));
    }
}
