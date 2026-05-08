package com.sl.rag4j.model;

/**
 * RAG查询响应DTO，包含AI生成的回答文本
 */
public record QueryResponse(String answer) {}