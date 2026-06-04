package com.example.ragbackend.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.example.ragbackend.knowledge.dto.KnowledgeBaseCreateRequest;
import com.example.ragbackend.knowledge.dto.KnowledgeBaseResponse;
import com.example.ragbackend.knowledge.entity.KnowledgeBase;
import com.example.ragbackend.knowledge.mapper.KnowledgeBaseMapper;
import com.example.ragbackend.knowledge.service.KnowledgeBaseService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class KnowledgeBasePersistenceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @BeforeEach
    void cleanData() {
        jdbcTemplate.execute("DELETE FROM knowledge_base");
    }

    @Test
    void flywayCreatesKnowledgeBaseTable() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'knowledge_base'",
                Integer.class
        );

        assertThat(tableCount).isEqualTo(1);
    }

    @Test
    void mapperInsertsAndFindsKnowledgeBase() {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setName("Mapper knowledge base");
        knowledgeBase.setDescription("Created by mapper test");
        knowledgeBase.setOwnerId(10L);
        knowledgeBase.setVisibility("PRIVATE");

        int inserted = knowledgeBaseMapper.insert(knowledgeBase);
        KnowledgeBase found = knowledgeBaseMapper.selectById(knowledgeBase.getId());

        assertThat(inserted).isEqualTo(1);
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Mapper knowledge base");
        assertThat(found.getOwnerId()).isEqualTo(10L);
        assertThat(found.getDocumentCount()).isZero();
        assertThat(found.getChunkCount()).isZero();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void serviceCreatesFindsAndChecksKnowledgeBase() {
        KnowledgeBaseCreateRequest request = new KnowledgeBaseCreateRequest(
                "Service knowledge base",
                "Created by service test",
                null,
                null
        );

        KnowledgeBaseResponse created = knowledgeBaseService.create(request);
        KnowledgeBaseResponse found = knowledgeBaseService.findById(created.id()).orElseThrow();
        List<KnowledgeBaseResponse> all = knowledgeBaseService.findAll();

        assertThat(created.id()).isNotNull();
        assertThat(found.name()).isEqualTo("Service knowledge base");
        assertThat(found.ownerId()).isEqualTo(1L);
        assertThat(found.visibility()).isEqualTo("PRIVATE");
        assertThat(knowledgeBaseService.existsById(created.id())).isTrue();
        assertThat(knowledgeBaseService.existsById(-1L)).isFalse();
        assertThat(all).extracting(KnowledgeBaseResponse::id).contains(created.id());
    }
}
