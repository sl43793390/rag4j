package com.sl.rag4j.ragservice;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 向量存储服务，支持两种模式：
 * 1. milvus-lite模式：使用InMemoryEmbeddingStore，数据持久化到JSON文件，无需安装Milvus服务
 * 2. milvus模式：使用MilvusEmbeddingStore，需要运行Milvus服务
 * 每个知识库对应一个独立的存储实例，通过缓存避免重复创建
 * 模式通过rag4j.vector-store-mode配置项切换
 */
@Service
public class MilvusService {

    /** 向量存储模式：milvus-lite（内存）或milvus（服务端） */
    @Value("${rag4j.vector-store-mode}")
    private String vectorStoreMode;

    /** milvus-lite模式下的持久化文件路径 */
    @Value("${rag4j.vector-store-file}")
    private String vectorStoreFile;

    @Value("${milvus.host}")
    private String milvusHost;

    @Value("${milvus.port}")
    private int milvusPort;

    @Value("${milvus.username:}")
    private String milvusUsername;

    @Value("${milvus.password:}")
    private String milvusPassword;

    @Value("${rag4j.embedding-dimension}")
    private int embeddingDimension;

    private final EmbeddingModel embeddingModel;
    /** Milvus客户端，仅在milvus模式下使用，milvus-lite模式下为null */
    private MilvusClientV2 milvusClient;

    /** milvus-lite模式下，所有知识库共享一个InMemoryEmbeddingStore实例 */
    private InMemoryEmbeddingStore<TextSegment> sharedInMemoryStore;

    /** 缓存每个知识库对应的EmbeddingStore实例，避免重复创建 */
    private final ConcurrentHashMap<String, EmbeddingStore<TextSegment>> storeCache = new ConcurrentHashMap<>();

    public MilvusService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @PostConstruct
    public void init() {
        if ("milvus".equals(vectorStoreMode)) {
            this.milvusClient = new MilvusClientV2(ConnectConfig.builder()
                    .uri("http://"+milvusHost + ":" + milvusPort)
                    .token(milvusUsername + ":" + milvusPassword)
                    .build());
        } else if ("milvus-lite".equals(vectorStoreMode)) {
            this.sharedInMemoryStore = loadInMemoryStore();
        }
    }

    @PreDestroy
    public void destroy() {
        if (milvusClient != null) {
            milvusClient.close();
        }
    }

    /**
     * 获取指定collection的EmbeddingStore实例
     * milvus-lite模式：所有知识库共享同一个InMemoryEmbeddingStore，通过metadata区分
     * milvus模式：每个知识库对应独立的MilvusEmbeddingStore和collection
     * @param collectionName collection名称（milvus模式下使用）
     * @return EmbeddingStore实例
     */
    public EmbeddingStore<TextSegment> getEmbeddingStore(String collectionName) {
        if ("milvus-lite".equals(vectorStoreMode)) {
            return sharedInMemoryStore;
        } else {
            return storeCache.computeIfAbsent(collectionName, name ->
                    MilvusEmbeddingStore.builder()
                            .host(milvusHost)
                            .port(milvusPort)
                            .username(milvusUsername)
                            .password(milvusPassword)
                            .collectionName(name)
                            .dimension(embeddingDimension)
                            .build()
            );
        }
    }

    /**
     * 添加单个文本段到指定collection
     * @param collectionName collection名称
     * @param textSegment 文本段内容
     * @return 返回生成的ID
     */
    public String add(String collectionName, TextSegment textSegment) {
        EmbeddingStore<TextSegment> store = getEmbeddingStore(collectionName);
        Embedding embedding = embeddingModel.embed(textSegment).content();
        return store.add(embedding, textSegment);
    }

    /**
     * 批量添加文本段到指定collection
     * @param collectionName collection名称
     * @param textSegments 文本段列表
     * @return 返回生成的ID列表
     */
    public List<String> addAll(String collectionName, List<TextSegment> textSegments) {
        EmbeddingStore<TextSegment> store = getEmbeddingStore(collectionName);
        List<Embedding> embeddings = new ArrayList<>();
        for (TextSegment segment : textSegments) {
            embeddings.add(embeddingModel.embed(segment).content());
        }
        return store.addAll(embeddings, textSegments);
    }

