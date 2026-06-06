package com.example.ragbackend.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ragbackend.chat.entity.ChatMessage;
import com.example.ragbackend.chat.entity.ChatSession;
import com.example.ragbackend.chat.enums.ChatMessageRole;
import com.example.ragbackend.chat.service.ChatMessageService;
import com.example.ragbackend.chat.service.ChatSessionService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ChatPersistenceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private ChatMessageService chatMessageService;

    @BeforeEach
    void cleanData() {
        jdbcTemplate.execute("DELETE FROM chat_message");
        jdbcTemplate.execute("DELETE FROM chat_session");
    }

    @Test
    void flywayCreatesChatTables() {
        Integer sessionTableCount = tableCount("chat_session");
        Integer messageTableCount = tableCount("chat_message");

        assertThat(sessionTableCount).isEqualTo(1);
        assertThat(messageTableCount).isEqualTo(1);
    }

    @Test
    void servicesPersistSessionMessagesAndReferencesJson() {
        ChatSession session = chatSessionService.create(10L, 1L, "Test session");
        ChatMessage userMessage = chatMessageService.create(
                session.getId(),
                ChatMessageRole.USER,
                "What is RAG?",
                null
        );
        ChatMessage assistantMessage = chatMessageService.create(
                session.getId(),
                ChatMessageRole.ASSISTANT,
                "Mock answer",
                "[{\"chunkId\":1}]"
        );

        List<ChatMessage> messages = chatMessageService.findBySessionId(session.getId());

        assertThat(session.getId()).isNotNull();
        assertThat(session.getKnowledgeBaseId()).isEqualTo(10L);
        assertThat(userMessage.getRole()).isEqualTo("USER");
        assertThat(assistantMessage.getRole()).isEqualTo("ASSISTANT");
        assertThat(messages).hasSize(2);
        assertThat(messages.get(1).getReferencesJson()).isEqualTo("[{\"chunkId\":1}]");
    }

    private Integer tableCount(String tableName) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?",
                Integer.class,
                tableName
        );
    }
}
