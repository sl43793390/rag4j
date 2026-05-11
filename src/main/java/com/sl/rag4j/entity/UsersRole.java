package com.sl.rag4j.entity;

import java.io.Serializable;
import java.util.Date;

public class UsersRole implements Serializable {
    private String userId;
    private String roleId;

    private Integer version;

    private Date timestamp = new Date();

    private static final long serialVersionUID = 1L;

    
    public UsersRole() {
		super();
		// TODO Auto-generated constructor stub
	}

	public UsersRole(String userId, String roleId) {
		super();
		this.userId = userId;
		this.roleId = roleId;
	}

	public UsersRole(String userId, String roleId, Integer version) {
		super();
		this.userId = userId;
		this.roleId = roleId;
		this.version = version;
	}

	public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId == null ? null : userId.trim();
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId == null ? null : roleId.trim();
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}