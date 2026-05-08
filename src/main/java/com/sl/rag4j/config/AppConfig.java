package com.sl.rag4j.config;

import com.sl.rag4j.entity.User;
import com.sl.rag4j.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.hutool.crypto.SmUtil;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 应用通用配置，负责初始化上传目录、默认管理员用户和会话记忆提供者
 */
@Configuration
public class AppConfig {

    private static final Logger log  = LoggerFactory.getLogger(AppConfig.class);
    @Value("${rag4j.upload-dir}")
    private String uploadDir;

    private final UserMapper userMapper;
    private final SqlLiteChatMemoryStore sqlLiteChatMemoryStore;

    public AppConfig(UserMapper userMapper,
                     SqlLiteChatMemoryStore sqlLiteChatMemoryStore) {
        this.userMapper = userMapper;
        this.sqlLiteChatMemoryStore = sqlLiteChatMemoryStore;
    }

    /**
     * 创建会话记忆提供者，为每个会话ID创建独立的ChatMemory实例
     * 使用SqlLiteChatMemoryStore持久化对话历史到SQLite数据库
     * 每个会话最多保留10条消息，避免对话过长导致token消耗过大
     * 会话ID通过RagAssistant接口的@MemoryId参数传入
     *
     * @return ChatMemoryProvider实例
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .chatMemoryStore(sqlLiteChatMemoryStore)
                .maxMessages(10)
                .build();
    }

    /**
     * 应用启动后初始化：创建上传文件目录，插入默认管理员用户
     */
    @PostConstruct
    public void init() throws Exception {
        // 确保上传目录存在
        Path uploadPath = Path.of(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 检查是否已有admin用户，如果没有则创建默认管理员
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, "admin");
        if (userMapper.selectCount(wrapper) == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(SmUtil.sm3("admin"));
            admin.setRole("ADMIN");
            admin.setUserType("ADMIN");
            admin.setFlagStatus(1);
            admin.setEmail("admin@example.com");
            userMapper.insert(admin);
        }
    }

    /**
     * langchain4j 的observer,也就是观察模型输入输出的内容，方便定位问题
     * @return
     */
//    @Bean
//    ChatModelListener chatModelListener() {
//        return new ChatModelListener() {
//
//            @Override
//            public void onRequest(ChatModelRequestContext requestContext) {
//                log.info("onRequest(): {}", requestContext.chatRequest());
//                requestContext.chatRequest().messages().forEach( m -> m.toString());
//            }
//
//            @Override
//            public void onResponse(ChatModelResponseContext responseContext) {
//                log.info("onResponse(): {}", responseContext.chatResponse());
//            }
//
//            @Override
//            public void onError(ChatModelErrorContext errorContext) {
//                log.info("onError(): {}", errorContext.error().getMessage());
//            }
//        };
//    }
}