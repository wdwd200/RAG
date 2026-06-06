package com.example.ragbackend.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ragbackend.audit.dto.RetrievalLogResponse;
import com.example.ragbackend.audit.entity.RetrievalLog;
import com.example.ragbackend.audit.mapper.RetrievalLogMapper;
import com.example.ragbackend.audit.service.RetrievalLogService;
import com.example.ragbackend.common.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RetrievalLogServiceImpl implements RetrievalLogService {

    private static final String RETRIEVAL_LOG_REQUEST_ID_INVALID_CODE =
            "RETRIEVAL_LOG_REQUEST_ID_INVALID";

    private final RetrievalLogMapper retrievalLogMapper;

    @Override
    public void saveLogs(List<RetrievalLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        logs.forEach(retrievalLogMapper::insert);
    }

    @Override
    public List<RetrievalLogResponse> findByRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new BusinessException(
                    RETRIEVAL_LOG_REQUEST_ID_INVALID_CODE,
                    "Retrieval log requestId must not be blank"
            );
        }

        LambdaQueryWrapper<RetrievalLog> queryWrapper = new LambdaQueryWrapper<RetrievalLog>()
                .eq(RetrievalLog::getRequestId, requestId)
                .orderByAsc(RetrievalLog::getRankPosition)
                .orderByAsc(RetrievalLog::getId);
        return retrievalLogMapper.selectList(queryWrapper).stream()
                .map(this::toResponse)
                .toList();
    }

    private RetrievalLogResponse toResponse(RetrievalLog log) {
        return new RetrievalLogResponse(
                log.getId(),
                log.getRequestId(),
                log.getSessionId(),
                log.getMessageId(),
                log.getKnowledgeBaseId(),
                log.getQuestion(),
                log.getRetrieverType(),
                log.getTopK(),
                log.getChunkId(),
                log.getDocumentId(),
                log.getRankPosition(),
                log.getScore(),
                log.getCreatedAt()
        );
    }
}
