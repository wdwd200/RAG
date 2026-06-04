package com.example.ragbackend.knowledge.service.impl;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.knowledge.dto.KnowledgeBaseCreateRequest;
import com.example.ragbackend.knowledge.dto.KnowledgeBaseResponse;
import com.example.ragbackend.knowledge.dto.KnowledgeBaseUpdateRequest;
import com.example.ragbackend.knowledge.entity.KnowledgeBase;
import com.example.ragbackend.knowledge.mapper.KnowledgeBaseMapper;
import com.example.ragbackend.knowledge.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final long DEFAULT_OWNER_ID = 1L;
    private static final String DEFAULT_VISIBILITY = "PRIVATE";
    private static final String NOT_FOUND_CODE = "KNOWLEDGE_BASE_NOT_FOUND";

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    @Override
    public KnowledgeBaseResponse create(KnowledgeBaseCreateRequest request) {
        Long ownerId = request.ownerId() == null ? DEFAULT_OWNER_ID : request.ownerId();
        String visibility = normalizeVisibility(request.visibility());

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setName(request.name());
        knowledgeBase.setDescription(request.description());
        knowledgeBase.setOwnerId(ownerId);
        knowledgeBase.setVisibility(visibility);

        knowledgeBaseMapper.insert(knowledgeBase);

        return toResponse(knowledgeBaseMapper.selectById(knowledgeBase.getId()));
    }

    @Override
    public KnowledgeBaseResponse getById(Long id) {
        return toResponse(getExistingKnowledgeBase(id));
    }

    @Override
    public Optional<KnowledgeBaseResponse> findById(Long id) {
        return Optional.ofNullable(knowledgeBaseMapper.selectById(id))
                .map(this::toResponse);
    }

    @Override
    public List<KnowledgeBaseResponse> findAll() {
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<KnowledgeBase>()
                .orderByDesc(KnowledgeBase::getCreatedAt)
                .orderByDesc(KnowledgeBase::getId);
        return knowledgeBaseMapper.selectList(queryWrapper)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public KnowledgeBaseResponse update(Long id, KnowledgeBaseUpdateRequest request) {
        KnowledgeBase knowledgeBase = getExistingKnowledgeBase(id);
        knowledgeBase.setName(request.name());
        knowledgeBase.setDescription(request.description());
        knowledgeBase.setVisibility(normalizeVisibility(request.visibility()));

        knowledgeBaseMapper.updateById(knowledgeBase);

        return toResponse(getExistingKnowledgeBase(id));
    }

    @Override
    public void deleteById(Long id) {
        getExistingKnowledgeBase(id);
        knowledgeBaseMapper.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return knowledgeBaseMapper.selectById(id) != null;
    }

    private String normalizeVisibility(String visibility) {
        if (visibility == null || visibility.isBlank()) {
            return DEFAULT_VISIBILITY;
        }
        return visibility;
    }

    private KnowledgeBase getExistingKnowledgeBase(Long id) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(id);
        if (knowledgeBase == null) {
            throw new BusinessException(NOT_FOUND_CODE, "Knowledge base not found: " + id);
        }
        return knowledgeBase;
    }

    private KnowledgeBaseResponse toResponse(KnowledgeBase knowledgeBase) {
        return new KnowledgeBaseResponse(
                knowledgeBase.getId(),
                knowledgeBase.getName(),
                knowledgeBase.getDescription(),
                knowledgeBase.getOwnerId(),
                knowledgeBase.getVisibility(),
                knowledgeBase.getDocumentCount(),
                knowledgeBase.getChunkCount(),
                knowledgeBase.getCreatedAt(),
                knowledgeBase.getUpdatedAt()
        );
    }
}
