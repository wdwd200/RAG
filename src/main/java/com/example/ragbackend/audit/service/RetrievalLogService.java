package com.example.ragbackend.audit.service;

import com.example.ragbackend.audit.dto.RetrievalLogResponse;
import com.example.ragbackend.audit.entity.RetrievalLog;
import java.util.List;

public interface RetrievalLogService {

    void saveLogs(List<RetrievalLog> logs);

    List<RetrievalLogResponse> findByRequestId(String requestId);
}
