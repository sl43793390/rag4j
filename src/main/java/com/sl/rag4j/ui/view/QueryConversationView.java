package com.sl.rag4j.ui.view;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sl.rag4j.config.UserInfoHelper;
import com.sl.rag4j.entity.ChatMemory;
import com.sl.rag4j.entity.KnowledgeBase;
import com.sl.rag4j.mapper.ChatMemoryMapper;
import com.sl.rag4j.mapper.KnowledgeBaseMapper;
import com.sl.rag4j.ragservice.RagQueryService;
import com.sl.rag4j.ui.component.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.security.PermitAll;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 查询对话视图，提供基于知识库的RAG问答对话界面
 * 左侧展示当前用户在该知识库下的聊天历史记录列表
 * 右侧为聊天对话区域，支持流式RAG问答
 * 使用Vaadin @Push注解支持服务器推送实时更新
 */
@Route(value = "query/:kbId", layout = MainLayout.class)
@PageTitle("知识库查询")
@PermitAll
public class QueryConversationView extends VerticalLayout implements BeforeEnterObserver {

    private Long kbId;
    private String kbName;
    /** 会话ID，用于RAG查询的记忆机制，每个页面实例对应一个独立会话 */
    private String memoryId;

    private final RagQueryService ragQueryService;
    private final KnowledgeBaseMapper kbMapper;
    private final ChatMemoryMapper chatMemoryMapper;

    // 左侧布局
    private final VerticalLayout leftSidebar;
    private final ListBox<ChatMemory> chatHistoryListBox;
    private final Button newChatBtn;

    // 右侧布局
    private final VerticalLayout chatContainer;
    private final TextField inputField;
    private final Button sendBtn;

    /** 保存流式响应的推送注册，在detach时取消以避免内存泄漏 */
    private final AtomicReference<Registration> pushRegistration = new AtomicReference<>();

    /**
     * 构造查询对话视图，创建左侧聊天历史列表和右侧聊天消息区
     */
    public QueryConversationView(RagQueryService ragQueryService, KnowledgeBaseMapper kbMapper, ChatMemoryMapper chatMemoryMapper) {
        this.ragQueryService = ragQueryService;
        this.kbMapper = kbMapper;
        this.chatMemoryMapper = chatMemoryMapper;
        // 为每个页面实例生成独立的会话ID，确保不同对话会话的记忆互不干扰
        this.memoryId = IdUtil.getSnowflakeNextIdStr();

        setSpacing(false);
        setPadding(false);
        setSizeFull();

        // ===== 左侧聊天历史列表 =====
        leftSidebar = new VerticalLayout();
        leftSidebar.setWidth("360px");
        leftSidebar.setHeightFull();
        leftSidebar.getStyle().set("border-right", "1px solid #e0e0e0");
        leftSidebar.setPadding(false);
        leftSidebar.setSpacing(false);

        // 新建对话按钮
        newChatBtn = new Button("新建对话", new Icon(VaadinIcon.PLUS));
        newChatBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newChatBtn.setWidth("220px");
        newChatBtn.getStyle().set("margin", "12px");
        newChatBtn.addClickListener(e -> createNewChat());

        // 聊天历史列表
        chatHistoryListBox = new ListBox<>();
        chatHistoryListBox.setWidth("280px");
        chatHistoryListBox.getStyle().set("flex", "1");
        chatHistoryListBox.setRenderer(new ComponentRenderer<>(this::createChatHistoryItem));
        chatHistoryListBox.addValueChangeListener(e -> onChatHistorySelected(e.getValue()));

        // 将列表放入Scroller使其可滚动
        Scroller listScroller = new Scroller(chatHistoryListBox);
        listScroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        listScroller.getStyle().set("flex", "1");

        leftSidebar.add(newChatBtn, listScroller);

        // ===== 右侧聊天区域 =====
        VerticalLayout rightPanel = new VerticalLayout();
        rightPanel.setSpacing(false);
        rightPanel.setPadding(false);
        rightPanel.setSizeFull();

        // 聊天消息容器区域，滚动显示对话历史
        chatContainer = new VerticalLayout();
        chatContainer.setSizeFull();
        chatContainer.getStyle().set("overflow-y", "auto");
        chatContainer.setPadding(true);

        // 输入栏：文本输入框和发送按钮
        inputField = new TextField();
        inputField.setPlaceholder("请输入您的问题...");
        inputField.setWidthFull();
//        inputField.getStyle().set("height","70px");

        sendBtn = new Button("发送", e -> sendMessage());
        sendBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout inputBar = new HorizontalLayout(inputField, sendBtn);
        inputBar.setWidthFull();
        inputBar.setFlexGrow(1, inputField);
        inputBar.setAlignItems(FlexComponent.Alignment.CENTER);
        inputBar.getStyle().set("border-top", "1px solid #e0e0e0");
        inputBar.setPadding(true);

        rightPanel.add(chatContainer, inputBar);
        rightPanel.setFlexGrow(1, chatContainer);

        // 主布局：左右分栏
        HorizontalLayout mainLayout = new HorizontalLayout(leftSidebar, rightPanel);
        mainLayout.setSizeFull();
        mainLayout.setSpacing(false);
        mainLayout.setPadding(false);
        mainLayout.setFlexGrow(1, rightPanel);

        add(mainLayout);
        setFlexGrow(1, mainLayout);
    }

