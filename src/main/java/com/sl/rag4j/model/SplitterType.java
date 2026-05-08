package com.sl.rag4j.model;

/**
 * 文本切割方式枚举，对应LangChain4j的DocumentSplitter策略
 * 用户在创建知识库时选择切割方式，影响文档分割粒度
 */
public enum SplitterType {

    /** 递归切割：按段落→句子→字符递归分割，优先保证语义完整性 */
    RECURSIVE,

    /** 按段落切割：每个段落作为一个文本段 */
    BY_PARAGRAPH,

    /** 按句子切割：每个句子作为一个文本段 */
    BY_SENTENCE,

    /** 按token数量切割：按指定token数分割文本 */
//    BY_TOKEN,

    /** 按字符数量切割：按指定字符数分割文本，不需要tokenizer */
    BY_CHARACTER
}