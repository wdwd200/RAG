package com.example.ragbackend.evaluation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("evaluation_question_result")
public class EvaluationQuestionResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reportId;

    private Long questionId;

    private String question;

    private String expectedChunkIdsJson;

    private String retrievedChunkIdsJson;

    private Boolean hit;

    private Double reciprocalRank;

    private Double recallAtK;

    private Integer rankedHitPosition;

    private LocalDateTime createdAt;
}
