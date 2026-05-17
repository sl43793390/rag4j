package com.sl.rag4j.ragservice;

import com.sl.rag4j.entity.Document;
import com.sl.rag4j.entity.KnowledgeBase;
import com.sl.rag4j.mapper.DocumentMapper;
import com.sl.rag4j.mapper.KnowledgeBaseMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档嵌入服务，实现完整的RAG嵌入流水线：
 * 解析文件 → 切割文本 → 嵌入向量 → 存入Milvus → 记录到SQLite
 * 被UI创建对话框和REST API共同调用
 */
@Service
public class EmbeddingService {

    private final DocumentParsingService parsingService;
    private final TextSplittingService splittingService;
    private final MilvusService milvusService;
    private final EmbeddingModel embeddingModel;
    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    public EmbeddingService(DocumentParsingService parsingService,
                            TextSplittingService splittingService,
                            MilvusService milvusService,
                            EmbeddingModel embeddingModel,
                            DocumentMapper documentMapper,
                            KnowledgeBaseMapper knowledgeBaseMapper) {
        this.parsingService = parsingService;
        this.splittingService = splittingService;
        this.milvusService = milvusService;
        this.embeddingModel = embeddingModel;
        this.documentMapper = documentMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
    }

    /**
     * 执行完整的文档嵌入流水线
     * 对每个上传的文件依次执行：解析、切割、嵌入、存储，并更新知识库文档计数
     * @param kbId 知识库ID
     * @param fileName 文件名
     * @param inputStream 文件内容输入流
     */
    public void ingestDocument(Long kbId, String fileName, long fileLength,java.io.InputStream inputStream) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        String collectionName = kb.getMilvusCollectionName();

        // 1. 解析文档内容
        dev.langchain4j.data.document.Document doc = parsingService.parseFile(inputStream, fileName);

        // 2. 按知识库配置的切割方式分割文本
        List<TextSegment> segments = splittingService.split(
                doc, kb.getSplitterType(), kb.getMaxSegmentSize(), kb.getMaxOverlapSize());

        // 3. 添加元数据：知识库ID和文件名，便于后续查询时溯源
        for (TextSegment segment : segments) {
            segment.metadata().put("knowledge_base_id", String.valueOf(kbId));
            segment.metadata().put("file_name", fileName);
        }

        // 4. 批量嵌入文本段为向量
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        // 5. 将向量和文本段存入Milvus collection
        milvusService.storeEmbeddings(collectionName, embeddings, segments);

        // 6. 在SQLite中记录文档信息
        Document docRecord = new Document();
        docRecord.setKnowledgeBaseId(kbId);
        docRecord.setFileName(fileName);
        docRecord.setFileType(getExtension(fileName));
        docRecord.setChunkCount(segments.size());
        docRecord.setCreatedAt(LocalDateTime.now().toString());
        double len = (double) fileLength / 1024;
        if (len < 1024) {
            docRecord.setFileSize(String.format("%.1f KB", len));
        } else if (len < 1024 * 1024) {
            docRecord.setFileSize(String.format("%.2f MB", len / 1024));
        } else {
            docRecord.setFileSize(String.format("%.2f GB", len / (1024 * 1024)));
        }
        documentMapper.insert(docRecord);

        // 7. 更新知识库文档计数
        kb.setDocCount(kb.getDocCount() != null ? kb.getDocCount() + 1 : 1);
        knowledgeBaseMapper.updateById(kb);
    }

    /**
     * 从文件名提取扩展名
     */
    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * 删除指定的Milvus向量集合
     * @param collectionName 集合名称
     */
    public void deleteCollection(String collectionName) {
        milvusService.deleteCollection(collectionName);
    }
}