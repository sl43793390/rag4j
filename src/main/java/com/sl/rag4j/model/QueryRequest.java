package com.sl.rag4j.model;

/**
 * RAG查询请求DTO，包含用户提出的问题文本
 */
public record QueryRequest(String question) {}