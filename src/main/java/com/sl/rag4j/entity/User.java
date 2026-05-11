package com.sl.rag4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

@TableName("users")
public class User {

    @TableId(type = IdType.ASSIGN_ID)
    private String userId;
    private String userName;
    private String password;
    private String role;
    /** 用户类型：ADMIN/USER/GUEST */
    private String userType;
    /** 状态标识：1启用 0禁用 */
    private Integer flagStatus;
    private String email;
    private String cdPhone;
    private String department;//
    private Date expireTime;//
    private String organization;//
    private String idInstitution;//
    private Date createTime;//

    public User() {}

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getIdInstitution() {
        return idInstitution;
    }

    public void setIdInstitution(String idInstitution) {
        this.idInstitution = idInstitution;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

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

    public String getCdPhone() {
        return cdPhone;
    }

    public void setCdPhone(String cdPhone) {
        this.cdPhone = cdPhone;
    }
}