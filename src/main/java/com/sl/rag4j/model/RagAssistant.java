package com.sl.rag4j.model;

import dev.langchain4j.service.*;

/**
 * RAG助手接口，定义基于知识库检索的问答能力
 * 不使用@AiService注解，因为ContentRetriever需要根据知识库动态构建
 * 通过AiServices.builder()在RagQueryService中动态创建实例
 */
public interface RagAssistant {

    /**
     * 同步问答接口，根据检索到的文档内容回答用户问题
     */
    @SystemMessage("""
        你是一个知识助手。根据检索到的文档内容回答用户的问题。
        如果在提供的内容中找不到答案，请说"我没有足够的信息来回答这个问题。"
        尽可能引用来源文档,如果你确定知道答案也可以回答，但不能编造。
        """)
    @UserMessage("问题：{{msg}}")
    String answer(@MemoryId String memoryId, @V("msg") String userMessage);

    /**
     * 流式问答接口，逐步返回回答内容（用于UI实时显示）
     */
    @SystemMessage("""
        你是一个知识助手。根据检索到的文档内容回答用户的问题。
        如果在提供的内容中找不到答案，请说"我没有足够的信息来回答这个问题。"
        尽可能引用来源文档,如果你确定知道答案也可以回答，但不能编造。
        """)
    @UserMessage("问题：{{msg}}")
    TokenStream stream(@MemoryId String memoryId, @V("msg") String question);
}