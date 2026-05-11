package com.sl.rag4j.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

@TableName("permissions")
public class Permission implements Serializable {
    private String permissionId;//Ȩ��ID

    private String resourceName;//��Դ��

    private String action;//������

    private Double version;//�汾

    private Date timestamp;//ʱ���

    private String idParent;//��id

    private Integer nbrLevel;//�㼶

    private Long nbrOrder;//���

    private static final long serialVersionUID = 1L;

    public String getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(String permissionId) {
        this.permissionId = permissionId == null ? null : permissionId.trim();
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName == null ? null : resourceName.trim();
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action == null ? null : action.trim();
    }

    public Double getVersion() {
        return version;
    }

    public void setVersion(Double version) {
        this.version = version;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getIdParent() {
        return idParent;
    }

    public void setIdParent(String idParent) {
        this.idParent = idParent == null ? null : idParent.trim();
    }

    public Integer getNbrLevel() {
        return nbrLevel;
    }

    public void setNbrLevel(Integer nbrLevel) {
        this.nbrLevel = nbrLevel;
    }

    public Long getNbrOrder() {
        return nbrOrder;
    }

    public void setNbrOrder(Long nbrOrder) {
        this.nbrOrder = nbrOrder;
    }

}