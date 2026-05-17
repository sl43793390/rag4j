package com.sl.rag4j.ragservice;

import com.sl.rag4j.entity.KnowledgeBase;
import com.sl.rag4j.mapper.KnowledgeBaseMapper;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.embedding.EmbeddingStore;
import com.sl.rag4j.model.RagAssistant;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * RAG查询服务，实现基于知识库的检索增强生成问答
 * 为每个知识库动态构建AiService实例，包含同步查询和流式查询两种模式
 * 支持会话记忆机制（通过ChatMemoryProvider和SqlLiteChatMemoryStore持久化对话历史）
 * 流式查询支持Vaadin UI实时显示和REST API SSE推送
 */
@Service
public class RagQueryService {

    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final EmbeddingModel embeddingModel;
    private final MilvusService milvusService;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final ChatMemoryProvider chatMemoryProvider;
    private final QueryRewriteService queryRewriteService;

    /** 缓存每个知识库对应的RagAssistant实例（同步模式），避免重复构建代理对象 */
    private final ConcurrentHashMap<String, RagAssistant> assistantCache = new ConcurrentHashMap<>();
    /** 缓存每个知识库对应的RagAssistant实例（流式模式） */
    private final ConcurrentHashMap<String, RagAssistant> streamingAssistantCache = new ConcurrentHashMap<>();

    /**
     * 构造RAG查询服务，注入所有必要依赖
     * ChatMemoryProvider用于为每个会话创建独立的对话记忆
     */
    public RagQueryService(ChatModel chatModel,
                           StreamingChatModel streamingChatModel,
                           EmbeddingModel embeddingModel,
                           MilvusService milvusService,
                           KnowledgeBaseMapper knowledgeBaseMapper,
                           ChatMemoryProvider chatMemoryProvider,
                           QueryRewriteService queryRewriteService) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.embeddingModel = embeddingModel;
        this.milvusService = milvusService;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.chatMemoryProvider = chatMemoryProvider;
        this.queryRewriteService = queryRewriteService;
    }

    /**
     * 同步查询：根据知识库检索相关文档，调用LLM生成完整回答
     * 使用会话记忆机制，同一会话ID的多次对话会保留上下文
     * @param kbId 知识库ID
     * @param memoryId 会话ID，用于区分不同的对话会话
     * @param question 用户问题
     * @return AI生成的回答文本
     */
    public String query(Long kbId, String memoryId, String question) {
        // 使用kbId和memoryId的组合作为缓存键，确保不同会话的助手实例独立
        String cacheKey = kbId + "_" + memoryId;
        RagAssistant assistant = assistantCache.computeIfAbsent(cacheKey, key -> {
            KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
            RetrievalAugmentor augmentor = buildRetrievalAugmentor(kb);
            return AiServices.builder(RagAssistant.class)
                    .chatModel(chatModel)
                    .chatMemoryProvider(chatMemoryProvider)
                    .retrievalAugmentor(augmentor)
                    .build();
        });
        return assistant.answer(memoryId, question);
    }

    /**
     * 带引用来源的同步查询：返回回答文本和检索到的源文档信息
     * @param kbId 知识库ID
     * @param memoryId 会话ID
     * @param question 用户问题
     * @return 包含回答和源文档的RagResult
     */
//    public RagResult queryWithSources(Long kbId, String memoryId, String question) {
//        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
//        EmbeddingStore<TextSegment> embeddingStore = milvusService.getEmbeddingStore(kb.getMilvusCollectionName());
//        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
//                .embeddingStore(embeddingStore)
//                .embeddingModel(embeddingModel)
//                .maxResults(5)
//                .minScore(0.5)
//                .build();
//
//        // 先用问题做向量检索获取源文档
//        List<Content> contents = contentRetriever.retrieve(
//                dev.langchain4j.rag.query.Query.from(question));
//
//        // 提取源文档的metadata信息（文件名等）
//        List<String> sourceDocs = contents.stream()
//                .map(c -> c.textSegment().metadata().getString("file_name"))
//                .filter(s -> s != null && !s.isBlank())
//                .distinct()
//                .toList();
//
//        // 使用缓存的助手生成回答
//        String cacheKey = kbId + "_" + memoryId;
//        RagAssistant assistant = assistantCache.computeIfAbsent(cacheKey, key -> {
//            RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
//                    .contentRetriever(contentRetriever)
//                    .build();
//            return AiServices.builder(RagAssistant.class)
//                    .chatModel(chatModel)
//                    .chatMemoryProvider(chatMemoryProvider)
//                    .retrievalAugmentor(augmentor)
//                    .build();
//        });
//        String answer = assistant.answer(memoryId, question);
//        return new RagResult(answer, sourceDocs);
//    }

    /**
     * 流式查询（Vaadin UI用）：返回TokenStream对象，通过回调逐步接收回答内容
     * 使用会话记忆机制，同一会话ID的多次对话会保留上下文
     * @param kbId 知识库ID
     * @param memoryId 会话ID
     * @param question 用户问题
     * @return TokenStream对象，支持onNext/onComplete/onError回调
     */
    public TokenStream streamQuery(Long kbId, String memoryId, String question) {
        String cacheKey = kbId + "_" + memoryId;
        RagAssistant assistant = streamingAssistantCache.computeIfAbsent(cacheKey, key -> {
            KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
            RetrievalAugmentor augmentor = buildRetrievalAugmentor(kb);
            return AiServices.builder(RagAssistant.class)
                    .streamingChatModel(streamingChatModel)
                    .chatMemoryProvider(chatMemoryProvider)
                    .retrievalAugmentor(augmentor)
                    .build();
        });
        return assistant.stream(memoryId, question);
    }

    /**
     * 流式查询（REST API用）：将TokenStream适配为WebFlux Flux<String>，支持SSE推送
     * 使用Flux.create将TokenStream回调转换为响应式流
     * @param kbId 知识库ID
     * @param memoryId 会话ID
     * @param question 用户问题
     * @return Flux<String>流，每个元素为一个token片段
     */
    public Flux<String> streamQueryFlux(Long kbId, String memoryId, String question) {
        return Flux.create(sink -> {
            TokenStream tokenStream = streamQuery(kbId, memoryId, question);
            tokenStream
                    .onPartialResponse(sink::next)
                    .onCompleteResponse(response -> sink.complete())
                    .onError(sink::error)
                    .start();
        });
    }

    /**
     * 根据知识库配置构建检索增强器
     * 使用知识库对应的向量存储（Milvus或InMemory）作为检索源
     * 配置最大返回2个相关结果，最低相似度0.5
     * 集成查询重写，将用户问题改写为多种表述以提高检索召回率
     */
    private RetrievalAugmentor buildRetrievalAugmentor(KnowledgeBase kb) {
        EmbeddingStore<TextSegment> embeddingStore = milvusService.getEmbeddingStore(kb.getMilvusCollectionName());
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.5)
                .build();

        // 查询转换器：将原始问题重写为多种表述方式
        QueryTransformer queryTransformer = (query) -> {
            List<String> variations = queryRewriteService.rewrite(query.text());
            return variations.stream()
                    .map(Query::from)
                    .collect(Collectors.toList());
        };

        return DefaultRetrievalAugmentor.builder()
                .queryTransformer(queryTransformer)
                .contentRetriever(contentRetriever)
                .build();
    }

    /**
     * 清除指定知识库的AiService缓存，在知识库配置变更时调用
     */
    public void clearCache(Long kbId) {
        assistantCache.keySet().removeIf(key -> key.startsWith(kbId + "_"));
        streamingAssistantCache.keySet().removeIf(key -> key.startsWith(kbId + "_"));
    }
}