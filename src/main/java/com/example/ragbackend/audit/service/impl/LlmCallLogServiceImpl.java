package com.example.ragbackend.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ragbackend.audit.dto.LlmCallLogResponse;
import com.example.ragbackend.audit.entity.LlmCallLog;
import com.example.ragbackend.audit.mapper.LlmCallLogMapper;
import com.example.ragbackend.audit.service.LlmCallLogService;
import com.example.ragbackend.common.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LlmCallLogServiceImpl implements LlmCallLogService {

    private static final String LLM_CALL_LOG_REQUEST_ID_INVALID_CODE =
            "LLM_CALL_LOG_REQUEST_ID_INVALID";

    private final LlmCallLogMapper llmCallLogMapper;

    @Override
    @Transactional
    public LlmCallLog save(LlmCallLog log) {
        return insert(log);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LlmCallLog saveInNewTransaction(LlmCallLog log) {
        return insert(log);
    }

    private LlmCallLog insert(LlmCallLog log) {
        llmCallLogMapper.insert(log);
        return llmCallLogMapper.selectById(log.getId());
    }

    @Override
    public List<LlmCallLogResponse> findByRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new BusinessException(
                    LLM_CALL_LOG_REQUEST_ID_INVALID_CODE,
                    "LLM call log requestId must not be blank"
            );
        }

        LambdaQueryWrapper<LlmCallLog> queryWrapper = new LambdaQueryWrapper<LlmCallLog>()
                .eq(LlmCallLog::getRequestId, requestId)
                .orderByAsc(LlmCallLog::getCreatedAt)
                .orderByAsc(LlmCallLog::getId);
        return llmCallLogMapper.selectList(queryWrapper).stream()
                .map(this::toResponse)
                .toList();
    }

    private LlmCallLogResponse toResponse(LlmCallLog log) {
        return new LlmCallLogResponse(
                log.getId(),
                log.getRequestId(),
                log.getSessionId(),
                log.getMessageId(),
                log.getKnowledgeBaseId(),
                log.getProvider(),
                log.getModelName(),
                log.getPromptTokens(),
                log.getCompletionTokens(),
                log.getLatencyMs(),
                log.getSuccess(),
                log.getErrorMessage(),
                log.getCreatedAt()
        );
    }
}
