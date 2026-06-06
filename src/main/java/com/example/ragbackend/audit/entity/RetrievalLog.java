package com.example.ragbackend.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("retrieval_log")
public class RetrievalLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String requestId;

    private Long sessionId;

    private Long messageId;

    private Long knowledgeBaseId;

    private String question;

    private String retrieverType;

    private Integer topK;

    private Long chunkId;

    private Long documentId;

    private Integer rankPosition;

    private Double score;

    private LocalDateTime createdAt;
}
