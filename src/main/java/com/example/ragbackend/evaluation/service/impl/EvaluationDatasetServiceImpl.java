package com.example.ragbackend.evaluation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.evaluation.dto.EvaluationDatasetCreateRequest;
import com.example.ragbackend.evaluation.dto.EvaluationDatasetResponse;
import com.example.ragbackend.evaluation.entity.EvaluationDataset;
import com.example.ragbackend.evaluation.mapper.EvaluationDatasetMapper;
import com.example.ragbackend.evaluation.service.EvaluationDatasetService;
import com.example.ragbackend.knowledge.service.KnowledgeBaseService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EvaluationDatasetServiceImpl implements EvaluationDatasetService {

    private static final String DATASET_NOT_FOUND_CODE = "EVALUATION_DATASET_NOT_FOUND";
    private static final String KNOWLEDGE_BASE_NOT_FOUND_CODE = "KNOWLEDGE_BASE_NOT_FOUND";

    private final EvaluationDatasetMapper evaluationDatasetMapper;
    private final KnowledgeBaseService knowledgeBaseService;

    @Override
    public EvaluationDatasetResponse create(EvaluationDatasetCreateRequest request) {
        ensureKnowledgeBaseExists(request.knowledgeBaseId());

        EvaluationDataset dataset = new EvaluationDataset();
        dataset.setName(request.name());
        dataset.setKnowledgeBaseId(request.knowledgeBaseId());
        dataset.setDescription(request.description());

        evaluationDatasetMapper.insert(dataset);

        return toResponse(getExistingDataset(dataset.getId()));
    }

    @Override
    public List<EvaluationDatasetResponse> findAll() {
        LambdaQueryWrapper<EvaluationDataset> queryWrapper =
                new LambdaQueryWrapper<EvaluationDataset>()
                        .orderByDesc(EvaluationDataset::getCreatedAt)
                        .orderByDesc(EvaluationDataset::getId);
        return evaluationDatasetMapper.selectList(queryWrapper)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public EvaluationDatasetResponse getById(Long id) {
        return toResponse(getExistingDataset(id));
    }

    @Override
    @Transactional
    public void incrementQuestionCount(Long id, int delta) {
        if (delta <= 0) {
            return;
        }
        EvaluationDataset dataset = getExistingDataset(id);
        int currentCount = dataset.getQuestionCount() == null ? 0 : dataset.getQuestionCount();
        dataset.setQuestionCount(currentCount + delta);
        evaluationDatasetMapper.updateById(dataset);
    }

    private void ensureKnowledgeBaseExists(Long knowledgeBaseId) {
        if (!knowledgeBaseService.existsById(knowledgeBaseId)) {
            throw new BusinessException(
                    KNOWLEDGE_BASE_NOT_FOUND_CODE,
                    "Knowledge base not found: " + knowledgeBaseId
            );
        }
    }

    private EvaluationDataset getExistingDataset(Long id) {
        EvaluationDataset dataset = evaluationDatasetMapper.selectById(id);
        if (dataset == null) {
            throw new BusinessException(DATASET_NOT_FOUND_CODE, "Evaluation dataset not found: " + id);
        }
        return dataset;
    }

    private EvaluationDatasetResponse toResponse(EvaluationDataset dataset) {
        return new EvaluationDatasetResponse(
                dataset.getId(),
                dataset.getName(),
                dataset.getKnowledgeBaseId(),
                dataset.getDescription(),
                dataset.getQuestionCount(),
                dataset.getCreatedAt()
        );
    }
}
