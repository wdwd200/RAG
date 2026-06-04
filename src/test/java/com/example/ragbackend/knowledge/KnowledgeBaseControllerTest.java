package com.example.ragbackend.knowledge;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KnowledgeBaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanData() {
        jdbcTemplate.execute("DELETE FROM knowledge_base");
    }

    @Test
    void createsKnowledgeBase() throws Exception {
        mockMvc.perform(post("/api/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "API knowledge base",
                                  "description": "Created through API",
                                  "ownerId": 20,
                                  "visibility": "PUBLIC"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.name").value("API knowledge base"))
                .andExpect(jsonPath("$.data.ownerId").value(20))
                .andExpect(jsonPath("$.data.visibility").value("PUBLIC"));
    }

    @Test
    void listsKnowledgeBases() throws Exception {
        createKnowledgeBase("First knowledge base");
        createKnowledgeBase("Second knowledge base");

        mockMvc.perform(get("/api/knowledge-bases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void getsKnowledgeBaseById() throws Exception {
        Long id = createKnowledgeBase("Readable knowledge base");

        mockMvc.perform(get("/api/knowledge-bases/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.name").value("Readable knowledge base"));
    }

    @Test
    void updatesKnowledgeBase() throws Exception {
        Long id = createKnowledgeBase("Old knowledge base");

        mockMvc.perform(put("/api/knowledge-bases/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated knowledge base",
                                  "description": "Updated through API",
                                  "visibility": "PUBLIC"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.name").value("Updated knowledge base"))
                .andExpect(jsonPath("$.data.description").value("Updated through API"))
                .andExpect(jsonPath("$.data.visibility").value("PUBLIC"));
    }

    @Test
    void deletesKnowledgeBase() throws Exception {
        Long id = createKnowledgeBase("Deleted knowledge base");

        mockMvc.perform(delete("/api/knowledge-bases/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"));

        mockMvc.perform(get("/api/knowledge-bases/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_BASE_NOT_FOUND"));
    }

    @Test
    void returnsErrorWhenKnowledgeBaseDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/knowledge-bases/{id}", 999999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_BASE_NOT_FOUND"));
    }

    @Test
    void returnsErrorWhenUpdatingMissingKnowledgeBase() throws Exception {
        mockMvc.perform(put("/api/knowledge-bases/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Missing knowledge base",
                                  "description": "Cannot update missing data",
                                  "visibility": "PRIVATE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_BASE_NOT_FOUND"));
    }

    @Test
    void returnsErrorWhenDeletingMissingKnowledgeBase() throws Exception {
        mockMvc.perform(delete("/api/knowledge-bases/{id}", 999999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_BASE_NOT_FOUND"));
    }

    private Long createKnowledgeBase(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "description": "Created through API",
                                  "ownerId": 1,
                                  "visibility": "PRIVATE"
                                }
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.at("/data/id").asLong();
    }
}
