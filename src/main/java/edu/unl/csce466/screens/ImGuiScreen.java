package edu.unl.csce466.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import edu.unl.csce466.cheat.CheatManager;
import edu.unl.csce466.imgui.ImGuiRenderer;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import imgui.ImVec4;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ImGuiScreen extends Screen {
    private static ImGuiScreen _INSTANCE = null;
    private final ImBoolean windowOpen = new ImBoolean(true);

    public static ImGuiScreen getInstance() {
        if (_INSTANCE == null) _INSTANCE = new ImGuiScreen();
        return _INSTANCE;
    }

    private ImGuiScreen() {
        super(Component.literal("GD Mega Hack"));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        ImGuiIO io = ImGui.getIO();
        Minecraft mc = Minecraft.getInstance();
        io.setMousePos((float) mc.mouseHandler.xpos(), (float) mc.mouseHandler.ypos());

        ImGuiRenderer.getInstance().draw(this::renderMegaHackMenu);
    }

    private void renderMegaHackMenu() {
        setupGDMegaHackStyle();

        ImGui.setNextWindowPos(ImGui.getIO().getDisplaySizeX() * 0.5f, ImGui.getIO().getDisplaySizeY() * 0.5f,
                ImGuiCond.FirstUseEver, 0.5f, 0.5f);
        ImGui.setNextWindowSize(540, 460, ImGuiCond.FirstUseEver);

        ImGui.begin("GD Mega Hack v8 - Minecraft Edition", windowOpen,
                ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.AlwaysAutoResize);

        ImGui.pushStyleColor(ImGuiCol.Text, 0.0f, 0.82f, 1.0f, 1.0f);
        ImGui.text("   GEOMETRIC DASH MEGA HACK   ");
        ImGui.text("       PORTED TO MINECRAFT 1.19.2       ");
        ImGui.popStyleColor();
        ImGui.separator();

        if (ImGui.beginTabBar("MainTabs")) {

            if (ImGui.beginTabItem("Combat")) {
                ImBoolean killaura = new ImBoolean(CheatManager.killAuraEnabled);
                ImGui.checkbox("KillAura", killaura);
                CheatManager.killAuraEnabled = killaura.get();

                ImFloat reach = new ImFloat(CheatManager.reachDistance);
                ImGui.sliderFloat("Reach Distance", reach.getData(), 3.0f, 8.0f, "%.1f blocks");
                CheatManager.reachDistance = reach.get();

                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Movement")) {
                ImBoolean fly = new ImBoolean(CheatManager.flyEnabled);
                ImGui.checkbox("Fly", fly);
                CheatManager.flyEnabled = fly.get();

                ImFloat speed = new ImFloat(CheatManager.speedMultiplier);
                ImGui.sliderFloat("Speed Multiplier", speed.getData(), 1.0f, 15.0f, "%.1fx");
                CheatManager.speedMultiplier = speed.get();

                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Visuals")) {
                ImBoolean esp = new ImBoolean(CheatManager.espEnabled);
                ImGui.checkbox("ESP (Players & Mobs)", esp);
                CheatManager.espEnabled = esp.get();

                ImBoolean fullbright = new ImBoolean(CheatManager.fullbrightEnabled);
                ImGui.checkbox("Fullbright", fullbright);
                CheatManager.fullbrightEnabled = fullbright.get();

                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Misc")) {
                if (ImGui.button("Panic Button - Disable All")) {
                    CheatManager.disableAll();
                }

                ImGui.separator();
                ImGui.textColored(0.0f, 0.82f, 1.0f, 1.0f, "Hotkeys:");
                ImGui.text("Insert - Toggle Menu");

                ImGui.endTabItem();
            }

            ImGui.endTabBar();
        }

        ImGui.separator();
        ImGui.textDisabled("ImGui-Java ~1.90 | Forge 1.19.2");

        ImGui.end();

        if (!windowOpen.get()) {
            Minecraft.getInstance().setScreen(null);
        }
    }

    private void setupGDMegaHackStyle() {
        ImGuiStyle style = ImGui.getStyle();

        style.setWindowRounding(10.0f);
        style.setFrameRounding(5.0f);
        style.setGrabRounding(5.0f);
        style.setScrollbarRounding(10.0f);
        style.setWindowPadding(16.0f, 16.0f);
        style.setItemSpacing(10.0f, 6.0f);

        // Цвета в формате ImU32 через colorConvertFloat4ToU32
        style.setColor(ImGuiCol.WindowBg,        ImGui.colorConvertFloat4ToU32(new ImVec4(20f/255f, 20f/255f, 40f/255f, 240f/255f)));
        style.setColor(ImGuiCol.TitleBg,         ImGui.colorConvertFloat4ToU32(new ImVec4(10f/255f, 10f/255f, 30f/255f, 240f/255f)));
        style.setColor(ImGuiCol.TitleBgActive,   ImGui.colorConvertFloat4ToU32(new ImVec4(0f/255f, 100f/255f, 255f/255f, 220f/255f)));
        style.setColor(ImGuiCol.MenuBarBg,       ImGui.colorConvertFloat4ToU32(new ImVec4(15f/255f, 15f/255f, 35f/255f, 240f/255f)));

        style.setColor(ImGuiCol.FrameBg,         ImGui.colorConvertFloat4ToU32(new ImVec4(30f/255f, 30f/255f, 60f/255f, 200f/255f)));
        style.setColor(ImGuiCol.FrameBgHovered,  ImGui.colorConvertFloat4ToU32(new ImVec4(0f/255f, 120f/255f, 255f/255f, 180f/255f)));
        style.setColor(ImGuiCol.FrameBgActive,   ImGui.colorConvertFloat4ToU32(new ImVec4(0f/255f, 140f/255f, 255f/255f, 220f/255f)));

        style.setColor(ImGuiCol.Button,          ImGui.colorConvertFloat4ToU32(new ImVec4(0f/255f, 80f/255f, 200f/255f, 180f/255f)));
        style.setColor(ImGuiCol.ButtonHovered,   ImGui.colorConvertFloat4ToU32(new ImVec4(0f/255f, 120f/255f, 255f/255f, 220f/255f)));
        style.setColor(ImGuiCol.ButtonActive,    ImGui.colorConvertFloat4ToU32(new ImVec4(0f/255f, 160f/255f, 255f/255f, 255f/255f)));

        style.setColor(ImGuiCol.SliderGrab,      ImGui.colorConvertFloat4ToU32(new ImVec4(0f/255f, 140f/255f, 255f/255f, 220f/255f)));
        style.setColor(ImGuiCol.SliderGrabActive,ImGui.colorConvertFloat4ToU32(new ImVec4(0f/255f, 180f/255f, 255f/255f, 255f/255f)));

        style.setColor(ImGuiCol.CheckMark,       ImGui.colorConvertFloat4ToU32(new ImVec4(0f/255f, 160f/255f, 255f/255f, 255f/255f)));
        style.setColor(ImGuiCol.Tab,             ImGui.colorConvertFloat4ToU32(new ImVec4(30f/255f, 30f/255f, 60f/255f, 200f/255f)));
        style.setColor(ImGuiCol.TabHovered,      ImGui.colorConvertFloat4ToU32(new ImVec4(0f/255f, 120f/255f, 255f/255f, 220f/255f)));
        style.setColor(ImGuiCol.TabActive,       ImGui.colorConvertFloat4ToU32(new ImVec4(0f/255f, 100f/255f, 255f/255f, 255f/255f)));

        style.setColor(ImGuiCol.ScrollbarBg,     ImGui.colorConvertFloat4ToU32(new ImVec4(10f/255f, 10f/255f, 20f/255f, 180f/255f)));
        style.setColor(ImGuiCol.ScrollbarGrab,   ImGui.colorConvertFloat4ToU32(new ImVec4(0f/255f, 100f/255f, 255f/255f, 200f/255f)));
        style.setColor(ImGuiCol.ScrollbarGrabHovered, ImGui.colorConvertFloat4ToU32(new ImVec4(0f/255f, 140f/255f, 255f/255f, 220f/255f)));
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        ImGui.getIO().setMouseWheel((float) delta);
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
