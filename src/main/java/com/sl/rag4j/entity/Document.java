package com.sl.rag4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("document")
public class Document {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long knowledgeBaseId;
    private String fileName;
    private String fileType;
    private Integer chunkCount;
    private String createdAt;
    private String fileSize;

    public Document() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(Long knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    @Override
    public String toString() {
        return "Document{id=" + id + ", fileName='" + fileName + "', chunkCount=" + chunkCount + "}";
    }
}