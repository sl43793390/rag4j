package com.sl.rag4j.controller;

import com.sl.rag4j.config.UserInfoHelper;
import com.sl.rag4j.entity.Document;
import com.sl.rag4j.entity.KnowledgeBase;
import com.sl.rag4j.mapper.DocumentMapper;
import com.sl.rag4j.mapper.KnowledgeBaseMapper;
import com.sl.rag4j.model.CreateKbRequest;
import com.sl.rag4j.ragservice.EmbeddingService;
import com.sl.rag4j.ragservice.MilvusService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 知识库REST API控制器，提供知识库的CRUD操作和文档嵌入接口
 * 供外部应用通过HTTP Basic认证调用RAG服务
 */
@RestController
@RequestMapping("/api/knowledge-bases")
public class KnowledgeBaseController {

    private final KnowledgeBaseMapper kbMapper;
    private final DocumentMapper documentMapper;
    private final EmbeddingService embeddingService;
    private final MilvusService milvusService;

    public KnowledgeBaseController(KnowledgeBaseMapper kbMapper,
                                   DocumentMapper documentMapper,
                                   EmbeddingService embeddingService,
                                   MilvusService milvusService) {
        this.kbMapper = kbMapper;
        this.documentMapper = documentMapper;
        this.embeddingService = embeddingService;
        this.milvusService = milvusService;
    }

    /**
     * 获取知识库列表，支持按名称关键词过滤
     * @param keyword 搜索关键词（可选）
     * @return 知识库列表
     */
    @GetMapping
    public List<KnowledgeBase> list(@RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(KnowledgeBase::getName, keyword);
        }
        wrapper.orderByDesc(KnowledgeBase::getCreatedAt);
        return kbMapper.selectList(wrapper);
    }

    /**
     * 创建新知识库，自动生成Milvus collection名称
     * @param request 创建请求DTO
     * @return 创建成功后的知识库实体（含自动生成的ID）
     */
    @PostMapping
    public KnowledgeBase create(@RequestBody CreateKbRequest request) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(request.name());
        kb.setDescription(request.description());
        kb.setSplitterType(request.splitterType());
        kb.setEmbeddingModelName(request.embeddingModelName());
        kb.setMilvusCollectionName("kb_" + System.currentTimeMillis());
        kb.setMaxSegmentSize(request.maxSegmentSize());
        kb.setMaxOverlapSize(request.maxOverlapSize());
        kb.setDocCount(0);
        kb.setUserName(UserInfoHelper.getUserIdFromSession()); // TODO: 从认证信息获取用户ID
        kbMapper.insert(kb);
        return kb;
    }

    /**
     * 向指定知识库嵌入文档文件
     * 对每个上传文件执行完整的嵌入流水线：解析→切割→嵌入→存储
     * @param kbId 知识库ID
     * @param files 上传的文件数组
     * @return 嵌入结果消息
     */
    @PostMapping("/{kbId}/embed")
    public ResponseEntity<String> embedDocuments(@PathVariable Long kbId,
                                                 @RequestParam("files") MultipartFile[] files) {
        for (MultipartFile file : files) {
            try {
                embeddingService.ingestDocument(kbId, file.getOriginalFilename(), file.getInputStream());
            } catch (IOException e) {
                return ResponseEntity.badRequest().body("文件处理失败: " + file.getOriginalFilename());
            }
        }
        return ResponseEntity.ok("文档嵌入成功，共处理 " + files.length + " 个文件");
    }

    /**
     * 删除知识库，同时清理关联的文档记录和Milvus collection
     * @param kbId 知识库ID
     * @return 204 No Content
     */
    @DeleteMapping("/{kbId}")
    public ResponseEntity<Void> delete(@PathVariable Long kbId) {
        KnowledgeBase kb = kbMapper.selectById(kbId);
        if (kb == null) {
            return ResponseEntity.notFound().build();
        }

        // 删除Milvus向量数据
        milvusService.deleteCollection(kb.getMilvusCollectionName());

        // 删除SQLite中的文档记录
        documentMapper.delete(new LambdaQueryWrapper<Document>()
                .eq(Document::getKnowledgeBaseId, kbId));

        // 删除知识库记录
        kbMapper.deleteById(kbId);

        return ResponseEntity.noContent().build();
    }
}