package com.sl.rag4j.ragservice;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.document.splitter.DocumentByWordSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文本切割服务，根据用户选择的切割策略将文档分割为多个文本段
 * 每种切割策略对应LangChain4j的不同DocumentSplitter实现
 */
@Service
public class TextSplittingService {

    @Value("${langchain4j.open-ai.embedding-model.model-name}")
    private String embeddingModelName;

    /**
     * 将文档按指定切割策略分割为文本段列表
     * @param document LangChain4j文档对象
     * @param splitterType 切割方式（RECURSIVE/BY_PARAGRAPH等）
     * @param maxSegmentSize 每段最大大小（token数或字符数）
     * @param maxOverlapSize 相邻段重叠大小
     * @return 分割后的文本段列表
     */
    public List<TextSegment> split(Document document, String splitterType,
                                    int maxSegmentSize, int maxOverlapSize) {
        DocumentSplitter splitter = createSplitter(splitterType, maxSegmentSize, maxOverlapSize);
        return splitter.split(document);
    }

    /**
     * 根据切割类型创建对应的DocumentSplitter实例
     * RECURSIVE/BY_PARAGRAPH/BY_SENTENCE/BY_TOKEN需要OpenAiTokenizer
     * BY_CHARACTER不需要tokenizer，直接按字符数分割
     */
    private DocumentSplitter createSplitter(String type, int maxSegmentSize, int maxOverlapSize) {
        switch (type) {
            case "RECURSIVE":
                // 递归分割：优先按段落分割，段落过长则按句子，句子过长则按字符
                return DocumentSplitters.recursive(maxSegmentSize, maxOverlapSize);
            case "BY_PARAGRAPH":
                // 按段落分割：每个段落作为一个段，超过最大尺寸则进一步分割
                return new DocumentByParagraphSplitter(maxSegmentSize, maxOverlapSize);
            case "BY_SENTENCE":
                // 按句子分割：每个句子作为一个段
                return new DocumentBySentenceSplitter(maxSegmentSize, maxOverlapSize);
//            case "BY_TOKEN":
//                // 按token数分割：精确控制每段的token数量
//                return DocumentSplitters.byToken(maxSegmentSize, maxOverlapSize);
            case "BY_CHARACTER":
                // 按字符数分割：不需要tokenizer，直接按字符长度分割
                return new DocumentByWordSplitter(maxSegmentSize, maxOverlapSize);
            default:
                // 默认使用递归分割
//                return DocumentSplitters.recursive(maxSegmentSize, maxOverlapSize);
                return new DocumentByParagraphSplitter(maxSegmentSize, maxOverlapSize);
        }
    }
}