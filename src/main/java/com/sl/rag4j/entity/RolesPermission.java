package com.sl.rag4j.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
@TableName("roles_permissions")
public class RolesPermission implements Serializable {
    private String roleId;

    private String permissionId;

    private Integer version;

    private Date timestamp = new Date();

    private static final long serialVersionUID = 1L;

    
    public RolesPermission(String roleId, String permissionId, Integer version, Date timestamp) {
		super();
		this.roleId = roleId;
		this.permissionId = permissionId;
		this.version = version;
		this.timestamp = timestamp;
	}

	public RolesPermission(String roleId, String permissionId, Integer version) {
		super();
		this.roleId = roleId;
		this.permissionId = permissionId;
		this.version = version;
	}

	public RolesPermission(String roleId, String permissionId) {
		super();
		this.roleId = roleId;
		this.permissionId = permissionId;
	}

	public RolesPermission() {
		super();
		// TODO Auto-generated constructor stub
	}

	public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId == null ? null : roleId.trim();
    }

    public String getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(String permissionId) {
        this.permissionId = permissionId == null ? null : permissionId.trim();
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