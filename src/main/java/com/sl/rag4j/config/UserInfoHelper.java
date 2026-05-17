package com.sl.rag4j.config;

import com.sl.rag4j.entity.User;
import com.sl.rag4j.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vaadin.flow.server.VaadinSession;

/**
 * 用户会话信息工具类，管理登录用户信息在VaadinSession中的存取
 * 用户登录成功后，将用户实体信息保存到VaadinSession中
 * 其他视图和组件通过此工具类获取当前登录用户信息，避免每次查询数据库
 */
public class UserInfoHelper {

    /** VaadinSession中存储用户信息的属性键名 */
    private static final String USER_SESSION_KEY = "currentUser";

    /**
     * 将用户信息保存到当前VaadinSession中
     * 在登录成功后调用，将完整的User实体存入会话
     * @param user 登录成功的用户实体
     */
    public static void saveUserToSession(User user) {
        VaadinSession session = VaadinSession.getCurrent();
        session.setAttribute(USER_SESSION_KEY, user);
    }

    /**
     * 从当前VaadinSession中获取登录用户信息
     * 在视图和组件中调用，获取当前登录用户的完整信息
     * @return 当前登录用户实体，如果会话中没有则返回null
     */
    public static User getUserFromSession() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            return (User) session.getAttribute(USER_SESSION_KEY);
        }
        return null;
    }

    /**
     * 从当前VaadinSession中获取登录用户ID
     * 在创建知识库等需要关联用户的操作中调用
     * @return 当前登录用户ID，如果会话中没有用户信息则返回null
     */
    public static String getUserIdFromSession() {
        User user = getUserFromSession();
        return user != null ? user.getUserName() : null;
    }

    /**
     * 从当前VaadinSession中获取登录用户名
     * 在界面显示和日志记录中调用
     * @return 当前登录用户名，如果会话中没有则返回"unknown"
     */
    public static String getUsernameFromSession() {
        User user = getUserFromSession();
        return user != null ? user.getUserName() : "unknown";
    }

    /**
     * 根据Spring Security认证信息从数据库查询用户，并保存到VaadinSession
     * 在登录成功后的监听器中调用，确保会话中有完整的用户信息
     * @param username Spring Security认证后的用户名
     * @param userMapper 用户数据库Mapper
     */
    public static void loadAndSaveUser(String username, UserMapper userMapper) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUserName, username);
        User user = userMapper.selectOne(wrapper);
        if (user != null) {
            saveUserToSession(user);
        }
    }

    /**
     * 清除当前VaadinSession中的用户信息
     * 在用户注销时调用
     */
    public static void clearUserFromSession() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(USER_SESSION_KEY, null);
        }
    }
}