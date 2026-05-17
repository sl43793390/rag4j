package com.sl.rag4j.ui.component;

import cn.hutool.core.util.IdUtil;
import com.sl.rag4j.entity.KnowledgeBase;
import com.sl.rag4j.mapper.KnowledgeBaseMapper;
import com.sl.rag4j.model.SplitterType;
import com.sl.rag4j.ragservice.EmbeddingService;
import com.sl.rag4j.config.UserInfoHelper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 创建/编辑知识库对话框，提供表单输入和文件上传功能
 * 支持两种模式：创建新库（mode=CREATE）或编辑已有库（mode=EDIT）
 * 创建模式下所有字段可编辑，编辑模式下仅名称、描述可修改，其他字段只读或隐藏
 * 可同时上传文档文件，创建或编辑后自动执行嵌入流水线
 * 通过构造函数接收Spring bean和可选的待编辑知识库实体
 */
public class CreateKnowledgeBaseDialog extends Dialog {

    /** 对话框模式：创建新库或编辑已有库 */
    public enum Mode { CREATE, EDIT }

    private final TextField nameField;
    private final TextArea descriptionField;
    private final ComboBox<SplitterType> splitterSelect;
    private final IntegerField maxSegmentSizeField;
    private final IntegerField maxOverlapSizeField;
    private final Upload fileUpload;
    private final MemoryBuffer memoryBuffer;

    /** Spring bean通过构造函数注入 */
    private final KnowledgeBaseMapper kbMapper;
    private final EmbeddingService embeddingService;

    /** 当前模式，默认创建模式 */
    private final Mode mode;
    /** 编辑模式下的目标知识库实体 */
    private KnowledgeBase editingKb;

    /** 临时存储上传的文件名和内容列表，支持多文件 */
    private static class UploadedFile {
        final String fileName;
        final byte[] content;
        UploadedFile(String fileName, byte[] content) {
            this.fileName = fileName;
            this.content = content;
        }
    }
    private final java.util.List<UploadedFile> uploadedFiles = new java.util.ArrayList<>();

    /**
     * 构造创建知识库对话框，通过构造函数接收Spring bean
     * @param kbMapper 知识库Mapper，用于保存知识库到SQLite
     * @param embeddingService 嵌入服务，用于执行文档嵌入流水线
     */
    public CreateKnowledgeBaseDialog(KnowledgeBaseMapper kbMapper, EmbeddingService embeddingService) {
        this(kbMapper, embeddingService, Mode.CREATE, null);
    }

