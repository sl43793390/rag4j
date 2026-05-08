package com.sl.rag4j.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.sl.rag4j.entity.User;
import com.sl.rag4j.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 数据库相关配置，包含MyBatis-Plus分页插件、Spring Security用户认证和密码编码器
 */
@Configuration
public class DataConfig {

    /**
     * MyBatis-Plus分页拦截器，配置SQLite方言以支持分页查询
     * 在Vaadin Grid等组件中使用分页功能时需要此配置
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.SQLITE));
        return interceptor;
    }

    /**
     * Spring Security用户详情服务，从SQLite数据库查询用户信息
     * 通过MyBatis-Plus的LambdaQueryWrapper按用户名查询
     */
    @Bean
    public UserDetailsService userDetailsService(UserMapper userMapper) {
        return username -> {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, username);
            User user = userMapper.selectOne(wrapper);
            if (user == null) {
                throw new UsernameNotFoundException("用户不存在: " + username);
            }
            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getUsername())
                    .password(user.getPassword())
                    .roles(user.getRole())
                    .build();
        };
    }

    /**
     * BCrypt密码编码器，用于用户密码的加密存储和验证
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}