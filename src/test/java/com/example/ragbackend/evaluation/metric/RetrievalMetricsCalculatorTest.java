package com.example.ragbackend.evaluation.metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ragbackend.common.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;

class RetrievalMetricsCalculatorTest {

    private final RetrievalMetricsCalculator calculator = new RetrievalMetricsCalculator();

    @Test
    void calculatesQuestionRecallAtK() {
        QuestionMetricResult result = calculator.calculateQuestion(1L, List.of(10L, 20L), List.of(30L, 10L, 40L));

        assertThat(result.recallAtK()).isEqualTo(0.5d);
        assertThat(result.hit()).isTrue();
        assertThat(result.rankedHitPosition()).isEqualTo(2);
    }

    @Test
    void calculatesHitRateAtK() {
        RetrievalMetricResult result = calculator.summarize(List.of(
                calculator.calculateQuestion(1L, List.of(10L), List.of(10L, 20L)),
                calculator.calculateQuestion(2L, List.of(30L), List.of(40L, 50L))
        ));

        assertThat(result.hitRateAtK()).isEqualTo(0.5d);
    }

    @Test
    void calculatesMrr() {
        RetrievalMetricResult result = calculator.summarize(List.of(
                calculator.calculateQuestion(1L, List.of(10L), List.of(30L, 10L)),
                calculator.calculateQuestion(2L, List.of(20L), List.of(20L, 40L))
        ));

        assertThat(result.mrr()).isEqualTo(0.75d);
    }

    @Test
    void calculatesAverageMetricsForMultipleQuestions() {
        RetrievalMetricResult result = calculator.summarize(List.of(
                calculator.calculateQuestion(1L, List.of(10L, 20L), List.of(10L, 30L)),
                calculator.calculateQuestion(2L, List.of(40L), List.of(50L, 40L)),
                calculator.calculateQuestion(3L, List.of(60L), List.of(70L, 80L))
        ));

        assertThat(result.questionCount()).isEqualTo(3);
        assertThat(result.recallAtK()).isEqualTo((0.5d + 1.0d + 0.0d) / 3.0d);
        assertThat(result.hitRateAtK()).isEqualTo(2.0d / 3.0d);
        assertThat(result.mrr()).isEqualTo((1.0d + 0.5d + 0.0d) / 3.0d);
    }

    @Test
    void returnsZeroMetricsWhenNoQuestionHits() {
        RetrievalMetricResult result = calculator.summarize(List.of(
                calculator.calculateQuestion(1L, List.of(10L), List.of(20L, 30L)),
                calculator.calculateQuestion(2L, List.of(40L), List.of(50L, 60L))
        ));

        assertThat(result.recallAtK()).isZero();
        assertThat(result.hitRateAtK()).isZero();
        assertThat(result.mrr()).isZero();
    }

    @Test
    void rejectsEmptyQuestionResults() {
        assertThatThrownBy(() -> calculator.summarize(List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Question metric results cannot be empty");
    }

    @Test
    void rejectsEmptyExpectedChunkIds() {
        assertThatThrownBy(() -> calculator.calculateQuestion(1L, List.of(), List.of(10L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Expected chunk ids cannot be empty");
    }
}
