package com.sl.rag4j.ui.view;

import com.sl.rag4j.entity.Role;
import com.sl.rag4j.entity.User;
import com.sl.rag4j.mapper.UserMapper;
import com.sl.rag4j.user.service.UserManageMentProcess;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Route("user-mgmt")
@PageTitle("User Management")
@Menu(order = 30, icon = "vaadin:user", title = "用户管理")
@PermitAll
public class UserMgmtView extends VerticalLayout {

    private UserMapper userMapper;
    
    private UserManageMentProcess userManageMentProcess;
    
    private TextField searchField;
    private Button searchBtn;
    private Button addUserBtn;
    private Grid<User> userGrid;
    private ListDataProvider<User> userDataProvider;
    private User currentUser;

    @Autowired
    public UserMgmtView(UserMapper userMapper, UserManageMentProcess userManageMentProcess) {
        currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
        if (null == currentUser){
            UI.getCurrent().navigate("login");
        }
        this.userMapper = userMapper;
        this.userManageMentProcess = userManageMentProcess;
        // 初始化UI组件
        initView();
        
        // 加载数据
        refreshData();
    }

    private void initView() {
        // 创建搜索区域
        HorizontalLayout searchLayout = new HorizontalLayout();
        searchField = new TextField("用户ID搜索");
        searchBtn = new Button("搜索");
        addUserBtn = new Button("新增用户");
        Button roleMgmtBtn = new Button("角色管理");
        
        searchBtn.addClickListener(e -> searchData());
        addUserBtn.addClickListener(e -> openAddUserDialog());
        addUserBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        roleMgmtBtn.addClickListener(e -> UI.getCurrent().navigate("roles-mgmt"));

        searchLayout.add(searchField, searchBtn, addUserBtn, roleMgmtBtn);
        searchLayout.setDefaultVerticalComponentAlignment(Alignment.END);
        searchLayout.setSpacing(true);
        
        // 创建用户表格
        userGrid = new Grid<>(User.class, false);

        userGrid.addColumn(User::getUserId).setHeader("用户ID").setAutoWidth(true);
        userGrid.addColumn(User::getUserName).setHeader("用户名").setAutoWidth(true);
        userGrid.addColumn(User::getEmail).setHeader("邮箱").setAutoWidth(true);
        userGrid.addColumn(User::getDepartment).setHeader("部门").setAutoWidth(true);
        userGrid.addColumn(User::getOrganization).setHeader("组织").setAutoWidth(true);
        userGrid.addColumn(User::getIdInstitution).setHeader("机构ID").setAutoWidth(true);
        userGrid.addColumn(user -> user.getCreateTime() != null ? 
                user.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null)
                .setHeader("创建时间").setAutoWidth(true);
        
        // 添加关联角色列
        userGrid.addComponentColumn(user -> {
            Button roleBtn = new Button("关联角色");
            roleBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            roleBtn.addClickListener(e -> openRoleAssignmentDialog(user));
            return roleBtn;
        }).setHeader("关联角色").setAutoWidth(true);
        
        // 添加操作列
        userGrid.addComponentColumn(user -> {
            Button editBtn = new Button("修改");
            editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            editBtn.addClickListener(e -> openEditUserDialog(user));
            return editBtn;
        }).setHeader("修改").setAutoWidth(true);
        
        userGrid.addComponentColumn(user -> {
            Button deleteBtn = new Button("删除");
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteBtn.addClickListener(e -> openDeleteConfirmationDialog(user));
            return deleteBtn;
        }).setHeader("删除").setAutoWidth(true);
        add(searchLayout, userGrid);
        setSizeFull();
        userGrid.setSizeFull();
    }
    
    private void refreshData() {
        List<User> users = userManageMentProcess.selectAllUser();
        userGrid.setItems(users);
    }
    
    private void searchData() {
        String keyword = searchField.getValue();
        if (keyword == null || keyword.trim().isEmpty()) {
            refreshData();
            return;
        }
        
        List<User> users = userManageMentProcess.selectAllUser().stream()
                .filter(user -> user.getUserId().contains(keyword))
                .collect(Collectors.toList());
        userGrid.setItems(users);
    }
    
    private void openAddUserDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("50%");
        dialog.setModal(true);
        dialog.setHeaderTitle("新增用户");
        
        // 创建表单组件
        FormLayout formLayout = new FormLayout();
        
        TextField userIdField = new TextField("用户ID");
        TextField userNameField = new TextField("用户名");
        EmailField emailField = new EmailField("邮箱");
        PasswordField passwordField = new PasswordField("密码");
        TextField departmentField = new TextField("部门");
        TextField organizationField = new TextField("组织");
        TextField institutionIdField = new TextField("机构ID");
        DatePicker createTimePicker = new DatePicker("创建时间");
        DatePicker expireTimePicker = new DatePicker("过期时间");
        TextField phoneField = new TextField("电话");
        
