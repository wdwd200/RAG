package com.example.ragbackend.chunk.service.impl;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ragbackend.chunk.dto.DocumentChunkResponse;
import com.example.ragbackend.chunk.entity.DocumentChunk;
import com.example.ragbackend.chunk.mapper.DocumentChunkMapper;
import com.example.ragbackend.chunk.service.DocumentChunkService;
import com.example.ragbackend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentChunkServiceImpl implements DocumentChunkService {

    private static final String DOCUMENT_CHUNK_NOT_FOUND_CODE = "DOCUMENT_CHUNK_NOT_FOUND";

    private final DocumentChunkMapper documentChunkMapper;

    @Override
    public DocumentChunkResponse create(DocumentChunk documentChunk) {
        documentChunkMapper.insert(documentChunk);
        return toResponse(getExistingDocumentChunk(documentChunk.getId()));
    }

    @Override
    public DocumentChunkResponse findById(Long id) {
        return toResponse(getExistingDocumentChunk(id));
    }

    @Override
    public List<DocumentChunkResponse> findByDocumentId(Long documentId) {
        LambdaQueryWrapper<DocumentChunk> queryWrapper = new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getDocumentId, documentId)
                .eq(DocumentChunk::getIsActive, true)
                .orderByAsc(DocumentChunk::getChunkIndex)
                .orderByAsc(DocumentChunk::getId);
        return documentChunkMapper.selectList(queryWrapper)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        LambdaQueryWrapper<DocumentChunk> queryWrapper = new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getDocumentId, documentId);
        documentChunkMapper.delete(queryWrapper);
    }

    private DocumentChunk getExistingDocumentChunk(Long id) {
        LambdaQueryWrapper<DocumentChunk> queryWrapper = new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getId, id)
                .eq(DocumentChunk::getIsActive, true);
        DocumentChunk documentChunk = documentChunkMapper.selectOne(queryWrapper);
        if (documentChunk == null) {
            throw new BusinessException(DOCUMENT_CHUNK_NOT_FOUND_CODE, "Document chunk not found: " + id);
        }
        return documentChunk;
    }

    private DocumentChunkResponse toResponse(DocumentChunk documentChunk) {
        return new DocumentChunkResponse(
                documentChunk.getId(),
                documentChunk.getKnowledgeBaseId(),
                documentChunk.getDocumentId(),
                documentChunk.getChunkIndex(),
                documentChunk.getContent(),
                documentChunk.getContentHash(),
                documentChunk.getProcessingVersion(),
                documentChunk.getIsActive(),
                documentChunk.getTokenCount(),
                documentChunk.getVectorId(),
                documentChunk.getPageNumber(),
                documentChunk.getMetadataJson(),
                documentChunk.getCreatedAt()
        );
    }
}
