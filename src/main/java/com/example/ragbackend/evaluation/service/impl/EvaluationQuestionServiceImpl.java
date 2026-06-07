package com.example.ragbackend.evaluation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ragbackend.chunk.dto.DocumentChunkResponse;
import com.example.ragbackend.chunk.service.DocumentChunkService;
import com.example.ragbackend.common.exception.BusinessException;
import com.example.ragbackend.evaluation.dto.EvaluationDatasetResponse;
import com.example.ragbackend.evaluation.dto.EvaluationQuestionCreateRequest;
import com.example.ragbackend.evaluation.dto.EvaluationQuestionImportRequest;
import com.example.ragbackend.evaluation.dto.EvaluationQuestionResponse;
import com.example.ragbackend.evaluation.entity.EvaluationQuestion;
import com.example.ragbackend.evaluation.mapper.EvaluationQuestionMapper;
import com.example.ragbackend.evaluation.service.EvaluationDatasetService;
import com.example.ragbackend.evaluation.service.EvaluationQuestionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EvaluationQuestionServiceImpl implements EvaluationQuestionService {

    private static final String QUESTION_NOT_FOUND_CODE = "EVALUATION_QUESTION_NOT_FOUND";
    private static final String QUESTION_REQUEST_INVALID_CODE = "EVALUATION_QUESTION_REQUEST_INVALID";
    private static final String RELEVANT_CHUNKS_EMPTY_CODE = "EVALUATION_RELEVANT_CHUNKS_EMPTY";
    private static final String RELEVANT_CHUNKS_NOT_FOUND_CODE = "EVALUATION_RELEVANT_CHUNKS_NOT_FOUND";
    private static final String RELEVANT_CHUNK_KB_MISMATCH_CODE = "EVALUATION_RELEVANT_CHUNK_KB_MISMATCH";
    private static final String QUESTION_JSON_INVALID_CODE = "EVALUATION_QUESTION_JSON_INVALID";

    private final EvaluationQuestionMapper evaluationQuestionMapper;
    private final EvaluationDatasetService evaluationDatasetService;
    private final DocumentChunkService documentChunkService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public EvaluationQuestionResponse create(Long datasetId, EvaluationQuestionCreateRequest request) {
        EvaluationDatasetResponse dataset = evaluationDatasetService.getById(datasetId);
        EvaluationQuestionResponse response = createOne(dataset, request);
        evaluationDatasetService.incrementQuestionCount(datasetId, 1);
        return response;
    }

    @Override
    @Transactional
    public List<EvaluationQuestionResponse> importQuestions(
            Long datasetId,
            EvaluationQuestionImportRequest request
    ) {
        if (request == null || request.questions() == null || request.questions().isEmpty()) {
            throw new BusinessException(QUESTION_REQUEST_INVALID_CODE, "Evaluation questions cannot be empty");
        }

        EvaluationDatasetResponse dataset = evaluationDatasetService.getById(datasetId);
        List<EvaluationQuestionResponse> responses = request.questions()
                .stream()
                .map(question -> createOne(dataset, question))
                .toList();
        evaluationDatasetService.incrementQuestionCount(datasetId, responses.size());
        return responses;
    }

    @Override
    public List<EvaluationQuestionResponse> findByDatasetId(Long datasetId) {
        evaluationDatasetService.getById(datasetId);

        LambdaQueryWrapper<EvaluationQuestion> queryWrapper =
                new LambdaQueryWrapper<EvaluationQuestion>()
                        .eq(EvaluationQuestion::getDatasetId, datasetId)
                        .orderByAsc(EvaluationQuestion::getCreatedAt)
                        .orderByAsc(EvaluationQuestion::getId);
        return evaluationQuestionMapper.selectList(queryWrapper)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public EvaluationQuestionResponse getById(Long id) {
        EvaluationQuestion question = evaluationQuestionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException(QUESTION_NOT_FOUND_CODE, "Evaluation question not found: " + id);
        }
        return toResponse(question);
    }

    private EvaluationQuestionResponse createOne(
            EvaluationDatasetResponse dataset,
            EvaluationQuestionCreateRequest request
    ) {
        validateQuestionRequest(request);

        List<Long> relevantChunkIds = request.relevantChunkIds();
        Map<Long, DocumentChunkResponse> chunksById = activeChunksById(relevantChunkIds);
        ensureAllRelevantChunksExist(relevantChunkIds, chunksById);
        ensureRelevantChunksBelongToDataset(relevantChunkIds, chunksById, dataset.knowledgeBaseId());

        List<String> contentHashes = relevantChunkIds.stream()
                .map(chunkId -> chunksById.get(chunkId).contentHash())
                .toList();

        EvaluationQuestion question = new EvaluationQuestion();
        question.setDatasetId(dataset.id());
        question.setQuestion(request.question());
        question.setGroundTruthAnswer(request.groundTruthAnswer());
        question.setRelevantChunkIdsJson(writeJson(relevantChunkIds));
        question.setRelevantContentHashesJson(writeJson(contentHashes));
        question.setDocumentProcessingVersion(resolveProcessingVersion(relevantChunkIds, chunksById));
        question.setQuestionType(normalizeQuestionType(request.questionType()));

        evaluationQuestionMapper.insert(question);

        return toResponse(evaluationQuestionMapper.selectById(question.getId()));
    }

    private void validateQuestionRequest(EvaluationQuestionCreateRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new BusinessException(QUESTION_REQUEST_INVALID_CODE, "Evaluation question cannot be blank");
        }
        if (request.relevantChunkIds() == null || request.relevantChunkIds().isEmpty()) {
            throw new BusinessException(RELEVANT_CHUNKS_EMPTY_CODE, "Relevant chunk ids cannot be empty");
        }
        if (request.relevantChunkIds().stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException(QUESTION_REQUEST_INVALID_CODE, "Relevant chunk ids must be positive");
        }
        String questionType = request.questionType();
        if (questionType != null && questionType.length() > 30) {
            throw new BusinessException(QUESTION_REQUEST_INVALID_CODE, "Question type length must be at most 30");
        }
    }

    private Map<Long, DocumentChunkResponse> activeChunksById(List<Long> relevantChunkIds) {
        List<DocumentChunkResponse> chunks = documentChunkService.findActiveByIds(relevantChunkIds);
        Map<Long, DocumentChunkResponse> chunksById = new LinkedHashMap<>();
        for (DocumentChunkResponse chunk : chunks) {
            chunksById.put(chunk.id(), chunk);
        }
        return chunksById;
    }

    private void ensureAllRelevantChunksExist(
            List<Long> relevantChunkIds,
            Map<Long, DocumentChunkResponse> chunksById
    ) {
        List<Long> missingIds = relevantChunkIds.stream()
                .distinct()
                .filter(chunkId -> !chunksById.containsKey(chunkId))
                .toList();
        if (!missingIds.isEmpty()) {
            throw new BusinessException(
                    RELEVANT_CHUNKS_NOT_FOUND_CODE,
                    "Relevant chunks not found or inactive: " + missingIds
            );
        }
    }

    private void ensureRelevantChunksBelongToDataset(
            List<Long> relevantChunkIds,
            Map<Long, DocumentChunkResponse> chunksById,
            Long knowledgeBaseId
    ) {
        List<Long> mismatchIds = relevantChunkIds.stream()
                .distinct()
                .filter(chunkId -> !knowledgeBaseId.equals(chunksById.get(chunkId).knowledgeBaseId()))
                .toList();
        if (!mismatchIds.isEmpty()) {
            throw new BusinessException(
                    RELEVANT_CHUNK_KB_MISMATCH_CODE,
                    "Relevant chunks do not belong to knowledge base " + knowledgeBaseId + ": " + mismatchIds
            );
        }
    }

    private Integer resolveProcessingVersion(
            List<Long> relevantChunkIds,
            Map<Long, DocumentChunkResponse> chunksById
    ) {
        Set<Integer> versions = relevantChunkIds.stream()
                .map(chunkId -> chunksById.get(chunkId).processingVersion())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        boolean everyChunkHasVersion = relevantChunkIds.stream()
                .map(chunksById::get)
                .allMatch(chunk -> chunk.processingVersion() != null);
        if (everyChunkHasVersion && versions.size() == 1) {
            return versions.iterator().next();
        }
        return null;
    }

    private String normalizeQuestionType(String questionType) {
        if (questionType == null || questionType.isBlank()) {
            return null;
        }
        return questionType;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(QUESTION_JSON_INVALID_CODE, "Failed to serialize evaluation question data");
        }
    }

    private List<Long> readLongList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new BusinessException(QUESTION_JSON_INVALID_CODE, "Failed to parse relevant chunk ids");
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new BusinessException(QUESTION_JSON_INVALID_CODE, "Failed to parse relevant content hashes");
        }
    }

    private EvaluationQuestionResponse toResponse(EvaluationQuestion question) {
        return new EvaluationQuestionResponse(
                question.getId(),
                question.getDatasetId(),
                question.getQuestion(),
                question.getGroundTruthAnswer(),
                readLongList(question.getRelevantChunkIdsJson()),
                readStringList(question.getRelevantContentHashesJson()),
                question.getDocumentProcessingVersion(),
                question.getQuestionType(),
                question.getCreatedAt()
        );
    }
}
