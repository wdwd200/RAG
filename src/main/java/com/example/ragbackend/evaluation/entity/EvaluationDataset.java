package com.example.ragbackend.evaluation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("evaluation_dataset")
public class EvaluationDataset {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Long knowledgeBaseId;

    private String description;

    private Integer questionCount;

    private LocalDateTime createdAt;
}
