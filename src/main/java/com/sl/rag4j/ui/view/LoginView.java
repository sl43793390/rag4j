package com.sl.rag4j.ui.view;

import cn.hutool.crypto.SmUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sl.rag4j.config.UserInfoHelper;
import com.sl.rag4j.entity.User;
import com.sl.rag4j.mapper.UserMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * 自定义全页面登录视图，提供用户名/密码输入和登录按钮
 * 登录成功后由Spring Security重定向到知识库列表页面
 */
@Route("login")
@AnonymousAllowed
public class LoginView extends VerticalLayout {

    private final TextField usernameField;
    private final PasswordField passwordField;
    private final Button loginButton;
    private final UserMapper userMapper;

    /**
     * 构造登录页面，创建居中布局的登录表单
     */
    public LoginView(UserMapper userMapper) {
        this.userMapper = userMapper;
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);

        // 页面标题
        H1 title = new H1("rag4j 登录");
        title.getStyle().set("margin-bottom", "0");

        // 用户名输入框
        usernameField = new TextField("用户名");
        usernameField.setPlaceholder("请输入用户名");
        usernameField.setRequired(true);
        usernameField.setWidth("300px");

        // 密码输入框
        passwordField = new PasswordField("密码");
        passwordField.setPlaceholder("请输入密码");
        passwordField.setRequired(true);
        passwordField.setWidth("300px");

        // 登录按钮
        loginButton = new Button("登录", e -> handleLogin());
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidth("300px");

        // 将表单元素放入垂直布局
        VerticalLayout formLayout = new VerticalLayout(title, usernameField, passwordField, loginButton);
        formLayout.setAlignItems(Alignment.CENTER);
        formLayout.setPadding(false);
        formLayout.setSpacing(true);
        formLayout.getStyle()
                .set("background", "#f8f9fa")
                .set("padding", "40px")
                .set("border-radius", "8px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)");

        add(formLayout);
    }

    /**
     * 处理登录请求：查询数据库用户信息，SM3加密比对密码，成功后存入会话
     */
    private void handleLogin() {
        String username = usernameField.getValue();
        String password = passwordField.getValue();

        if (username.isBlank() || password.isBlank()) {
            Notification.show("用户名和密码不能为空", 3000, Notification.Position.MIDDLE);
            return;
        }

        // 在数据库中查询用户信息
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUserId, username);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            Notification.show("用户不存在", 3000, Notification.Position.MIDDLE);
            return;
        }

        // 使用SM3加密用户输入的密码，与数据库中存储的密码比对
        String encryptedPassword = SmUtil.sm3(password);
        if (!encryptedPassword.equals(user.getPassword())) {
            Notification.show("密码错误", 3000, Notification.Position.MIDDLE);
            return;
        }

        // 检查用户状态
        if (user.getFlagStatus() != null && user.getFlagStatus() == 0) {
            Notification.show("账号已被禁用", 3000, Notification.Position.MIDDLE);
            return;
        }

        // 登录成功，将用户信息放入会话
        UserInfoHelper.saveUserToSession(user);

        Notification.show("登录成功", 2000, Notification.Position.BOTTOM_END);

        // 跳转到首页
        UI.getCurrent().navigate("know");
    }
}