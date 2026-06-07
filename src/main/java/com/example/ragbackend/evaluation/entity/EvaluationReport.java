package com.example.ragbackend.evaluation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("evaluation_report")
public class EvaluationReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long datasetId;

    private Long knowledgeBaseId;

    private Integer topK;

    private Integer questionCount;

    private Double recallAtK;

    private Double hitRateAtK;

    private Double mrr;

    private String status;

    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime finishedAt;
}
