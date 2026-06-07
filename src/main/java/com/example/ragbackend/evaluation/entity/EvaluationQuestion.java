package com.example.ragbackend.evaluation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("evaluation_question")
public class EvaluationQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long datasetId;

    private String question;

    private String groundTruthAnswer;

    private String relevantChunkIdsJson;

    private String relevantContentHashesJson;

    private Integer documentProcessingVersion;

    private String questionType;

    private LocalDateTime createdAt;
}
