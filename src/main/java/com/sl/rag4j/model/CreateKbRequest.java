package com.sl.rag4j.model;

/**
 * 创建知识库请求DTO，用于REST API接收创建知识库的参数
 */
public record CreateKbRequest(
        String name,
        String description,
        String splitterType,
        String embeddingModelName,
        int maxSegmentSize,
        int maxOverlapSize
) {}