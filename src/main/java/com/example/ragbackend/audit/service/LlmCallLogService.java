package com.example.ragbackend.audit.service;

import com.example.ragbackend.audit.dto.LlmCallLogResponse;
import com.example.ragbackend.audit.entity.LlmCallLog;
import java.util.List;

public interface LlmCallLogService {

    LlmCallLog save(LlmCallLog log);

    LlmCallLog saveInNewTransaction(LlmCallLog log);

    List<LlmCallLogResponse> findByRequestId(String requestId);
}
