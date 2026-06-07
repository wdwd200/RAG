package com.example.ragbackend.evaluation.service.impl;

import com.example.ragbackend.evaluation.dto.EvaluationBadCaseResponse;
import com.example.ragbackend.evaluation.dto.EvaluationQuestionResultResponse;
import com.example.ragbackend.evaluation.dto.EvaluationReportResponse;
import com.example.ragbackend.evaluation.dto.EvaluationReportSummaryResponse;
import com.example.ragbackend.evaluation.service.EvaluationAnalysisService;
import com.example.ragbackend.evaluation.service.EvaluationReportService;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EvaluationAnalysisServiceImpl implements EvaluationAnalysisService {

    static final String FAILURE_REASON_NO_HIT = "NO_HIT";
    static final String FAILURE_REASON_LOW_RECALL = "LOW_RECALL";
    static final String FAILURE_REASON_LOW_RANK = "LOW_RANK";

    private final EvaluationReportService evaluationReportService;

    @Override
    public List<EvaluationBadCaseResponse> findBadCases(Long reportId) {
        evaluationReportService.getById(reportId);
        return evaluationReportService.findQuestionResults(reportId)
                .stream()
                .map(this::toBadCaseOrNull)
                .filter(badCase -> badCase != null)
                .sorted(badCaseComparator())
                .toList();
    }

    @Override
    public EvaluationReportSummaryResponse getSummary(Long reportId) {
        EvaluationReportResponse report = evaluationReportService.getById(reportId);
        List<EvaluationBadCaseResponse> badCases = findBadCases(reportId);

        int noHitCount = countByReason(badCases, FAILURE_REASON_NO_HIT);
        int lowRecallCount = countByReason(badCases, FAILURE_REASON_LOW_RECALL);
        int lowRankCount = countByReason(badCases, FAILURE_REASON_LOW_RANK);

        return new EvaluationReportSummaryResponse(
                report.id(),
                report.datasetId(),
                report.knowledgeBaseId(),
                report.topK(),
                report.questionCount(),
                report.recallAtK(),
                report.hitRateAtK(),
                report.mrr(),
                badCases.size(),
                noHitCount,
                lowRecallCount,
                lowRankCount,
                report.status(),
                report.createdAt(),
                report.finishedAt()
        );
    }

    private EvaluationBadCaseResponse toBadCaseOrNull(EvaluationQuestionResultResponse result) {
        String failureReason = classifyFailureReason(result);
        if (failureReason == null) {
            return null;
        }

        return new EvaluationBadCaseResponse(
                result.questionId(),
                result.question(),
                result.expectedChunkIds(),
                result.retrievedChunkIds(),
                result.hit(),
                result.recallAtK(),
                result.reciprocalRank(),
                result.rankedHitPosition(),
                failureReason
        );
    }

    private String classifyFailureReason(EvaluationQuestionResultResponse result) {
        if (!Boolean.TRUE.equals(result.hit())) {
            return FAILURE_REASON_NO_HIT;
        }
        if (result.recallAtK() != null && result.recallAtK() < 1.0d) {
            return FAILURE_REASON_LOW_RECALL;
        }
        if (result.rankedHitPosition() != null && result.rankedHitPosition() > 1) {
            return FAILURE_REASON_LOW_RANK;
        }
        return null;
    }

    private Comparator<EvaluationBadCaseResponse> badCaseComparator() {
        return Comparator
                .comparingInt((EvaluationBadCaseResponse badCase) -> severityRank(badCase.failureReason()))
                .thenComparing(EvaluationBadCaseResponse::recallAtK, Comparator.nullsLast(Double::compareTo))
                .thenComparing(
                        EvaluationBadCaseResponse::rankedHitPosition,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
                .thenComparing(EvaluationBadCaseResponse::questionId);
    }

    private int severityRank(String failureReason) {
        return switch (failureReason) {
            case FAILURE_REASON_NO_HIT -> 0;
            case FAILURE_REASON_LOW_RECALL -> 1;
            case FAILURE_REASON_LOW_RANK -> 2;
            default -> 3;
        };
    }

    private int countByReason(List<EvaluationBadCaseResponse> badCases, String failureReason) {
        return Math.toIntExact(badCases.stream()
                .filter(badCase -> failureReason.equals(badCase.failureReason()))
                .count());
    }
}
