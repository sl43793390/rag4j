package com.sl.rag4j.controller;

import com.sl.rag4j.model.QueryRequest;
import com.sl.rag4j.model.QueryResponse;
import com.sl.rag4j.ragservice.RagQueryService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * RAG查询REST API控制器，提供同步查询和流式查询两种模式
 * 同步查询返回完整回答JSON，流式查询通过SSE逐步推送回答内容
 * 供外部应用通过HTTP Basic认证调用RAG问答服务
 * 支持会话记忆机制，通过memoryId参数区分不同对话会话
 */
@RestController
@RequestMapping("/api/query")
public class RagQueryController {

    private final RagQueryService ragQueryService;

    public RagQueryController(RagQueryService ragQueryService) {
        this.ragQueryService = ragQueryService;
    }

    /**
     * 同步查询接口：根据知识库检索相关文档，调用LLM生成完整回答
     * @param kbId 知识库ID
     * @param memoryId 会话ID（可选，默认为"default"），用于对话记忆
     * @param request 查询请求（包含用户问题）
     * @return 查询响应（包含AI回答文本）
     */
    @PostMapping("/{kbId}")
    public QueryResponse query(@PathVariable Long kbId,
                               @RequestParam(defaultValue = "default") String memoryId,
                               @RequestBody QueryRequest request) {
        String answer = ragQueryService.query(kbId, memoryId, request.question());
        return new QueryResponse(answer);
    }

    /**
     * 流式查询接口：逐步推送AI回答的每个token片段
     * 使用WebFlux Flux<String>实现SSE（Server-Sent Events）推送
     * @param kbId 知识库ID
     * @param memoryId 会话ID（可选，默认为"default"）
     * @param request 查询请求（包含用户问题）
     * @return Flux<String>流，每个元素为一个token片段
     */
    @PostMapping(value = "/{kbId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamQuery(@PathVariable Long kbId,
                                    @RequestParam(defaultValue = "default") String memoryId,
                                    @RequestBody QueryRequest request) {
        return ragQueryService.streamQueryFlux(kbId, memoryId, request.question());
    }
}