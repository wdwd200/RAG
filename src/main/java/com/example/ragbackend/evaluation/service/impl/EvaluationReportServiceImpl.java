package com.example.ragbackend.evaluation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.evaluation.dto.EvaluationQuestionResultCreateRequest;
import com.example.ragbackend.evaluation.dto.EvaluationQuestionResultResponse;
import com.example.ragbackend.evaluation.dto.EvaluationReportResponse;
import com.example.ragbackend.evaluation.entity.EvaluationQuestionResult;
import com.example.ragbackend.evaluation.entity.EvaluationReport;
import com.example.ragbackend.evaluation.mapper.EvaluationQuestionResultMapper;
import com.example.ragbackend.evaluation.mapper.EvaluationReportMapper;
import com.example.ragbackend.evaluation.metric.RetrievalMetricResult;
import com.example.ragbackend.evaluation.service.EvaluationReportService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EvaluationReportServiceImpl implements EvaluationReportService {

    private static final String REPORT_NOT_FOUND_CODE = "EVALUATION_REPORT_NOT_FOUND";
    private static final String REPORT_JSON_INVALID_CODE = "EVALUATION_REPORT_JSON_INVALID";

    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    private final EvaluationReportMapper evaluationReportMapper;
    private final EvaluationQuestionResultMapper evaluationQuestionResultMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public EvaluationReportResponse createRunningReport(
            Long datasetId,
            Long knowledgeBaseId,
            Integer topK,
            Integer questionCount
    ) {
        EvaluationReport report = new EvaluationReport();
        report.setDatasetId(datasetId);
        report.setKnowledgeBaseId(knowledgeBaseId);
        report.setTopK(topK);
        report.setQuestionCount(questionCount);
        report.setRecallAtK(0.0d);
        report.setHitRateAtK(0.0d);
        report.setMrr(0.0d);
        report.setStatus(STATUS_RUNNING);

        evaluationReportMapper.insert(report);

        return toResponse(getExistingReport(report.getId()));
    }

    @Override
    @Transactional
    public EvaluationReportResponse markSuccess(Long reportId, RetrievalMetricResult metricResult) {
        EvaluationReport report = getExistingReport(reportId);
        report.setQuestionCount(metricResult.questionCount());
        report.setRecallAtK(metricResult.recallAtK());
        report.setHitRateAtK(metricResult.hitRateAtK());
        report.setMrr(metricResult.mrr());
        report.setStatus(STATUS_SUCCESS);
        report.setErrorMessage(null);
        report.setFinishedAt(LocalDateTime.now());
        evaluationReportMapper.updateById(report);

        return toResponse(getExistingReport(reportId));
    }

    @Override
    @Transactional
    public EvaluationReportResponse markFailed(Long reportId, String errorMessage) {
        EvaluationReport report = getExistingReport(reportId);
        report.setStatus(STATUS_FAILED);
        report.setErrorMessage(errorMessage);
        report.setFinishedAt(LocalDateTime.now());
        evaluationReportMapper.updateById(report);

        return toResponse(getExistingReport(reportId));
    }

    @Override
    @Transactional
    public void saveQuestionResults(Long reportId, List<EvaluationQuestionResultCreateRequest> requests) {
        getExistingReport(reportId);
        if (requests == null || requests.isEmpty()) {
            return;
        }

        for (EvaluationQuestionResultCreateRequest request : requests) {
            EvaluationQuestionResult result = new EvaluationQuestionResult();
            result.setReportId(reportId);
            result.setQuestionId(request.questionId());
            result.setQuestion(request.question());
            result.setExpectedChunkIdsJson(writeJson(request.expectedChunkIds()));
            result.setRetrievedChunkIdsJson(writeJson(request.retrievedChunkIds()));
            result.setHit(request.hit());
            result.setReciprocalRank(request.reciprocalRank());
            result.setRecallAtK(request.recallAtK());
            result.setRankedHitPosition(request.rankedHitPosition());
            evaluationQuestionResultMapper.insert(result);
        }
    }

    @Override
    public EvaluationReportResponse getById(Long id) {
        return toResponse(getExistingReport(id));
    }

    @Override
    public List<EvaluationQuestionResultResponse> findQuestionResults(Long reportId) {
        getExistingReport(reportId);
        LambdaQueryWrapper<EvaluationQuestionResult> queryWrapper =
                new LambdaQueryWrapper<EvaluationQuestionResult>()
                        .eq(EvaluationQuestionResult::getReportId, reportId)
                        .orderByAsc(EvaluationQuestionResult::getCreatedAt)
                        .orderByAsc(EvaluationQuestionResult::getId);
        return evaluationQuestionResultMapper.selectList(queryWrapper)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private EvaluationReport getExistingReport(Long id) {
        EvaluationReport report = evaluationReportMapper.selectById(id);
        if (report == null) {
            throw new BusinessException(REPORT_NOT_FOUND_CODE, "Evaluation report not found: " + id);
        }
        return report;
    }

    private EvaluationReportResponse toResponse(EvaluationReport report) {
        return new EvaluationReportResponse(
                report.getId(),
                report.getDatasetId(),
                report.getKnowledgeBaseId(),
                report.getTopK(),
                report.getQuestionCount(),
                report.getRecallAtK(),
                report.getHitRateAtK(),
                report.getMrr(),
                report.getStatus(),
                report.getErrorMessage(),
                report.getCreatedAt(),
                report.getFinishedAt(),
                countQuestionResults(report.getId())
        );
    }

    private EvaluationQuestionResultResponse toResponse(EvaluationQuestionResult result) {
        return new EvaluationQuestionResultResponse(
                result.getId(),
                result.getReportId(),
                result.getQuestionId(),
                result.getQuestion(),
                readLongList(result.getExpectedChunkIdsJson()),
                readLongList(result.getRetrievedChunkIdsJson()),
                result.getHit(),
                result.getReciprocalRank(),
                result.getRecallAtK(),
                result.getRankedHitPosition(),
                result.getCreatedAt()
        );
    }

    private Integer countQuestionResults(Long reportId) {
        LambdaQueryWrapper<EvaluationQuestionResult> queryWrapper =
                new LambdaQueryWrapper<EvaluationQuestionResult>()
                        .eq(EvaluationQuestionResult::getReportId, reportId);
        return Math.toIntExact(evaluationQuestionResultMapper.selectCount(queryWrapper));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(REPORT_JSON_INVALID_CODE, "Failed to serialize evaluation report data");
        }
    }

    private List<Long> readLongList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new BusinessException(REPORT_JSON_INVALID_CODE, "Failed to parse evaluation report data");
        }
    }
}
