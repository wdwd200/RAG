package com.example.ragbackend.chunk.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("document_chunk")
public class DocumentChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long knowledgeBaseId;

    private Long documentId;

    private Integer chunkIndex;

    private String content;

    private String contentHash;

    private Integer processingVersion;

    private Boolean isActive;

    private Integer tokenCount;

    private String vectorId;

    private Integer pageNumber;

    private String metadataJson;

    private LocalDateTime createdAt;
}
