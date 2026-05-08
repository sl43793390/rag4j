package com.sl.rag4j.ui.view;

import com.sl.rag4j.entity.KnowledgeBase;
import com.sl.rag4j.mapper.KnowledgeBaseMapper;
import com.sl.rag4j.ragservice.EmbeddingService;
import com.sl.rag4j.config.UserInfoHelper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.*;
import com.sl.rag4j.ui.component.MainLayout;
import com.sl.rag4j.ui.component.CreateKnowledgeBaseDialog;
import com.sl.rag4j.ui.component.CreateKnowledgeBaseDialog.Mode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.PermitAll;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
    private final EmbeddingService embeddingService;
    private TextField searchField;
    private final Grid<KnowledgeBase> grid;

    /**
     * 构造知识库列表视图，创建搜索栏和知识库表格
     */
    public KnowledgeBaseListView(KnowledgeBaseMapper kbMapper, EmbeddingService embeddingService) {
        this.kbMapper = kbMapper;
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

        topBar.add(searchField, searchBtn, createBtn);
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