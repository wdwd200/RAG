package com.example.ragbackend.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("llm_call_log")
public class LlmCallLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String requestId;

    private Long sessionId;

    private Long messageId;

    private Long knowledgeBaseId;

    private String provider;

    private String modelName;

    private Integer promptTokens;

    private Integer completionTokens;

    private Long latencyMs;

    private Boolean success;

    private String errorMessage;

    private LocalDateTime createdAt;
}
