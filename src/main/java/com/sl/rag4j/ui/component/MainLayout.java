package com.sl.rag4j.ui.component;

import com.sl.rag4j.config.UserInfoHelper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * 主布局组件，为所有页面提供顶部导航栏
 * 包含应用标题和注销按钮，注销通过Spring Security的/logout端点实现
 */
public class MainLayout extends AppLayout {

    private final HorizontalLayout navbar;
    private Button backToKbBtn;
    /**
     * 构造主布局，创建顶部导航栏
     */
    public MainLayout() {
        // 应用标题
        H1 title = new H1("rag4j");
        title.getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin-left", "30px").set("color","white");
        //返回按钮
        backToKbBtn = new Button("返回知识库", new Icon(VaadinIcon.ARROW_LEFT));
        backToKbBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backToKbBtn.addClickListener(e -> UI.getCurrent().navigate("know"));
        backToKbBtn.getStyle().set("color","white");
        // 注销按钮
        Button logoutButton = new Button("注销", e -> {
            UserInfoHelper.saveUserToSession(null);
            UI.getCurrent().getPage().setLocation("/login");
        });
        logoutButton.getStyle().set("margin-right","20px").set("color","white");;
        // 导航栏布局
        navbar = new HorizontalLayout(title,backToKbBtn, logoutButton);
        navbar.setWidthFull();
        navbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        navbar.setAlignItems(FlexComponent.Alignment.CENTER);
        navbar.setPadding(false);
        navbar.getStyle().set("background-color","#0665D0");
        addToNavbar(navbar);
    }

    public Button getBackToKbBtn() {
        return backToKbBtn;
    }

    public void setBackToKbBtn(Button backToKbBtn) {
        this.backToKbBtn = backToKbBtn;
    }
}