package com.sl.rag4j.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 聊天记忆实体，对应chat_memory表
 * 存储按会话ID分组的聊天消息JSON序列化数据
 */
@TableName("chat_memory")
public class ChatMemory {

    @TableId
    private String memoryId;
    private String userName;
    private String messages;
    private Long kbId;
    private String chatTitle;

    public ChatMemory() {}

    public String getMemoryId() { return memoryId; }
    public void setMemoryId(String memoryId) { this.memoryId = memoryId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getMessages() { return messages; }
    public void setMessages(String messages) { this.messages = messages; }

    public Long getKbId() {
        return kbId;
    }

    public void setKbId(Long kbId) {
        this.kbId = kbId;
    }

    public String getChatTitle() {
        return chatTitle;
    }

    public void setChatTitle(String chatTitle) {
        this.chatTitle = chatTitle;
    }
}