package com.example.ragbackend.document.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("document")
public class Document {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long knowledgeBaseId;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String storagePath;

    private String status;

    private Integer chunkCount;

    private Integer processingVersion;

    private String failedStage;

    private String errorMessage;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
