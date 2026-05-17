package com.sl.rag4j.ui.view;

import com.sl.rag4j.entity.KnowledgeBase;
import com.sl.rag4j.entity.Document;
import com.sl.rag4j.mapper.KnowledgeBaseMapper;
import com.sl.rag4j.mapper.DocumentMapper;
import com.sl.rag4j.ragservice.EmbeddingService;
import com.sl.rag4j.config.UserInfoHelper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.*;
import com.sl.rag4j.ui.component.MainLayout;
import com.sl.rag4j.ui.component.CreateKnowledgeBaseDialog;
import com.sl.rag4j.ui.component.CreateKnowledgeBaseDialog.Mode;
import jakarta.annotation.security.PermitAll;

import java.util.List;

/**
 * 知识库列表视图，展示所有已创建的知识库
 * 顶部提供搜索框和新建按钮，下方使用Grid表格展示知识库信息
 * 每行包含知识库名称、切割方式、嵌入模型和查询按钮
 */
@Route(value = "know", layout = MainLayout.class)
@PageTitle("知识库管理")
@PermitAll
public class KnowledgeBaseListView extends VerticalLayout implements BeforeEnterObserver {

    private final KnowledgeBaseMapper kbMapper;
    private final DocumentMapper documentMapper;
    private final EmbeddingService embeddingService;
    private TextField searchField;
    private final Grid<KnowledgeBase> grid;

    /**
     * 构造知识库列表视图，创建搜索栏和知识库表格
     */
    public KnowledgeBaseListView(KnowledgeBaseMapper kbMapper, DocumentMapper documentMapper, EmbeddingService embeddingService) {
        this.kbMapper = kbMapper;
        this.documentMapper = documentMapper;
        this.embeddingService = embeddingService;
        setSizeFull();

        // 顶部搜索栏
        HorizontalLayout topBar = createTopBar();

        // 知识库表格
        grid = createGrid();

        add(topBar, grid);
        setFlexGrow(1, grid);
        refreshGrid();
    }

    /**
     * 创建顶部搜索栏，包含搜索输入框、搜索按钮和新建知识库按钮
     */
    private HorizontalLayout createTopBar() {
        HorizontalLayout topBar = new HorizontalLayout();
        topBar.setWidthFull();
        topBar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        topBar.setPadding(true);

        // 搜索输入框，输入时实时过滤列表
        searchField = new TextField();
        searchField.setPlaceholder("搜索知识库...");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> refreshGrid());

        // 搜索按钮
        Button searchBtn = new Button("搜索", e -> refreshGrid());

        // 新建知识库按钮
        Button createBtn = new Button("新建知识库", e -> openCreateDialog());
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // 用户管理图标按钮
        Button userMgmtBtn = new Button("用户管理",VaadinIcon.USER.create());
        userMgmtBtn.addClickListener(e -> UI.getCurrent().navigate("user-mgmt"));
        userMgmtBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        topBar.add(searchField, searchBtn, createBtn, userMgmtBtn);
        topBar.setFlexGrow(1, searchField);

