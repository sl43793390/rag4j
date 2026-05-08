package com.sl.rag4j.config;

import com.sl.rag4j.mapper.UserMapper;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * 登录成功事件监听器，在Spring Security认证成功后自动触发
 * 将登录用户的完整信息从数据库查询后保存到VaadinSession中
 * 这样其他视图和组件可以通过UserInfoHelper获取当前用户信息
 */
@Component
public class LoginSuccessListener implements ApplicationListener<AuthenticationSuccessEvent> {

    private final UserMapper userMapper;

    /**
     * 构造监听器，注入UserMapper用于查询用户信息
     */
    public LoginSuccessListener(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 处理登录成功事件：从数据库查询完整用户信息，保存到VaadinSession
     * Spring Security认证成功后仅保存了用户名和角色，需要补充完整的用户实体信息
     */
    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        // 将用户信息保存到VaadinSession，供UI组件使用
        UserInfoHelper.loadAndSaveUser(username, userMapper);
    }
}