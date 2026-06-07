package com.example.ragbackend.evaluation.service;

import com.example.ragbackend.evaluation.dto.EvaluationBadCaseResponse;
import com.example.ragbackend.evaluation.dto.EvaluationReportSummaryResponse;
import java.util.List;

public interface EvaluationAnalysisService {

    List<EvaluationBadCaseResponse> findBadCases(Long reportId);

    EvaluationReportSummaryResponse getSummary(Long reportId);
}
