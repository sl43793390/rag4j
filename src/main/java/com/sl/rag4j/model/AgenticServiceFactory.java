package com.sl.rag4j.model;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;


public class AgenticServiceFactory implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    public static RagAgent createRagAgent(){
        OpenAiChatModel openAiChatModel = applicationContext.getBean(OpenAiChatModel.class);
        ChatMemoryProvider chatMemoryProvider = applicationContext.getBean(ChatMemoryProvider.class);
        return AgenticServices.agentBuilder(RagAgent.class)
                .chatModel(openAiChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .systemMessage("你是一个知识助手。根据检索到的文档内容回答用户的问题。 如果在提供的内容中找不到答案，请说:我没有足够的信息来回答这个问题。 尽可能引用来源文档")
//                .outputKey("story")
                .build();
    }
    /**
     * 第一种手动创建agent的方式
     * @return
     */
//    public TestAgent createTestAgent(){
//        OpenAiChatModel openAiChatModel = applicationContext.getBean(OpenAiChatModel.class);
//        ChatMemoryProvider chatMemoryProvider = applicationContext.getBean(ChatMemoryProvider.class);
//        return AgenticServices.agentBuilder(TestAgent.class)
//                .chatModel(openAiChatModel)
//                .chatMemoryProvider(chatMemoryProvider)
//                .outputKey("story")
//                .name("test-agent")
//                .description("你是一个专业的助手，你的任务是帮助用户解决问题，...")
//                .build();
//    }

    /**
     * 第二种手动创建agent的方式,可动态修改agent的描述信息，可动态修改agent的输入参数，
     * 可动态修改agent的输出参数，可动态修改agent的返回类型，
     * 可动态修改agent的输出key，可动态修改agent的输入key，
     * @return
     */
    public UntypedAgent creativeWriter2(){
        OpenAiChatModel openAiChatModel = applicationContext.getBean(OpenAiChatModel.class);
        ChatMemoryProvider chatMemoryProvider = applicationContext.getBean(ChatMemoryProvider.class);
        UntypedAgent creativeWriter = AgenticServices.agentBuilder()
                .chatModel(openAiChatModel)
                .description("Generate a story based on the given topic")
                .userMessage("""
                You are a creative writer.
                Generate a draft of a story no more than
                3 sentences long around the given topic.
                Return only the story and nothing else.
                The topic is {{topic}}.
                """)
                .inputKey(String.class, "topic")
                .chatMemoryProvider(chatMemoryProvider)
                .returnType(String.class) // String is the default return type for untyped agents
                .outputKey("story")
                .build();
        return creativeWriter;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        AgenticServiceFactory.applicationContext = applicationContext;
    }
}
