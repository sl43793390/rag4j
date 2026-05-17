package com.sl.rag4j.ui.view;

import com.sl.rag4j.entity.Permission;
import com.sl.rag4j.entity.Role;
import com.sl.rag4j.entity.User;
import com.sl.rag4j.user.service.UserManageMentProcess;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import jakarta.annotation.security.PermitAll;

import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Route("roles-mgmt")
@PageTitle("Roles Management")
@Menu(order = 31, icon = "vaadin:user-star", title = "角色管理")
@PermitAll
public class RolesMgmtView extends VerticalLayout {

    private UserManageMentProcess userManageMentProcess ;
    
    private TextField searchField;
    private Button searchBtn;
    private Button addRoleBtn;
    private Grid<Role> roleGrid;
    private ListDataProvider<Role> roleDataProvider;
    private User currentUser;
    
    private static final String DEFAULT_INSTITUTION_ID = "DEFAULT_INST";

    public RolesMgmtView(UserManageMentProcess userManageMentProcess) {
        currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
        if (null == currentUser){
            UI.getCurrent().navigate("login");
        }
        this.userManageMentProcess = userManageMentProcess;
        // 初始化UI组件
        initView();
        
        // 加载数据
        refreshData();
    }

    private void initView() {
        // 创建搜索区域
        HorizontalLayout searchLayout = new HorizontalLayout();
        searchField = new TextField("角色ID/名称搜索");
        searchBtn = new Button("搜索");
        addRoleBtn = new Button("新增角色");
        
        Button backToKbBtn = new Button("返回知识库");

        searchBtn.addClickListener(e -> searchData());
        addRoleBtn.addClickListener(e -> openAddRoleDialog());
        addRoleBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        backToKbBtn.addClickListener(e -> UI.getCurrent().navigate("know"));

        searchLayout.add(searchField, searchBtn, addRoleBtn, backToKbBtn);
        searchLayout.setDefaultVerticalComponentAlignment(Alignment.END);
        searchLayout.setSpacing(true);
        
        // 创建角色表格
        roleGrid = new Grid<>(Role.class, false);
        roleGrid.addColumn(Role::getRoleId).setHeader("角色ID").setAutoWidth(true);
        roleGrid.addColumn(Role::getRoleName).setHeader("角色名称").setAutoWidth(true);
        roleGrid.addColumn(Role::getRoleDesc).setHeader("角色描述").setAutoWidth(true);
        roleGrid.addColumn(Role::getIdInstitution).setHeader("机构ID").setAutoWidth(true);
        roleGrid.addColumn(role -> role.getTimestamp() != null ? 
                role.getTimestamp().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
                .setHeader("创建时间").setAutoWidth(true);
        
        // 添加关联权限列
        roleGrid.addComponentColumn(role -> {
            Button permissionBtn = new Button("关联权限");
            permissionBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            permissionBtn.addClickListener(e -> openPermissionAssignmentDialog(role));
            return permissionBtn;
        }).setHeader("关联权限").setAutoWidth(true);
        
        // 添加操作列
        roleGrid.addComponentColumn(role -> {
            Button editBtn = new Button("修改");
            editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            editBtn.addClickListener(e -> openEditRoleDialog(role));
            return editBtn;
        }).setHeader("修改").setAutoWidth(true);
        
        roleGrid.addComponentColumn(role -> {
            Button deleteBtn = new Button("删除");
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteBtn.addClickListener(e -> openDeleteConfirmationDialog(role));
            return deleteBtn;
        }).setHeader("删除").setAutoWidth(true);
        
        roleGrid.setAllRowsVisible(true);
        add(searchLayout, roleGrid);
        setSizeFull();
        roleGrid.setSizeFull();
    }
    
    private void refreshData() {
        List<Role> roles = userManageMentProcess.getAllRoles();
        roleDataProvider = new ListDataProvider<>(roles);
        roleGrid.setDataProvider(roleDataProvider);
    }
    
    private void searchData() {
        String keyword = searchField.getValue();
        if (keyword == null || keyword.trim().isEmpty()) {
            refreshData();
            return;
        }
        
        List<Role> roles = userManageMentProcess.getAllRoles().stream()
                .filter(role -> role.getRoleId().contains(keyword) || 
                               (role.getRoleName() != null && role.getRoleName().contains(keyword)))
                .collect(Collectors.toList());
        roleDataProvider = new ListDataProvider<>(roles);
        roleGrid.setDataProvider(roleDataProvider);
    }
    
    private void openAddRoleDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("30%");
        dialog.setModal(true);
        dialog.setHeaderTitle("新增角色");
        
        // 创建表单组件
        FormLayout formLayout = new FormLayout();
        
        TextField roleIdField = new TextField("角色ID");
        TextField roleNameField = new TextField("角色名称");
        TextField institutionIdField = new TextField("机构ID");
        institutionIdField.setValue(DEFAULT_INSTITUTION_ID);
        TextArea roleDescField = new TextArea("角色描述");

        formLayout.add(roleIdField, roleNameField, institutionIdField, roleDescField);
        
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        formLayout.setColspan(roleIdField, 2);
        formLayout.setColspan(roleNameField, 2);
        formLayout.setColspan(institutionIdField, 2);
        formLayout.setColspan(roleDescField, 2);
        
        // 创建按钮布局
        Button saveButton = new Button("保存", e -> {
            Role newRole = new Role();
            newRole.setRoleId(roleIdField.getValue());
            newRole.setRoleName(roleNameField.getValue());
            newRole.setIdInstitution(institutionIdField.getValue());
            newRole.setRoleDesc(roleDescField.getValue());
            
            newRole.setTimestamp(new java.sql.Timestamp(System.currentTimeMillis()));

            try {
                userManageMentProcess.insertRole(newRole);
                Notification notification = Notification.show("角色创建成功");
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                refreshData();
            } catch (Exception ex) {
                Notification notification = Notification.show("角色创建失败: " + ex.getMessage());
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        Button cancelButton = new Button("取消", e -> dialog.close());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setSpacing(true);
        
        VerticalLayout dialogLayout = new VerticalLayout(formLayout, buttonLayout);
        dialogLayout.setSpacing(false);
        dialogLayout.setPadding(false);
        
        dialog.add(dialogLayout);
        dialog.open();
    }
    
    private void openEditRoleDialog(Role role) {
        Dialog dialog = new Dialog();
        dialog.setWidth("30%");
        dialog.setModal(true);
        dialog.setHeaderTitle("修改角色");
        
        // 创建表单组件
        FormLayout formLayout = new FormLayout();
        
        // 角色ID不能修改
        TextField roleIdField = new TextField("角色ID");
        roleIdField.setValue(role.getRoleId());
        roleIdField.setReadOnly(true);
        
        TextField roleNameField = new TextField("角色名称");
        roleNameField.setValue(role.getRoleName() != null ? role.getRoleName() : "");
        
        TextField institutionIdField = new TextField("机构ID");
        institutionIdField.setValue(role.getIdInstitution() != null ? role.getIdInstitution() : "");
        institutionIdField.setReadOnly(true); // 机构ID通常也不应修改
        
        TextArea roleDescField = new TextArea("角色描述");
        roleDescField.setValue(role.getRoleDesc() != null ? role.getRoleDesc() : "");

        formLayout.add(roleIdField, roleNameField, institutionIdField, roleDescField);
        
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        formLayout.setColspan(roleIdField, 2);
        formLayout.setColspan(roleNameField, 2);
        formLayout.setColspan(institutionIdField, 2);
        formLayout.setColspan(roleDescField, 2);
        
        // 创建按钮布局
        Button saveButton = new Button("保存", e -> {
            // 更新角色信息，但不修改角色ID和机构ID
            role.setRoleName(roleNameField.getValue());
            role.setRoleDesc(roleDescField.getValue());
            
            try {
                userManageMentProcess.insertRole(role); // RoleMapper中的updateByPrimaryKeySelective方法
                Notification notification = Notification.show("角色更新成功");
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                refreshData();
            } catch (Exception ex) {
                Notification notification = Notification.show("角色更新失败: " + ex.getMessage());
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        Button cancelButton = new Button("取消", e -> dialog.close());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setSpacing(true);
        
        VerticalLayout dialogLayout = new VerticalLayout(formLayout, buttonLayout);
        dialogLayout.setSpacing(false);
        dialogLayout.setPadding(false);
        
        dialog.add(dialogLayout);
        dialog.open();
    }
    
    private void openDeleteConfirmationDialog(Role role) {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("确认删除");
        confirmDialog.setText("确定要删除角色 \"" + role.getRoleName() + "\" 吗？此操作不可撤销。");
        
        confirmDialog.setConfirmButton("删除", e -> {
            try {
                userManageMentProcess.deleteRole(role.getRoleId());
                Notification notification = Notification.show("角色删除成功");
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshData();
            } catch (Exception ex) {
                Notification notification = Notification.show("角色删除失败: " + ex.getMessage());
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmDialog.setCancelButton("取消", e -> {});
        confirmDialog.open();
    }
    
    private void openPermissionAssignmentDialog(Role role) {
        Dialog dialog = new Dialog();
        dialog.setWidth("30%");
        dialog.setHeight("60%");
        dialog.setModal(true);
        dialog.setHeaderTitle("关联权限 - 角色: " + role.getRoleName() + " (" + role.getRoleId() + ")");
        
        // 获取所有权限
        List<Permission> allPermissions = userManageMentProcess.selectAllPermissions();
        
        // 获取角色当前已关联的权限ID
        List<String> rolePermissionIds = userManageMentProcess.selectPermissionIdsByRoleId(role.getRoleId());
        
        // 用于跟踪选中的权限
        Set<String> selectedPermissionIds = new HashSet<>(rolePermissionIds);
        
        // 创建权限树形表格
        TreeGrid<Permission> permissionTreeGrid = new TreeGrid<>(Permission.class, false);
        permissionTreeGrid.setWidth("100%");
        permissionTreeGrid.setHeight("463px");
        
        // 添加复选框列
        permissionTreeGrid.addComponentColumn(permission -> {
            Checkbox checkbox = new Checkbox();
            checkbox.setValue(rolePermissionIds.contains(permission.getPermissionId()));
            checkbox.addValueChangeListener(e -> {
                if (e.getValue()) {
                    selectedPermissionIds.add(permission.getPermissionId());
                } else {
                    selectedPermissionIds.remove(permission.getPermissionId());
                }
            });
            return checkbox;
        }).setHeader("选择").setAutoWidth(true);
        
        // 添加权限ID列
//        permissionTreeGrid.addColumn(Permission::getPermissionId).setHeader("权限ID").setAutoWidth(true);
        
        // 添加资源名称列
        permissionTreeGrid.addHierarchyColumn(Permission::getResourceName).setHeader("资源名称").setAutoWidth(true);
        
        // 添加操作列
        permissionTreeGrid.addColumn(Permission::getAction).setHeader("操作").setAutoWidth(true);
        
        // 构建树形数据
        TreeData<Permission> treeData = new TreeData<>();
        
        // 先添加根节点（没有父节点的权限）
        List<Permission> rootPermissions = allPermissions.stream()
                .filter(p -> p.getIdParent() == null || p.getIdParent().isEmpty())
                .collect(Collectors.toList());
        
        // 添加所有根节点
        treeData.addRootItems(rootPermissions);
        
        // 递归添加子节点
        addChildrenToTreeData(treeData, allPermissions, rootPermissions);
        
        // 设置数据提供者
        permissionTreeGrid.setDataProvider(new TreeDataProvider<>(treeData));
        
        // 展开所有节点以便查看
        for (Permission rootPermission : rootPermissions) {
            permissionTreeGrid.expand(rootPermission);
        }
        
        // 创建按钮布局
        Button saveButton = new Button("保存");
        Button cancelButton = new Button("取消", e -> dialog.close());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        saveButton.addClickListener(e -> {
            try {
                // 删除角色当前所有权限关联
                userManageMentProcess.deleteRolePermissionByRoleId(role.getRoleId());
                
                // 添加新选择的权限关联
                for (String permissionId : selectedPermissionIds) {
                    userManageMentProcess.insertRolePermission(role.getRoleId(), permissionId);
                }
                
                Notification notification = Notification.show("权限关联更新成功");
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
            } catch (Exception ex) {
                Notification notification = Notification.show("权限关联更新失败: " + ex.getMessage());
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setSpacing(true);
        buttonLayout.setPadding(false);
        
        VerticalLayout dialogLayout = new VerticalLayout(permissionTreeGrid, buttonLayout);
        dialogLayout.setSpacing(false);
        dialogLayout.setPadding(false);
        
        dialog.add(dialogLayout);
        dialog.open();
    }
    
    /**
     * 递归添加子节点到TreeData中
     * @param treeData 树形数据
     * @param allPermissions 所有权限列表
     * @param parentPermissions 父权限列表
     */
    private void addChildrenToTreeData(TreeData<Permission> treeData, List<Permission> allPermissions, List<Permission> parentPermissions) {
        for (Permission parent : parentPermissions) {
            // 查找当前权限的子权限
            List<Permission> children = allPermissions.stream()
                    .filter(p -> p.getIdParent() != null && p.getIdParent().equals(parent.getPermissionId()))
                    .collect(Collectors.toList());
            
            // 添加子节点
            treeData.addItems(parent, children);
            
            // 递归添加子节点的子节点
            if (!children.isEmpty()) {
                addChildrenToTreeData(treeData, allPermissions, children);
            }
        }
    }
}