    /**
     * 路由进入时获取知识库ID和名称，加载聊天历史列表
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (null == UserInfoHelper.getUserFromSession()) {
            UI.getCurrent().navigate("login");
            return;
        }
        // 隐藏顶部"返回知识库"按钮
        getMainLayout().ifPresent(layout -> layout.getBackToKbBtn().setVisible(true));
        kbId = event.getRouteParameters().getLong("kbId").orElse(-1L);
        if (kbId > 0) {
            KnowledgeBase kb = kbMapper.selectById(kbId);
            if (kb != null) {
                kbName = kb.getName();
            }
        }
        // 加载聊天历史列表
        loadChatHistoryList();
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

    /**
     * 从数据库加载当前用户在该知识库下的所有聊天历史记录，按memoryId倒序排列
     */
    private void loadChatHistoryList() {
        String userName = UserInfoHelper.getUserIdFromSession();
        if (userName == null) {
            return;
        }
        List<ChatMemory> chatMemories = chatMemoryMapper.selectByUserAndKbId(userName, kbId);
        chatHistoryListBox.setItems(chatMemories);

        // 默认选中第一条会话
        if (!chatMemories.isEmpty()) {
            ChatMemory first = chatMemories.get(0);
            memoryId = first.getMemoryId();
            chatHistoryListBox.setValue(first);
            // 加载该会话的历史消息到聊天区域
            loadChatMessages(first);
        } else {
            // 没有任何历史记录，清空聊天区域
            chatContainer.removeAll();
        }
    }

    /**
     * 加载指定会话的历史消息到聊天展示区域
     * @param chatMemory 聊天记忆实体
     */
    private void loadChatMessages(ChatMemory chatMemory) {
        chatContainer.removeAll();
        if (chatMemory == null || chatMemory.getMessages() == null || chatMemory.getMessages().isBlank()) {
            return;
        }
        // 反序列化历史消息并展示
        List<ChatMessage> messages = ChatMessageDeserializer.messagesFromJson(chatMemory.getMessages());
        for (ChatMessage msg : messages) {
            if (msg instanceof UserMessage) {
                addUserMessage(((UserMessage) msg).singleText());
            } else if (msg instanceof AiMessage) {
                addAiMessage(((AiMessage) msg).text());
            }
        }
        // 滚动到底部
        scrollToBottom();
    }

