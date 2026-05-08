package com.sl.rag4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("knowledge_base")
public class KnowledgeBase {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String splitterType;
    private String embeddingModelName;
    private String milvusCollectionName;
    private Integer maxSegmentSize;
    private Integer maxOverlapSize;
    private Integer docCount;
    private String userName;
    private String createdAt;
    private String updatedAt;

    public KnowledgeBase() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSplitterType() { return splitterType; }
    public void setSplitterType(String splitterType) { this.splitterType = splitterType; }
    public String getEmbeddingModelName() { return embeddingModelName; }
    public void setEmbeddingModelName(String embeddingModelName) { this.embeddingModelName = embeddingModelName; }
    public String getMilvusCollectionName() { return milvusCollectionName; }
    public void setMilvusCollectionName(String milvusCollectionName) { this.milvusCollectionName = milvusCollectionName; }
    public Integer getMaxSegmentSize() { return maxSegmentSize; }
    public void setMaxSegmentSize(Integer maxSegmentSize) { this.maxSegmentSize = maxSegmentSize; }
    public Integer getMaxOverlapSize() { return maxOverlapSize; }
    public void setMaxOverlapSize(Integer maxOverlapSize) { this.maxOverlapSize = maxOverlapSize; }
    public Integer getDocCount() { return docCount; }
    public void setDocCount(Integer docCount) { this.docCount = docCount; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String toString() {
        return "KnowledgeBase{id=" + id + ", name='" + name + "', splitterType='" + splitterType + "'}";
    }
}