        formLayout.add(userIdField, userNameField, emailField, passwordField, 
                departmentField, organizationField, institutionIdField, 
                createTimePicker, expireTimePicker, phoneField);
        
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        formLayout.setColspan(userIdField, 2);
        formLayout.setColspan(userNameField, 2);
        formLayout.setColspan(emailField, 2);
        formLayout.setColspan(passwordField, 2);
        
        // 创建按钮布局
        Button saveButton = new Button("保存", e -> {
            User newUser = new User();
            newUser.setUserId(userIdField.getValue());
            newUser.setUserName(userNameField.getValue());
            newUser.setEmail(emailField.getValue());
            newUser.setPassword(userManageMentProcess.getPassword(passwordField.getValue()));
            newUser.setDepartment(departmentField.getValue());
            newUser.setOrganization(organizationField.getValue());
            newUser.setIdInstitution(institutionIdField.getValue());
            newUser.setCdPhone(phoneField.getValue());
            
            if (createTimePicker.getValue() != null) {
                newUser.setCreateTime(Timestamp.valueOf(createTimePicker.getValue().atStartOfDay()));
            }
            
            if (expireTimePicker.getValue() != null) {
                newUser.setExpireTime(Timestamp.valueOf(expireTimePicker.getValue().atStartOfDay()));
            }
            
            try {
                userManageMentProcess.insertSelective(newUser);
                Notification notification = Notification.show("用户创建成功");
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                refreshData();
            } catch (Exception ex) {
                Notification notification = Notification.show("用户创建失败: " + ex.getMessage());
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        Button cancelButton = new Button("取消", e -> dialog.close());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setAlignItems(Alignment.END);
        buttonLayout.setSpacing(true);
        
        VerticalLayout dialogLayout = new VerticalLayout(formLayout, buttonLayout);
        dialogLayout.setSpacing(false);
        dialogLayout.setPadding(false);
        
        dialog.add(dialogLayout);
        dialog.open();
    }
    
    private void openEditUserDialog(User user) {
        Dialog dialog = new Dialog();
        dialog.setWidth("50%");
        dialog.setModal(true);
        dialog.setHeaderTitle("修改用户");
        
        // 创建表单组件
        FormLayout formLayout = new FormLayout();
        
        // 用户ID不能修改
        TextField userIdField = new TextField("用户ID");
        userIdField.setValue(user.getUserId());
        userIdField.setReadOnly(true);
        
        TextField userNameField = new TextField("用户名");
        userNameField.setValue(user.getUserName() != null ? user.getUserName() : "");
        
        EmailField emailField = new EmailField("邮箱");
        emailField.setValue(user.getEmail() != null ? user.getEmail() : "");
        
        PasswordField passwordField = new PasswordField("密码");
        // 密码通常不显示原值，留空表示不修改
        
        TextField departmentField = new TextField("部门");
        departmentField.setValue(user.getDepartment() != null ? user.getDepartment() : "");
        
        TextField organizationField = new TextField("组织");
        organizationField.setValue(user.getOrganization() != null ? user.getOrganization() : "");
        
        TextField institutionIdField = new TextField("机构ID");
        institutionIdField.setValue(user.getIdInstitution() != null ? user.getIdInstitution() : "");
        
        DatePicker createTimePicker = new DatePicker("创建时间");
        if (user.getCreateTime() != null) {
            createTimePicker.setValue(user.getCreateTime().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate());
        }
        
        DatePicker expireTimePicker = new DatePicker("过期时间");
        if (user.getExpireTime() != null) {
            expireTimePicker.setValue(user.getExpireTime().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate());
        }
        
        TextField phoneField = new TextField("电话");
        phoneField.setValue(user.getCdPhone() != null ? user.getCdPhone() : "");
        
        formLayout.add(userIdField, userNameField, emailField, passwordField, 
                departmentField, organizationField, institutionIdField, 
                createTimePicker, expireTimePicker, phoneField);
        
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        formLayout.setColspan(userIdField, 2);
        formLayout.setColspan(userNameField, 2);
        formLayout.setColspan(emailField, 2);
        formLayout.setColspan(passwordField, 2);
        
        // 创建按钮布局
        Button saveButton = new Button("保存", e -> {
            // 更新用户信息，但不修改用户ID
            user.setUserName(userNameField.getValue());
            user.setEmail(emailField.getValue());
            
            // 只有当密码字段不为空时才更新密码
            if (!passwordField.getValue().isEmpty()) {
                user.setPassword(userManageMentProcess.getPassword(passwordField.getValue()));
            }
            
            user.setDepartment(departmentField.getValue());
            user.setOrganization(organizationField.getValue());
            user.setIdInstitution(institutionIdField.getValue());
            user.setCdPhone(phoneField.getValue());
            
            if (createTimePicker.getValue() != null) {
                user.setCreateTime(Timestamp.valueOf(createTimePicker.getValue().atStartOfDay()));
            }
            
            if (expireTimePicker.getValue() != null) {
                user.setExpireTime(Timestamp.valueOf(expireTimePicker.getValue().atStartOfDay()));
            }
            
            try {
                userManageMentProcess.updateUserByUserId(user);
                Notification notification = Notification.show("用户更新成功");
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                refreshData();
            } catch (Exception ex) {
                Notification notification = Notification.show("用户更新失败: " + ex.getMessage());
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        Button cancelButton = new Button("取消", e -> dialog.close());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setAlignItems(Alignment.START);
        buttonLayout.setSpacing(true);
        
        VerticalLayout dialogLayout = new VerticalLayout(formLayout, buttonLayout);
        dialogLayout.setSpacing(false);
        dialogLayout.setPadding(false);
        
        dialog.add(dialogLayout);
        dialog.open();
    }
    
    private void openDeleteConfirmationDialog(User user) {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("确认删除");
        confirmDialog.setText("确定要删除用户 \"" + user.getUserId() + "\" 吗？此操作不可撤销。");
        
        confirmDialog.setConfirmButton("删除", e -> {
            try {
                userManageMentProcess.deleteUserById(user.getUserId());
                Notification.show("用户删除成功")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshData();
            } catch (Exception ex) {
                Notification.show("用户删除失败: " + ex.getMessage())
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        confirmDialog.setCancelButton("取消", e -> {});
        
        confirmDialog.setConfirmButtonTheme("error primary");
        confirmDialog.open();
    }
    
    private void openRoleAssignmentDialog(User user) {
        Dialog dialog = new Dialog();
        dialog.setWidth("50%");
        dialog.setModal(true);
        dialog.setHeaderTitle("关联角色 - 用户: " + user.getUserId());
        
        // 获取所有角色
        List<Role> allRoles = userManageMentProcess.getAllRoles();
        
        // 获取用户当前已关联的角色ID
        List<String> userRoleIds = userManageMentProcess.selectRoleIdsByUserId(user.getUserId());
        
        // 用于跟踪选中的角色
        Set<String> selectedRoleIds = new HashSet<>(userRoleIds);
        
        // 创建角色表格
        Grid<Role> roleGrid = new Grid<>(Role.class, false);
        roleGrid.setWidth("100%");
        roleGrid.setHeight("300px");
        
        // 添加复选框列
        roleGrid.addComponentColumn(role -> {
            Checkbox checkbox = new Checkbox();
            checkbox.setValue(userRoleIds.contains(role.getRoleId()));
            checkbox.addValueChangeListener(e -> {
                if (e.getValue()) {
                    selectedRoleIds.add(role.getRoleId());
                } else {
                    selectedRoleIds.remove(role.getRoleId());
                }
            });
            return checkbox;
        }).setHeader("选择").setAutoWidth(true);
        
        // 添加角色ID列
        roleGrid.addColumn(Role::getRoleId).setHeader("角色ID").setAutoWidth(true);
        
        // 添加角色名称列
        roleGrid.addColumn(Role::getRoleName).setHeader("角色名称").setAutoWidth(true);
        
        // 添加角色描述列
        roleGrid.addColumn(Role::getRoleDesc).setHeader("角色描述").setAutoWidth(true);
        
        roleGrid.setItems(allRoles);
        
        // 创建按钮布局
        Button saveButton = new Button("保存");
        Button cancelButton = new Button("取消", e -> dialog.close());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        saveButton.addClickListener(e -> {
            try {
                // 删除用户当前所有角色关联
                userManageMentProcess.deleUserRoleByUserId(user.getUserId());
                
                // 添加新选择的角色关联
                for (String roleId : selectedRoleIds) {
                    userManageMentProcess.insertUserRole(user.getUserId(), roleId);
                }
                
                Notification notification = Notification.show("角色关联更新成功");
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
            } catch (Exception ex) {
                Notification notification = Notification.show("角色关联更新失败: " + ex.getMessage());
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setSpacing(true);
        buttonLayout.setPadding(false);
        
        VerticalLayout dialogLayout = new VerticalLayout(roleGrid, buttonLayout);
        dialogLayout.setSpacing(false);
        dialogLayout.setPadding(false);
        
        dialog.add(dialogLayout);
        dialog.open();
    }
}