    /**
     * 创建聊天历史列表项组件
     */
    private Div createChatHistoryItem(ChatMemory chatMemory) {
        Div item = new Div();
        item.getStyle().set("padding", "12px");
        item.getStyle().set("cursor", "pointer");
        item.getStyle().set("border-bottom", "1px solid #f0f0f0");
        item.getStyle().set("transition", "background-color 0.2s");

        // 标题显示
        Span title = new Span(chatMemory.getChatTitle() != null ? chatMemory.getChatTitle() : "新对话");
        title.getStyle().set("font-weight", "500");
        title.getStyle().set("font-size", "14px");

        // 预览：取第一条用户消息作为预览
        Span preview = new Span(getFirstUserMessagePreview(chatMemory));
        preview.getStyle().set("font-size", "12px");
        preview.getStyle().set("color", "#888");
        preview.getStyle().set("white-space", "nowrap");
        preview.getStyle().set("overflow", "hidden");
        preview.getStyle().set("text-overflow", "ellipsis");
        preview.getStyle().set("display", "block");
        preview.getStyle().set("max-width", "100%");

        VerticalLayout textLayout = new VerticalLayout(title, preview);
        textLayout.setSpacing(false);
        textLayout.setPadding(false);
        textLayout.setWidthFull();

        // 删除按钮
        Button deleteBtn = new Button(new Icon(VaadinIcon.CLOSE));
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        deleteBtn.getStyle().set("color", "#999").set("flex-shrink", "0");
        deleteBtn.addClickListener(e ->openDeleteConfirmationDialog(chatMemory));

        HorizontalLayout itemLayout = new HorizontalLayout(textLayout, deleteBtn);
        itemLayout.setWidthFull();
        itemLayout.setSpacing(false);
        itemLayout.setPadding(false);
        itemLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        itemLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        item.add(itemLayout);

        // 点击切换会话
        item.addClickListener(click -> chatHistoryListBox.setValue(chatMemory));

        return item;
    }
    private void openDeleteConfirmationDialog(ChatMemory chatMemory) {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("确认删除");
        confirmDialog.setText("确定要删除该会话吗？此操作不可撤销。");

        confirmDialog.setConfirmButton("删除", e -> {
            try {
                chatMemoryMapper.deleteById(chatMemory.getMemoryId());
                Notification notification = Notification.show("删除成功");
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                reloadAfterDelete(chatMemory);
            } catch (Exception ex) {
                Notification notification = Notification.show("删除失败: " + ex.getMessage());
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmDialog.setCancelButton("取消", e -> {});
        confirmDialog.open();
    }
    /**
     * 从历史消息中获取第一条用户消息作为预览
     */
    private String getFirstUserMessagePreview(ChatMemory chatMemory) {
        if (chatMemory.getMessages() == null || chatMemory.getMessages().isBlank()) {
            return "暂无消息";
        }
        try {
            List<ChatMessage> messages = ChatMessageDeserializer.messagesFromJson(chatMemory.getMessages());
            for (ChatMessage msg : messages) {
                if (msg instanceof UserMessage userMsg) {
                    String text = userMsg.singleText();
                    return text.length() > 30 ? text.substring(0, 30) + "..." : text;
                }
            }
        } catch (Exception e) {
            // 忽略解析异常
        }
        return "暂无消息";
    }

    /**
     * 聊天历史列表选择变更事件处理
     */
    private void onChatHistorySelected(ChatMemory selected) {
        if (selected == null) {
            return;
        }
        memoryId = selected.getMemoryId();
        loadChatMessages(selected);
        refreshListBoxStyles();
    }

    /**
     * 创建新对话，生成新的memoryId并清空聊天区域
     */
    private void createNewChat() {
        memoryId = IdUtil.getSnowflakeNextIdStr();
        chatContainer.removeAll();
        chatHistoryListBox.setValue(null);
        refreshListBoxStyles();
    }

    /**
     * 刷新列表选中状态样式
     */
    private void refreshListBoxStyles() {
        // Vaadin ListBox的选中状态由组件自身管理，此处无需额外处理
    }

    /**
     * 删除会话后重新加载列表并处理选中状态
     */
    private void reloadAfterDelete(ChatMemory deleted) {
        String deletedId = deleted.getMemoryId();
        ChatMemory current = chatHistoryListBox.getValue();
        loadChatHistoryList();
        if (current != null && current.getMemoryId().equals(deletedId)) {
            ChatMemory next = chatHistoryListBox.getValue();
            if (next != null) {
                onChatHistorySelected(next);
            } else {
                chatContainer.removeAll();
                memoryId = IdUtil.getSnowflakeNextIdStr();
            }
        }
    }

    /**
     * 发送用户消息并触发流式RAG查询
     * 先在聊天区添加用户消息气泡，然后调用流式查询逐步显示AI回答
     * 使用memoryId确保同一对话会话保留上下文记忆
     */
    private void sendMessage() {
        String question = inputField.getValue();
        if (question == null || question.isBlank()) {
            return;
        }
        inputField.clear();

        String userName = UserInfoHelper.getUserIdFromSession();
        if (userName == null) {
            return;
        }

        // 确保当前memoryId在数据库中有记录，包含kbId和chatTitle
        ensureChatMemoryExists(question);

        // 添加用户消息到聊天区
        addUserMessage(question);

        // 创建AI消息占位区域
        Markdown aiMessageMarkdown = addAiMessagePlaceholder();
        StringBuilder responseBuilder = new StringBuilder();

        // 获取当前UI用于推送更新
        UI ui = getUI().orElse(null);
        if (ui == null) return;

        // 启动流式查询，传入memoryId实现会话记忆
        String answer = ragQueryService.query(kbId, memoryId, question);
        responseBuilder.append(answer);
        aiMessageMarkdown.setContent(responseBuilder.toString());
        loadChatHistoryList();
        scrollToBottom();
    }

    /**
     * 确保当前memoryId在数据库中有记录，包含kbId和chatTitle
     * 如果已存在则更新chatTitle（使用第一条用户消息作为标题）
     */
    private void ensureChatMemoryExists(String firstQuestion) {
        ChatMemory existing = chatMemoryMapper.selectById(memoryId);
        String userName = UserInfoHelper.getUserIdFromSession();
        if (existing == null) {
            // 不存在则创建，带上kbId和chatTitle
            ChatMemory chatMemory = new ChatMemory();
            chatMemory.setMemoryId(memoryId);
            chatMemory.setUserName(userName);
            chatMemory.setKbId(kbId);
            // 使用用户第一条消息作为会话标题
            chatMemory.setChatTitle(truncateTitle(firstQuestion));
            chatMemory.setMessages("");
            chatMemoryMapper.insert(chatMemory);
        } else if (existing.getChatTitle() == null || existing.getChatTitle().isBlank()) {
            // 已存在但没有标题，补充标题
            LambdaQueryWrapper<ChatMemory> updateWrapper = new LambdaQueryWrapper<>();
            updateWrapper.eq(ChatMemory::getMemoryId, memoryId);
            ChatMemory toUpdate = new ChatMemory();
            toUpdate.setChatTitle(truncateTitle(firstQuestion));
            chatMemoryMapper.update(toUpdate, updateWrapper);
        }
    }

    /**
     * 截取标题，超过20字符截断
     */
    private String truncateTitle(String text) {
        return text.length() > 10 ? text.substring(0, 10) + "..." : text;
    }

    /**
     * 在聊天区添加用户消息气泡（右对齐，带头像）
     */
    private void addUserMessage(String text) {
        String userName = UserInfoHelper.getUserIdFromSession();
        String initials = userName != null ? userName : "User";
        addMessageWithAvatar(text, initials, false);
    }

    /**
     * 在聊天区添加AI消息（左对齐，带头像）
     */
    private void addAiMessage(String text) {
        addMessageWithAvatar(text, "Artificial Intelligence", true);
    }

    /**
     * 通用消息渲染方法，带头像和消息气泡
     * @param text 消息文本
     * @param avatarName 头像显示的字母
     * @param isAi 是否为AI消息（true=左侧排列，false=右侧排列）
     */
    private void addMessageWithAvatar(String text, String avatarName, boolean isAi) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(false);
        row.setPadding(false);
        row.getStyle().set("margin-bottom", "8px");

        Avatar avatar = new Avatar(avatarName);
        avatar.getStyle()
                .set("width", "32px")
                .set("height", "32px")
                .set("flex-shrink", "0")
                .set("font-size", "12px")
                .set("font-weight", "600");
        if (isAi) {
            avatar.getStyle().set("background-color", "#e0e0e0");
        } else {
            avatar.getStyle().set("background-color", "#1976d2");
            avatar.getStyle().set("color", "#ffffff");
        }

        if (isAi) {
            Markdown markdown = new Markdown(text);
            markdown.getStyle()
                    .set("max-width", "70%")
                    .set("margin-left","20px");
            row.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
            row.add(avatar, markdown);
        } else {
            Div bubble = new Div();
            bubble.setText(text);
            bubble.getStyle()
                    .set("background", "#e3f2fd")
                    .set("padding", "8px 12px")
                    .set("border-radius", "8px")
                    .set("max-width", "70%")
                    .set("word-break", "break-word")
                    .set("margin-right","20px");
            row.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
            row.add(bubble, avatar);
        }

        chatContainer.add(row);
        scrollToBottom();
    }

    /**
     * 在聊天区添加AI消息占位区域（左对齐样式，带头像）
     * @return Markdown组件，用于后续流式更新文本内容
     */
    private Markdown addAiMessagePlaceholder() {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(false);
        row.setPadding(false);
        row.getStyle().set("margin-bottom", "8px");

        Avatar avatar = new Avatar("AI");
        avatar.getStyle()
                .set("width", "32px")
                .set("height", "32px")
                .set("flex-shrink", "0")
                .set("font-size", "12px")
                .set("font-weight", "600")
                .set("background-color", "#e0e0e0");

        Markdown markdown = new Markdown("思考中...");
        markdown.getStyle()
                .set("max-width", "70%")
                .set("margin-left","20px");

        row.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        row.add(avatar, markdown);

        chatContainer.add(row);
        scrollToBottom();
        return markdown;
    }

    /**
     * 滚动聊天区域到底部
     */
    private void scrollToBottom() {
        chatContainer.getElement().executeJs("setTimeout(function() { this.scrollTop = this.scrollHeight; }, 100);");
    }

    /**
     * 视图附加时记录，确保UI可用
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
    }

    /**
     * 视图分离时取消推送注册，避免内存泄漏
     */
    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
    }
}