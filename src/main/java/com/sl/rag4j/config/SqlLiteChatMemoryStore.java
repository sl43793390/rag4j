package com.sl.rag4j.config;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sl.rag4j.entity.ChatMemory;
import com.sl.rag4j.mapper.ChatMemoryMapper;
import dev.langchain4j.data.message.*;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * SQLite聊天记忆存储实现，将对话历史持久化到SQLite数据库
 * 实现LangChain4j的ChatMemoryStore接口，支持按会话ID存储和检索聊天消息
 * 使用JSON序列化方式存储ChatMessage列表，保证消息类型的完整性
 */
@Component
public class SqlLiteChatMemoryStore implements ChatMemoryStore {

    private final ChatMemoryMapper chatMemoryMapper;

    /**
     * 构造SQLite聊天记忆存储，注入ChatMemoryMapper用于数据库操作
     */
    public SqlLiteChatMemoryStore(ChatMemoryMapper chatMemoryMapper) {
        this.chatMemoryMapper = chatMemoryMapper;
    }

    /**
     * 启动时创建chat_memory表（如果不存在）
     * 使用CREATE TABLE IF NOT EXISTS保证幂等执行
     */
    @PostConstruct
    private void initTable() {
        chatMemoryMapper.selectList(null);
    }

    /**
     * 根据会话ID获取聊天消息列表
     * 从数据库读取JSON字符串，反序列化为ChatMessage对象列表
     * @param memoryId 会话标识ID
     * @return 该会话的所有聊天消息列表，如果不存在则返回空列表
     */
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String id = String.valueOf(memoryId);
        ChatMemory chatMemory = chatMemoryMapper.selectById(id);
        if (chatMemory == null || chatMemory.getMessages() == null || chatMemory.getMessages().isBlank()) {
            return new ArrayList<>();
        }
        return ChatMessageDeserializer.messagesFromJson(chatMemory.getMessages());
    }

    /**
     * 更新指定会话ID的聊天消息列表
     * 如果该会话已存在则更新消息内容，不存在则插入新记录
     * @param memoryId 会话标识ID
     * @param messages 要存储的聊天消息列表
     */
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String id = String.valueOf(memoryId);
        ListIterator<ChatMessage> iterator = messages.listIterator();
        while (iterator.hasNext()) {
            ChatMessage message = iterator.next();

            // 检查是否为用户类型的消息
            if (message.type() == ChatMessageType.USER) {
                String content = ((UserMessage) message).singleText();

                // 查找并截取
                String keyword = "Answer using the following information";
                int keywordIndex = content.indexOf(keyword);

                if (keywordIndex > 0) {
                    String truncatedContent = content.substring(0, keywordIndex).trim();
                    // 替换当前消息
                    iterator.set(UserMessage.from(truncatedContent));
                }
            }
        }
        String json = ChatMessageSerializer.messagesToJson(messages);

        ChatMemory existing = chatMemoryMapper.selectById(id);
        if (existing != null) {
            LambdaUpdateWrapper<ChatMemory> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(ChatMemory::getMemoryId, id)
                    .set(ChatMemory::getMessages, json);
            chatMemoryMapper.update(null, updateWrapper);
        } else {
            // 新记录：kbId和chatTitle由上层（UI）在首次发送消息前创建
            ChatMemory chatMemory = new ChatMemory();
            chatMemory.setMemoryId(id);
            chatMemory.setUserName(UserInfoHelper.getUserIdFromSession());
            chatMemory.setMessages(json);
            chatMemoryMapper.insert(chatMemory);
        }
    }

    /**
     * 删除指定会话ID的聊天消息记录
     * @param memoryId 会话标识ID
     */
    @Override
    public void deleteMessages(Object memoryId) {
        String id = String.valueOf(memoryId);
        chatMemoryMapper.deleteById(id);
    }
}