    /**
     * 批量存储嵌入向量和对应的文本段到指定collection
     * EmbeddingService调用此方法完成向量的持久化
     * @param collectionName collection名称
     * @param embeddings 嵌入向量列表
     * @param textSegments 文本段列表
     * @return 返回生成的ID列表
     */
    public List<String> storeEmbeddings(String collectionName, List<Embedding> embeddings, List<TextSegment> textSegments) {
        EmbeddingStore<TextSegment> store = getEmbeddingStore(collectionName);
        return store.addAll(embeddings, textSegments);
    }

    /**
     * 添加文本段（带metadata）到指定collection
     * @param collectionName collection名称
     * @param text 文本内容
     * @param metadata 元数据
     * @return 返回生成的ID
     */
    public String addWithMetadata(String collectionName, String text, Metadata metadata) {
        TextSegment segment = TextSegment.from(text, metadata);
        return add(collectionName, segment);
    }

    /**
     * 在指定存储中搜索与查询文本最相似的文本段
     * @param collectionName collection名称
     * @param queryText 查询文本
     * @param maxResults 最大返回结果数
     * @return 相似匹配结果列表
     */
    public List<EmbeddingMatch<TextSegment>> searchRelevant(String collectionName, String queryText, int maxResults) {
        EmbeddingStore<TextSegment> store = getEmbeddingStore(collectionName);
        Embedding queryEmbedding = embeddingModel.embed(queryText).content();
        EmbeddingSearchResult<TextSegment> result = store.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .build());
        return result.matches();
    }

    /**
     * 在指定存储中搜索与查询文本最相似的文本段，带最低分数阈值
     * @param collectionName collection名称
     * @param queryText 查询文本
     * @param maxResults 最大返回结果数
     * @param minScore 最低相似度分数阈值
     * @return 相似匹配结果列表
     */
    public List<EmbeddingMatch<TextSegment>> searchRelevant(String collectionName, String queryText, int maxResults, double minScore) {
        EmbeddingStore<TextSegment> store = getEmbeddingStore(collectionName);
        Embedding queryEmbedding = embeddingModel.embed(queryText).content();
        EmbeddingSearchResult<TextSegment> result = store.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .minScore(minScore)
                .build());
        return result.matches();
    }

    /**
     * 根据ID删除指定collection中的向量数据
     * @param collectionName collection名称
     * @param ids 要删除的ID列表
     */
    public void deleteByIds(String collectionName, List<String> ids) {
        if ("milvus-lite".equals(vectorStoreMode)) {
            InMemoryEmbeddingStore<TextSegment> memoryStore = sharedInMemoryStore;
            if (memoryStore != null) {
                for (String id : ids) {
                    memoryStore.remove(id);
                }
                persistInMemoryStore();
            }
        } else {
            if (milvusClient != null) {
                String filter = ids.stream()
                        .map(id -> "id == \"" + id + "\"")
                        .reduce((a, b) -> a + " || " + b)
                        .orElse("");
                if (!filter.isEmpty()) {
                    milvusClient.delete(DeleteReq.builder()
                            .collectionName(collectionName)
                            .filter(filter)
                            .build());
                }
            }
        }
    }

    /**
     * 根据ID删除单条向量数据
     * @param collectionName collection名称
     * @param id 要删除的ID
     */
    public void deleteById(String collectionName, String id) {
        if ("milvus-lite".equals(vectorStoreMode)) {
            if (sharedInMemoryStore != null) {
                sharedInMemoryStore.remove(id);
                persistInMemoryStore();
            }
        } else {
            if (milvusClient != null) {
                milvusClient.delete(DeleteReq.builder()
                        .collectionName(collectionName)
                        .filter("id == \"" + id + "\"")
                        .build());
            }
        }
    }

    /**
     * 删除指定collection的所有向量数据
     * @param collectionName collection名称
     */
    public void deleteCollection(String collectionName) {
        if ("milvus-lite".equals(vectorStoreMode)) {
            storeCache.remove(collectionName);
            persistInMemoryStore();
        } else {
            if (milvusClient != null) {
                milvusClient.dropCollection(DropCollectionReq.builder()
                        .collectionName(collectionName)
                        .build());
            }
            storeCache.remove(collectionName);
        }
    }

    /**
     * 列出所有collection
     * @return collection名称列表
     */
    public List<String> listCollections() {
        if ("milvus-lite".equals(vectorStoreMode)) {
            return List.of("shared-in-memory-store");
        } else {
            if (milvusClient != null) {
                try {
                    var resp = milvusClient.listCollections();
                    return resp.getCollectionNames();
                } catch (Exception e) {
                    System.err.println("列出collection失败: " + e.getMessage());
                    return List.of();
                }
            }
            return List.of();
        }
    }

    /**
     * 判断collection是否存在
     * @param collectionName collection名称
     * @return 是否存在
     */
    public boolean hasCollection(String collectionName) {
        if ("milvus-lite".equals(vectorStoreMode)) {
            return true;
        } else {
            if (milvusClient != null) {
                return milvusClient.hasCollection(HasCollectionReq.builder()
                        .collectionName(collectionName)
                        .build());
            }
            return false;
        }
    }

    /**
     * 创建collection（仅milvus模式）
     * milvus-lite模式下collection是隐式创建的
     * @param collectionName collection名称
     */
    public void createCollection(String collectionName) {
        if ("milvus-lite".equals(vectorStoreMode)) {
            return;
        }
        if (milvusClient != null && !hasCollection(collectionName)) {
            milvusClient.createCollection(CreateCollectionReq.builder()
                    .collectionName(collectionName)
                    .dimension(embeddingDimension)
                    .primaryFieldName("id")
                    .vectorFieldName("vector")
                    .metricType("COSINE")
                    .build());
        }
    }

    /**
     * 获取collection中的向量数量
     * milvus-lite模式：直接返回内存存储的大小
     * milvus模式：返回0（需通过MilvusClient获取准确统计）
     * @param collectionName collection名称
     * @return 向量数量
     */
    public long getCollectionCount(String collectionName) {
        if ("milvus-lite".equals(vectorStoreMode)) {
            return sharedInMemoryStore != null ? sharedInMemoryStore.size() : 0;
        } else {
            // milvus模式下需要通过MilvusClient查询，这里返回缓存中的store计数
            // 实际使用时可通过milvusClient.getCollectionStats获取
            EmbeddingStore<TextSegment> store = storeCache.get(collectionName);
            if (store == null) {
                return 0;
            }
            // 通过搜索一个空查询来估算，或者直接返回0
            return 0;
        }
    }

    /**
     * 加载InMemoryEmbeddingStore从JSON文件，如果文件不存在则创建新的
     */
    private InMemoryEmbeddingStore<TextSegment> loadInMemoryStore() {
        Path filePath = Path.of(vectorStoreFile);
        try {
            if (Files.exists(filePath)) {
                String json = Files.readString(filePath);
                return InMemoryEmbeddingStore.fromJson(json);
            }
        } catch (IOException e) {
            System.err.println("加载向量存储文件失败，创建新的: " + e.getMessage());
        }
        return new InMemoryEmbeddingStore<>();
    }

    /**
     * 将InMemoryEmbeddingStore持久化到JSON文件
     */
    private void persistInMemoryStore() {
        if (sharedInMemoryStore != null) {
            try {
                String json = sharedInMemoryStore.serializeToJson();
                Path filePath = Path.of(vectorStoreFile);
                Files.writeString(filePath, json);
            } catch (IOException e) {
                System.err.println("向量存储持久化失败: " + e.getMessage());
            }
        }
    }
}