        return topBar;
    }

    /**
     * 创建知识库表格，配置列定义和查询按钮
     * 表头包含：名称、切割方式、嵌入模型、文档数、查询
     */
    private Grid<KnowledgeBase> createGrid() {
        Grid<KnowledgeBase> grid = new Grid<>(KnowledgeBase.class, false);
        grid.setSizeFull();

        // 知识库名称列
        grid.addColumn(KnowledgeBase::getName).setHeader("知识库名称").setSortable(true);
        // 切割方式列
        grid.addColumn(KnowledgeBase::getSplitterType).setHeader("切割方式");
        // 嵌入模型列
        grid.addColumn(KnowledgeBase::getEmbeddingModelName).setHeader("嵌入模型");
        // 文档数列
        grid.addColumn(kb -> kb.getDocCount() != null ? kb.getDocCount() : 0).setHeader("文档数");

        // 文档详情按钮列：点击后打开文档详情对话框
        grid.addComponentColumn(kb -> {
            Button detailBtn = new Button("详情", e -> openDocumentDetailDialog(kb));
            detailBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
            return detailBtn;
        }).setHeader("文档详情");

        // 查询按钮列：点击后导航到查询对话页面
        grid.addComponentColumn(kb -> {
            Button queryBtn = new Button("查询", e -> navigateToQuery(kb.getId()));
            queryBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            return queryBtn;
        }).setHeader("查询");

        // 编辑按钮列：点击后打开编辑对话框，复用CreateKnowledgeBaseDialog的EDIT模式
        grid.addComponentColumn(kb -> {
            Button editBtn = new Button("编辑", e -> openEditDialog(kb));
            editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
            return editBtn;
        }).setHeader("编辑");

        // 删除按钮列：点击后弹出确认对话框，删除知识库及其所有文档和向量数据
        grid.addComponentColumn(kb -> {
            Button deleteBtn = new Button("删除", e -> openDeleteConfirmDialog(kb));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            return deleteBtn;
        }).setHeader("删除");

        return grid;
    }

    /**
     * 根据搜索关键词刷新知识库表格数据
     * 无关键词时显示所有知识库，有关键词时按名称模糊匹配
     */
    private void refreshGrid() {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        String keyword = searchField.getValue();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(KnowledgeBase::getName, keyword);
        }
        wrapper.orderByDesc(KnowledgeBase::getCreatedAt);
        List<KnowledgeBase> items = kbMapper.selectList(wrapper);
        grid.setItems(items);
    }

    /**
     * 打开创建知识库对话框，通过构造函数传递Spring bean
     */
    private void openCreateDialog() {
        CreateKnowledgeBaseDialog dialog = new CreateKnowledgeBaseDialog(kbMapper, embeddingService);
        dialog.open();
        // 对话框关闭后刷新列表数据
        dialog.addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                refreshGrid();
            }
        });
    }

    /**
     * 打开编辑知识库对话框，传入待编辑的知识库实体
     * 复用CreateKnowledgeBaseDialog的EDIT模式，仅允许修改名称、描述和追加文件
     */
    private void openEditDialog(KnowledgeBase kb) {
        CreateKnowledgeBaseDialog dialog = new CreateKnowledgeBaseDialog(kbMapper, embeddingService, Mode.EDIT, kb);
        dialog.open();
        // 对话框关闭后刷新列表数据
        dialog.addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                refreshGrid();
            }
        });
    }

    /**
     * 打开删除确认对话框，提示用户此操作不可撤销
     * 用户确认后执行删除操作，删除知识库记录、关联文档及Milvus向量集合
     */
    private void openDeleteConfirmDialog(KnowledgeBase kb) {
        Dialog dialog = new Dialog();
        dialog.setWidth("450px");
        dialog.setHeaderTitle("确认删除");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.add(new com.vaadin.flow.component.html.Span("您即将删除知识库："));
        com.vaadin.flow.component.html.Span nameSpan = new com.vaadin.flow.component.html.Span("\"" + kb.getName() + "\"");
        nameSpan.getStyle().set("font-weight", "bold");
        content.add(nameSpan);
        com.vaadin.flow.component.html.Span warning = new com.vaadin.flow.component.html.Span("此操作不可撤销，知识库内的所有文档和向量数据将被永久删除。");
        warning.getStyle().set("color", "var(--lumo-error-color)");
        content.add(warning);

        dialog.add(content);

        HorizontalLayout footer = new HorizontalLayout();
        footer.setPadding(true);
        Button cancelBtn = new Button("取消", e -> dialog.close());
        Button confirmBtn = new Button("删除", e -> {
            deleteKnowledgeBase(kb);
            dialog.close();
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        footer.add(cancelBtn, confirmBtn);
        dialog.getFooter().add(footer);

        dialog.open();
    }

    /**
     * 执行知识库删除逻辑
     * 1. 删除Milvus中的向量集合（如果存在）
     * 2. 删除关联的文档记录
     * 3. 删除知识库记录
     * 4. 刷新列表并显示成功提示
     */
    private void deleteKnowledgeBase(KnowledgeBase kb) {
        try {
            // 删除Milvus向量集合
            String collectionName = kb.getMilvusCollectionName();
            if (collectionName != null && !collectionName.isBlank()) {
                embeddingService.deleteCollection(collectionName);
            }

            // 删除关联的文档记录
            documentMapper.delete(new LambdaQueryWrapper<Document>()
                    .eq(Document::getKnowledgeBaseId, kb.getId()));

            // 删除知识库记录
            kbMapper.deleteById(kb.getId());

            // 刷新列表
            refreshGrid();

            // 显示成功提示
            Notification notification = Notification.show("知识库 \"" + kb.getName() + "\" 已成功删除");
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            // 显示失败提示
            Notification notification = Notification.show("删除知识库失败: " + e.getMessage());
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * 打开文档详情对话框，显示该知识库下的所有文档列表
     * 包含文件名、上传时间、分块数量、文件大小等信息
     */
    private void openDocumentDetailDialog(KnowledgeBase kb) {
        Dialog dialog = new Dialog();
        dialog.setWidth("850px");
        dialog.setHeight("700px");
        dialog.setHeaderTitle("文档详情 - " + kb.getName());

        Grid<Document> docGrid = new Grid<>(Document.class, false);
        docGrid.setSizeFull();
        docGrid.addColumn(Document::getFileName).setHeader("文件名").setSortable(true).setResizable(true).setAutoWidth(true);
        docGrid.addColumn(Document::getFileType).setHeader("文件类型");
        docGrid.addColumn(Document::getChunkCount).setHeader("分块数");
        docGrid.addColumn(Document::getFileSize).setHeader("文件大小");
        docGrid.addColumn(Document::getCreatedAt).setHeader("上传时间").setSortable(true).setResizable(true).setAutoWidth(true);

        // 查询该知识库下的所有文档
        List<Document> docs = documentMapper.selectList(
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getKnowledgeBaseId, kb.getId())
                        .orderByDesc(Document::getCreatedAt)
        );
        docGrid.setItems(docs);

        dialog.add(docGrid);
        Button closeBtn = new Button("关闭", e -> dialog.close());
        dialog.getFooter().add(closeBtn);
        dialog.open();
    }

    /**
     * 导航到查询对话页面，传入知识库ID作为路由参数
     */
    private void navigateToQuery(Long kbId) {
        UI.getCurrent().navigate("query/" + kbId);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (null == UserInfoHelper.getUserFromSession()) {
            event.rerouteTo("login");
            UI.getCurrent().navigate("login");
        }
        // 显示顶部"返回知识库"按钮
        getMainLayout().ifPresent(layout -> layout.getBackToKbBtn().setVisible(false));
    }

    /**
     * 获取当前页面的MainLayout父布局
     */
    private java.util.Optional<MainLayout> getMainLayout() {
        return UI.getCurrent().getInternals().getActiveRouterTargetsChain().stream()
                .filter(MainLayout.class::isInstance)
                .map(MainLayout.class::cast)
                .findFirst();
    }
}