package com.sl.rag4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("users")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String role;
    /** 用户类型：ADMIN/USER/GUEST */
    private String userType;
    /** 状态标识：1启用 0禁用 */
    private Integer flagStatus;
    private String email;
    private String phone;
    private String createdAt;
    private String updatedAt;

    public User() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public Integer getFlagStatus() { return flagStatus; }
    public void setFlagStatus(Integer flagStatus) { this.flagStatus = flagStatus; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', userType='" + userType + "', flagStatus=" + flagStatus + "}";
    }
}