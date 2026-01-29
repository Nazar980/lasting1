package edu.unl.csce466.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import edu.unl.csce466.imgui.ImGuiRenderer;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import imgui.ImVec4;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ImGuiScreen extends Screen {
    private static ImGuiScreen _INSTANCE = null;
    private boolean buttonClicked = false;
    private final ImBoolean showDemo = new ImBoolean(false);
    private final ImBoolean showStyleEditor = new ImBoolean(false);

    // Храним цвета как ImVec4 (0..1)
    private final ImVec4 colWindowBg = new ImVec4(30/255f, 30/255f, 35/255f, 180/255f);
    private final ImVec4 colTitleBg = new ImVec4(245/255f, 70/255f, 130/255f, 220/255f);
    private final ImVec4 colTitleBgActive = new ImVec4(255/255f, 90/255f, 150/255f, 220/255f);
    private final ImVec4 colTitleBgCollapsed = new ImVec4(245/255f, 70/255f, 130/255f, 160/255f);
    private final ImVec4 colText = new ImVec4(1f, 1f, 1f, 1f);
    private final ImVec4 colTextDisabled = new ImVec4(180/255f, 180/255f, 190/255f, 140/255f);
    private final ImVec4 colTab = new ImVec4(42/255f, 42/255f, 42/255f, 180/255f);
    private final ImVec4 colTabHovered = new ImVec4(60/255f, 60/255f, 65/255f, 200/255f);
    private final ImVec4 colTabActive = new ImVec4(50/255f, 50/255f, 55/255f, 220/255f);
    private final ImVec4 colButton = new ImVec4(70/255f, 70/255f, 80/255f, 160/255f);
    private final ImVec4 colButtonHovered = new ImVec4(90/255f, 90/255f, 100/255f, 180/255f);
    private final ImVec4 colButtonActive = new ImVec4(110/255f, 110/255f, 120/255f, 220/255f);
    private final ImVec4 colCheckMark = new ImVec4(245/255f, 70/255f, 130/255f, 1f);
    private final ImVec4 colFrameBg = new ImVec4(50/255f, 50/255f, 55/255f, 160/255f);
    private final ImVec4 colFrameBgHovered = new ImVec4(70/255f, 70/255f, 80/255f, 180/255f);
    private final ImVec4 colFrameBgActive = new ImVec4(90/255f, 90/255f, 100/255f, 220/255f);

    public static ImGuiScreen getInstance() {
        if (_INSTANCE == null) _INSTANCE = new ImGuiScreen();
        return _INSTANCE;
    }

    private ImGuiScreen() {
        super(Component.literal("ImGui"));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        ImGuiIO io = ImGui.getIO();
        Minecraft mc = Minecraft.getInstance();
        io.setMousePos((float) mc.mouseHandler.xpos(), (float) mc.mouseHandler.ypos());

        ImGuiRenderer.getInstance().draw(() -> {
            applyCurrentColors();

            ImGui.setNextWindowSize(500, 400, ImGuiCond.FirstUseEver);
            ImGui.setNextWindowPos(
                (io.getDisplaySizeX() - 500) * 0.5f,
                (io.getDisplaySizeY() - 400) * 0.5f,
                ImGuiCond.FirstUseEver
            );

            int flags = ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize;

            ImGui.begin("GD Mega Hack - Style Editor", flags);

            String titleText = "GD Mega Hack";
            float textWidth = ImGui.calcTextSize(titleText).x;
            float titleBarWidth = ImGui.getWindowWidth() - ImGui.getStyle().getWindowPaddingX() * 2;
            float cursorX = (titleBarWidth - textWidth) * 0.5f + ImGui.getStyle().getWindowPaddingX();
            ImGui.setCursorPosX(cursorX);
            ImGui.text(titleText);

            ImGui.separator();

            ImGui.checkbox("Open Style Color Editor", showStyleEditor);
            ImGui.separator();

            ImGui.text("Test elements:");
            if (ImGui.button("Click me")) {
                buttonClicked = true;
            }
            if (buttonClicked) {
                ImGui.text("Button clicked!");
            }
            ImGui.checkbox("Demo Window", showDemo);

            ImGui.end();

            if (showStyleEditor.get()) {
                ImGui.begin("Style Color Editor", showStyleEditor);
                ImGui.text("Change colors live:");

                ImGui.colorEdit4("WindowBg", colWindowBg);
                ImGui.colorEdit4("TitleBg", colTitleBg);
                ImGui.colorEdit4("TitleBgActive", colTitleBgActive);
                ImGui.colorEdit4("TitleBgCollapsed", colTitleBgCollapsed);
                ImGui.colorEdit4("Text", colText);
                ImGui.colorEdit4("TextDisabled", colTextDisabled);
                ImGui.colorEdit4("Tab", colTab);
                ImGui.colorEdit4("TabHovered", colTabHovered);
                ImGui.colorEdit4("TabActive", colTabActive);
                ImGui.colorEdit4("Button", colButton);
                ImGui.colorEdit4("ButtonHovered", colButtonHovered);
                ImGui.colorEdit4("ButtonActive", colButtonActive);
                ImGui.colorEdit4("CheckMark", colCheckMark);
                ImGui.colorEdit4("FrameBg", colFrameBg);
                ImGui.colorEdit4("FrameBgHovered", colFrameBgHovered);
                ImGui.colorEdit4("FrameBgActive", colFrameBgActive);

                if (ImGui.button("Reset to Default")) {
                    resetColorsToDefault();
                }

                ImGui.end();
            }

            if (showDemo.get()) {
                ImGui.showDemoWindow(showDemo);
            }
        });
    }

    private void applyCurrentColors() {
        ImGuiStyle style = ImGui.getStyle();

        style.setColor(ImGuiCol.WindowBg, colWindowBg.x, colWindowBg.y, colWindowBg.z, colWindowBg.w);
        style.setColor(ImGuiCol.TitleBg, colTitleBg.x, colTitleBg.y, colTitleBg.z, colTitleBg.w);
        style.setColor(ImGuiCol.TitleBgActive, colTitleBgActive.x, colTitleBgActive.y, colTitleBgActive.z, colTitleBgActive.w);
        style.setColor(ImGuiCol.TitleBgCollapsed, colTitleBgCollapsed.x, colTitleBgCollapsed.y, colTitleBgCollapsed.z, colTitleBgCollapsed.w);
        style.setColor(ImGuiCol.Text, colText.x, colText.y, colText.z, colText.w);
        style.setColor(ImGuiCol.TextDisabled, colTextDisabled.x, colTextDisabled.y, colTextDisabled.z, colTextDisabled.w);
        style.setColor(ImGuiCol.Tab, colTab.x, colTab.y, colTab.z, colTab.w);
        style.setColor(ImGuiCol.TabHovered, colTabHovered.x, colTabHovered.y, colTabHovered.z, colTabHovered.w);
        style.setColor(ImGuiCol.TabActive, colTabActive.x, colTabActive.y, colTabActive.z, colTabActive.w);
        style.setColor(ImGuiCol.Button, colButton.x, colButton.y, colButton.z, colButton.w);
        style.setColor(ImGuiCol.ButtonHovered, colButtonHovered.x, colButtonHovered.y, colButtonHovered.z, colButtonHovered.w);
        style.setColor(ImGuiCol.ButtonActive, colButtonActive.x, colButtonActive.y, colButtonActive.z, colButtonActive.w);
        style.setColor(ImGuiCol.CheckMark, colCheckMark.x, colCheckMark.y, colCheckMark.z, colCheckMark.w);
        style.setColor(ImGuiCol.FrameBg, colFrameBg.x, colFrameBg.y, colFrameBg.z, colFrameBg.w);
        style.setColor(ImGuiCol.FrameBgHovered, colFrameBgHovered.x, colFrameBgHovered.y, colFrameBgHovered.z, colFrameBgHovered.w);
        style.setColor(ImGuiCol.FrameBgActive, colFrameBgActive.x, colFrameBgActive.y, colFrameBgActive.z, colFrameBgActive.w);

        style.setWindowRounding(0.0f);
        style.setFrameRounding(0.0f);
        style.setTabRounding(0.0f);
        style.setGrabRounding(0.0f);
        style.setScrollbarRounding(0.0f);
        style.setPopupRounding(0.0f);
        style.setWindowPadding(10.0f, 10.0f);
        style.setFramePadding(6.0f, 4.0f);
        style.setItemSpacing(8.0f, 6.0f);
    }

    private void resetColorsToDefault() {
        colWindowBg.set(30/255f, 30/255f, 35/255f, 180/255f);
        colTitleBg.set(245/255f, 70/255f, 130/255f, 220/255f);
        colTitleBgActive.set(255/255f, 90/255f, 150/255f, 220/255f);
        colTitleBgCollapsed.set(245/255f, 70/255f, 130/255f, 160/255f);
        colText.set(1f, 1f, 1f, 1f);
        colTextDisabled.set(180/255f, 180/255f, 190/255f, 140/255f);
        colTab.set(42/255f, 42/255f, 42/255f, 180/255f);
        colTabHovered.set(60/255f, 60/255f, 65/255f, 200/255f);
        colTabActive.set(50/255f, 50/255f, 55/255f, 220/255f);
        colButton.set(70/255f, 70/255f, 80/255f, 160/255f);
        colButtonHovered.set(90/255f, 90/255f, 100/255f, 180/255f);
        colButtonActive.set(110/255f, 110/255f, 120/255f, 220/255f);
        colCheckMark.set(245/255f, 70/255f, 130/255f, 1f);
        colFrameBg.set(50/255f, 50/255f, 55/255f, 160/255f);
        colFrameBgHovered.set(70/255f, 70/255f, 80/255f, 180/255f);
        colFrameBgActive.set(90/255f, 90/255f, 100/255f, 220/255f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ImGui.getIO().setMouseDown(button, true);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        ImGui.getIO().setMouseDown(button, false);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        ImGui.getIO().setMouseWheel((float) delta);
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