    /**
     * 构造对话框，支持创建和编辑两种模式
     * @param kbMapper 知识库Mapper
     * @param embeddingService 嵌入服务
     * @param mode 对话框模式
     * @param editingKb 编辑模式下的目标知识库，创建模式下为null
     */
    public CreateKnowledgeBaseDialog(KnowledgeBaseMapper kbMapper, EmbeddingService embeddingService,
                                     Mode mode, KnowledgeBase editingKb) {
        this.kbMapper = kbMapper;
        this.embeddingService = embeddingService;
        this.mode = mode;
        this.editingKb = editingKb;
        setHeaderTitle(mode == Mode.EDIT ? "编辑知识库" : "新建知识库");

        // 知识库名称
        nameField = new TextField("名称");
        nameField.setRequired(true);
        nameField.setWidthFull();
        nameField.setPlaceholder("请输入知识库名称");

        // 知识库描述
        descriptionField = new TextArea("描述");
        descriptionField.setWidthFull();
        descriptionField.setPlaceholder("请输入知识库描述");
        descriptionField.setMaxHeight("100px");

        // 切割方式选择（编辑模式下只读）
        splitterSelect = new ComboBox<>();
        splitterSelect.setWidthFull();
        splitterSelect.setLabel("切割方式");
        splitterSelect.setItems(SplitterType.values());
        splitterSelect.setValue(SplitterType.RECURSIVE);
        splitterSelect.setReadOnly(mode == Mode.EDIT);

        // 每段最大大小（编辑模式下只读）
        maxSegmentSizeField = new IntegerField("每段最大大小");
        maxSegmentSizeField.setWidthFull();
        maxSegmentSizeField.setValue(500);
        maxSegmentSizeField.setMin(50);
        maxSegmentSizeField.setStep(50);
        maxSegmentSizeField.setReadOnly(mode == Mode.EDIT);

        // 相邻段重叠大小（编辑模式下只读）
        maxOverlapSizeField = new IntegerField("重叠大小");
        maxOverlapSizeField.setWidthFull();
        maxOverlapSizeField.setValue(100);
        maxOverlapSizeField.setMin(0);
        maxOverlapSizeField.setStep(10);
        maxOverlapSizeField.setReadOnly(mode == Mode.EDIT);

        // 编辑模式下预填已有数据
        if (mode == Mode.EDIT && editingKb != null) {
            nameField.setValue(editingKb.getName());
            descriptionField.setValue(editingKb.getDescription());
            splitterSelect.setValue(SplitterType.valueOf(editingKb.getSplitterType()));
            maxSegmentSizeField.setValue(editingKb.getMaxSegmentSize());
            maxOverlapSizeField.setValue(editingKb.getMaxOverlapSize());
        }

        // 文件上传组件，使用内存缓冲接收器
        memoryBuffer = new MemoryBuffer();
        fileUpload = new Upload(memoryBuffer);
        fileUpload.setWidthFull();
        fileUpload.setAcceptedFileTypes(".pdf", ".doc", ".docx", ".txt", ".md", ".xls", ".xlsx");
        fileUpload.setMaxFileSize(1024 * 1024 * 30);
        fileUpload.setMaxFiles(5);
        fileUpload.addFinishedListener(e -> {
            String fName = e.getFileName();
            try {
                byte[] content = memoryBuffer.getInputStream().readAllBytes();
                uploadedFiles.add(new UploadedFile(fName, content));
            } catch (Exception ex) {
                Notification.show("文件读取失败: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });

        // 创建/保存按钮
        String btnText = mode == Mode.EDIT ? "保存" : "创建";
        Button saveBtn = new Button(btnText, e -> saveKnowledgeBase());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("取消", e -> close());

        // 编辑模式下隐藏不可编辑字段的标签，仅保留视觉展示
        if (mode == Mode.EDIT) {
            VerticalLayout formLayout = new VerticalLayout(
                    nameField, descriptionField, splitterSelect, maxSegmentSizeField, maxOverlapSizeField, fileUpload);
            formLayout.setPadding(false);
            formLayout.setSpacing(true);
            add(formLayout);
        } else {
            // 创建模式下正常展示
            VerticalLayout formLayout = new VerticalLayout(
                    nameField, descriptionField, splitterSelect, maxSegmentSizeField, maxOverlapSizeField, fileUpload);
            formLayout.setPadding(false);
            formLayout.setSpacing(true);
            add(formLayout);
        }
        getFooter().add(cancelBtn, saveBtn);
        setWidth("500px");
    }

    /**
     * 保存知识库：创建模式下插入新记录，编辑模式下更新名称、描述并可选追加文件嵌入
     */
    private void saveKnowledgeBase() {
        // 验证必填字段
        if (nameField.isEmpty()) {
            Notification.show("知识库名称不能为空", 3000, Notification.Position.MIDDLE);
            return;
        }

        if (mode == Mode.CREATE) {
            // 创建模式：生成Milvus collection名称，插入新记录
            String collectionName = "kb_" + IdUtil.getSnowflakeNextIdStr();
            KnowledgeBase kb = new KnowledgeBase();
            kb.setName(nameField.getValue());
            kb.setDescription(descriptionField.getValue());
            kb.setSplitterType(splitterSelect.getValue().name());
            kb.setMilvusCollectionName(collectionName);
            kb.setMaxSegmentSize(maxSegmentSizeField.getValue());
            kb.setMaxOverlapSize(maxOverlapSizeField.getValue());
            kb.setDocCount(0);
            kb.setUserName(getCurrentUserId());
            kbMapper.insert(kb);

            // 如果有上传文件，循环对每个文件执行嵌入流水线
            if (!uploadedFiles.isEmpty()) {
                for (UploadedFile uf : uploadedFiles) {
                    InputStream inputStream = new ByteArrayInputStream(uf.content);
                    embeddingService.ingestDocument(kb.getId(), uf.fileName, uf.content.length, inputStream);
                }
            }
            Notification.show("知识库创建成功", 3000, Notification.Position.MIDDLE);
        } else if (mode == Mode.EDIT && editingKb != null) {
            // 编辑模式：仅更新名称和描述，可选追加文件嵌入
            editingKb.setName(nameField.getValue());
            editingKb.setDescription(descriptionField.getValue());
            kbMapper.updateById(editingKb);

            // 如果有上传文件，追加嵌入到已有知识库
            if (!uploadedFiles.isEmpty()) {
                for (UploadedFile uf : uploadedFiles) {
                    InputStream inputStream = new ByteArrayInputStream(uf.content);
                    embeddingService.ingestDocument(editingKb.getId(), uf.fileName, uf.content.length, inputStream);
                }
            }
            Notification.show("知识库更新成功", 3000, Notification.Position.MIDDLE);
        }

        close();
    }

    /**
     * 获取当前登录用户的ID，从VaadinSession中读取
     */
    private String getCurrentUserId() {
        return UserInfoHelper.getUserIdFromSession();
    }
}