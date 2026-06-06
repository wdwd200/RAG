package com.example.ragbackend.chat.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ragbackend.common.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;

class RagPromptBuilderTest {

    private final PromptBuilder promptBuilder = new RagPromptBuilder();

    @Test
    void includesQuestionChunkContentConstraintsAndCitationRequirement() {
        RagPromptRequest request = new RagPromptRequest(
                "员工年假可以累计多久？",
                List.of(new PromptContextChunk(
                        11L,
                        22L,
                        "employee-handbook.md",
                        0.91d,
                        "未休年假最多可以累计到下一年度。"
                ))
        );

        String prompt = promptBuilder.build(request);

        assertThat(prompt)
                .contains("员工年假可以累计多久？")
                .contains("未休年假最多可以累计到下一年度。")
                .contains("你只能基于给定上下文回答")
                .contains("根据当前知识库内容无法确定")
                .contains("不要编造知识库外的信息")
                .contains("[片段1]")
                .contains("employee-handbook.md");
    }

    @Test
    void remainsStructuredWhenNoChunksAreAvailable() {
        String prompt = promptBuilder.build(new RagPromptRequest("未知问题", List.of()));

        assertThat(prompt)
                .contains("用户问题：")
                .contains("未知问题")
                .contains("检索到的上下文片段：")
                .contains("未检索到可用上下文片段")
                .contains("根据当前知识库内容无法确定");
    }

    @Test
    void rejectsBlankQuestion() {
        assertThatThrownBy(() -> promptBuilder.build(new RagPromptRequest(" ", List.of())))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("PROMPT_REQUEST_INVALID"));
    }
}
