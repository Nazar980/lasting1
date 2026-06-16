package edu.unl.csce466.screens;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.authlib.GameProfile;
import edu.unl.csce466.imgui.ImGuiRenderer;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiSelectableFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.network.play.client.CChatMessagePacket;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class ImGuiScreen extends Screen {
    private static ImGuiScreen INSTANCE = null;
    private static final String POPUP_RULES = "SelectRulePopup";
    private static final String POPUP_PLAYERS = "SelectPlayerPopup";

    // ---------- Цвета (розовый заголовок) ----------
    private final float[] colWindowBg = {30/255f, 30/255f, 35/255f, 220/255f};
    private final float[] colTitleBg = {245/255f, 70/255f, 130/255f, 220/255f};
    private final float[] colTitleBgActive = {245/255f, 70/255f, 130/255f, 220/255f};
    private final float[] colTitleBgCollapsed = {245/255f, 70/255f, 130/255f, 160/255f};
    private final float[] colText = {1f, 1f, 1f, 1f};
    private final float[] colTextDisabled = {180/255f, 180/255f, 190/255f, 140/255f};
    private final float[] colTab = {42/255f, 42/255f, 42/255f, 180/255f};
    private final float[] colTabHovered = {60/255f, 60/255f, 65/255f, 200/255f};
    private final float[] colTabActive = {50/255f, 50/255f, 55/255f, 220/255f};
    private final float[] colButton = {70/255f, 70/255f, 80/255f, 220/255f};
    private final float[] colButtonHovered = {90/255f, 90/255f, 100/255f, 230/255f};
    private final float[] colButtonActive = {110/255f, 110/255f, 120/255f, 240/255f};
    private final float[] colCheckMark = {245/255f, 70/255f, 130/255f, 1f};
    private final float[] colFrameBg = {50/255f, 50/255f, 55/255f, 220/255f};
    private final float[] colFrameBgHovered = {70/255f, 70/255f, 80/255f, 230/255f};
    private final float[] colFrameBgActive = {90/255f, 90/255f, 100/255f, 240/255f};
    private final float[] colHeader = {90/255f, 40/255f, 60/255f, 220/255f};
    private final float[] colHeaderHovered = {130/255f, 55/255f, 85/255f, 230/255f};
    private final float[] colHeaderActive = {245/255f, 70/255f, 130/255f, 220/255f};

    // ---------- Правила ----------
    private static class Rule {
        final String id;        // "1.5", "1.5.1", ...
        final String desc;      // "Использование читов"
        final String duration;  // "14d" / "8d" / "5h" / "forever"
        final String category;  // "main" или "game"
        Rule(String id, String desc, String duration, String category) {
            this.id = id; this.desc = desc; this.duration = duration; this.category = category;
        }
        String commandReason() {
            return id + " [" + desc + "]";
        }
    }

    private static final List<Rule> RULES = new ArrayList<Rule>();
    static {
        // ---- Основные правила ----
        RULES.add(new Rule("1.5",   "Использование читов",                                  "14d",       "main"));
        RULES.add(new Rule("1.5.1", "Тим с читером",                                        "8d",        "main"));
        RULES.add(new Rule("1.5.2", "Клан читеров (каждый)",                                "14d",       "main"));
        RULES.add(new Rule("1.6",   "Признание в использовании читов",                     "12d",       "main"));
        RULES.add(new Rule("1.7",   "Ник похож на ник администрации / ютуберов (навсегда)", "forever",   "main"));
        RULES.add(new Rule("1.8",   "Использование DDoS пакетов",                           "28d",       "main"));
        RULES.add(new Rule("1.8.1", "Попытка краша сервера (навсегда)",                     "forever",   "main"));
        RULES.add(new Rule("1.9",   "Отказ от проверки",                                    "14d",       "main"));
        RULES.add(new Rule("2.0",   "Задерживание модератора во время проверки",            "14d",       "main"));
        RULES.add(new Rule("2.1",   "Выдача себя за модерацию проекта",                     "20d",       "main"));
        RULES.add(new Rule("2.5",   "Больше 5 аккаунтов в бане (каждый новый аккаунт)",     "14d",       "main"));
        RULES.add(new Rule("2.6",   "Обход бана (навсегда)",                                "forever",   "main"));
        RULES.add(new Rule("2.7",   "Покупка доната через сторонние маркетплейсы (навсегда)","forever",   "main"));
        // ---- Игровые правила ----
        RULES.add(new Rule("2.8",   "Заливание дома лавой/водой",                           "5h",        "game"));
    }

    // ---------- Состояние UI ----------
    private Rule selectedRule = null;
    private String selectedPlayerName = "";
    private final ImString offlineNameBuf = new ImString("", 32);
    private boolean useOffline = false;
    private String statusMessage = "";
    private float statusTimer = 0f;

    // Окно Style Editor / Demo — оставляем в Tools меню
    private final ImBoolean showStyleEditor = new ImBoolean(false);
    private final ImBoolean showDemo = new ImBoolean(false);

    public static ImGuiScreen getInstance() {
        if (INSTANCE == null) INSTANCE = new ImGuiScreen();
        return INSTANCE;
    }

    private ImGuiScreen() {
        super(new StringTextComponent("Ban Assistant"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(MatrixStack poseStack, int mouseX, int mouseY, float partialTick) {
        ImGuiRenderer renderer = ImGuiRenderer.getInstance();
        Minecraft mc = Minecraft.getInstance();
        ImGuiIO io = ImGui.getIO();

        renderer.draw(() -> {
            applyCurrentColors();

            ImGui.setNextWindowSize(600, 500, ImGuiCond.FirstUseEver);
            ImGui.setNextWindowPos(
                (io.getDisplaySizeX() - 600) * 0.5f,
                (io.getDisplaySizeY() - 500) * 0.5f,
                ImGuiCond.FirstUseEver
            );

            int flags = ImGuiWindowFlags.None;
            ImGui.begin("Ban Assistant", flags);

            // ==== Менюбар (Tools -> Style Editor / Demo) ====
            if (ImGui.beginMainMenuBar()) {
                if (ImGui.beginMenu("Tools")) {
                    if (ImGui.menuItem("Style Color Editor", "", showStyleEditor.get())) {
                        showStyleEditor.set(!showStyleEditor.get());
                    }
                    if (ImGui.menuItem("Show ImGui Demo Window", "", showDemo.get())) {
                        showDemo.set(!showDemo.get());
                    }
                    ImGui.endMenu();
                }
                if (ImGui.beginMenu("Help")) {
                    ImGui.text("Ban Assistant for Minecraft 1.16.5");
                    ImGui.textWrapped("1. Select rule → 2. Select online player OR type offline name → 3. Press Ban player.");
                    ImGui.endMenu();
                }
                ImGui.endMainMenuBar();
            }

            ImGui.spacing();
            ImGui.textColored(245/255f, 70/255f, 130/255f, 1f, "Ban Assistant");
            ImGui.separator();
            ImGui.spacing();

            // ==== Блок 1: Выбор правила ====
            ImGui.text("1) Rule:");
            if (selectedRule == null) {
                ImGui.textDisabled("  (no rule selected)");
            } else {
                ImGui.bulletText(selectedRule.id + " — " + selectedRule.desc);
                ImGui.bulletText("Duration: " + ("forever".equals(selectedRule.duration)
                        ? "PERMANENT BAN"
                        : selectedRule.duration));
            }
            if (ImGui.button("Select rule...")) {
                ImGui.openPopup(POPUP_RULES);
            }
            drawRulePopup();

            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();

            // ==== Блок 2: Выбор игрока ====
            ImGui.text("2) Player:");

            // Режим онлайн / оффлайн
            if (ImGui.radioButton("Online player (from tab list)", !useOffline)) {
                useOffline = false;
            }
            if (ImGui.radioButton("Offline / manual nickname", useOffline)) {
                useOffline = true;
            }
            ImGui.spacing();

            if (!useOffline) {
                if (selectedPlayerName.isEmpty()) {
                    ImGui.textDisabled("  (no player selected)");
                } else {
                    ImGui.bulletText("Selected: " + selectedPlayerName);
                }
                if (ImGui.button("Select player...")) {
                    ImGui.openPopup(POPUP_PLAYERS);
                }
                drawPlayerPopup();
            } else {
                ImGui.text("Nickname:");
                ImGui.setNextItemWidth(-1);
                ImGui.inputText("##offlinename", offlineNameBuf);
            }

            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();

            // ==== Блок 3: Бан ====
            ImGui.text("3) Ban:");
            ImGui.spacing();

            String targetName = useOffline ? offlineNameBuf.get().trim() : selectedPlayerName;
            boolean canBan = selectedRule != null && !targetName.isEmpty() && mc.player != null;

            if (!canBan) {
                // Делаем кнопку визуально "серой", если условия не выполнены
                ImGui.pushStyleColor(ImGuiCol.Button,        colButton[0] * 0.6f, colButton[1] * 0.6f, colButton[2] * 0.6f, colButton[3]);
                ImGui.pushStyleColor(ImGuiCol.ButtonHovered, colButton[0] * 0.6f, colButton[1] * 0.6f, colButton[2] * 0.6f, colButton[3]);
                ImGui.pushStyleColor(ImGuiCol.ButtonActive,  colButton[0] * 0.6f, colButton[1] * 0.6f, colButton[2] * 0.6f, colButton[3]);
            }
            if (ImGui.button("Ban player", -1, 36)) {
                if (canBan) {
                    executeBan(targetName);
                }
            }
            if (!canBan) {
                ImGui.popStyleColor(3);
                if (ImGui.isItemHovered()) {
                    ImGui.setTooltip("Select a rule AND a player (online or offline) first.");
                }
            }

            // Предпросмотр команды
            if (canBan) {
                ImGui.spacing();
                ImGui.textWrapped("Command to execute:");
                ImGui.textColored(0.4f, 0.8f, 1f, 1f, buildCommand(targetName));
            }

            // Статус
            if (statusTimer > 0f && !statusMessage.isEmpty()) {
                ImGui.spacing();
                ImGui.separator();
                ImGui.textWrapped(statusMessage);
                statusTimer -= io.getDeltaTime();
                if (statusTimer <= 0f) statusMessage = "";
            }

            ImGui.end();

            // ==== Дочерние окна ====
            if (showStyleEditor.get()) {
                drawStyleEditor();
            }
            if (showDemo.get()) {
                ImGui.showDemoWindow(showDemo);
            }
        });
    }

    // ================= Popup: выбор правила =================
    private void drawRulePopup() {
        ImGui.setNextWindowSize(520, 480, ImGuiCond.Appearing);
        if (ImGui.beginPopupModal(POPUP_RULES)) {
            ImGui.text("Select a rule for the ban:");
            ImGui.separator();

            // Группируем по категориям
            if (ImGui.collapsingHeader("Основные правила", ImGuiSelectableFlags.DontClosePopups)) {
                for (Rule r : RULES) {
                    if (!"main".equals(r.category)) continue;
                    String label = r.id + " — " + r.desc
                            + "  (" + ("forever".equals(r.duration) ? "PERM" : r.duration) + ")";
                    if (ImGui.selectable(label)) {
                        selectedRule = r;
                        ImGui.closeCurrentPopup();
                    }
                }
            }
            if (ImGui.collapsingHeader("Игровые правила", ImGuiSelectableFlags.DontClosePopups)) {
                for (Rule r : RULES) {
                    if (!"game".equals(r.category)) continue;
                    String label = r.id + " — " + r.desc
                            + "  (" + r.duration + ")";
                    if (ImGui.selectable(label)) {
                        selectedRule = r;
                        ImGui.closeCurrentPopup();
                    }
                }
            }

            ImGui.spacing();
            ImGui.separator();
            if (ImGui.button("Cancel", -1, 0)) {
                ImGui.closeCurrentPopup();
            }
            ImGui.endPopup();
        }
    }

    // ================= Popup: выбор онлайн-игрока =================
    private void drawPlayerPopup() {
        ImGui.setNextWindowSize(340, 400, ImGuiCond.Appearing);
        if (ImGui.beginPopupModal(POPUP_PLAYERS)) {
            Minecraft mc = Minecraft.getInstance();
            ImGui.text("Online players:");
            ImGui.separator();

            List<NetworkPlayerInfo> players = new ArrayList<NetworkPlayerInfo>();
            if (mc.getConnection() != null) {
                Collection<NetworkPlayerInfo> online = mc.getConnection().getOnlinePlayers();
                if (online != null) players.addAll(online);
                players.sort(Comparator.comparing(p -> p.getProfile().getName().toLowerCase()));
            }

            if (players.isEmpty()) {
                ImGui.textDisabled("(no online players or not connected)");
            } else {
                ImGui.beginChild("playersList", 0, -40, true);
                for (NetworkPlayerInfo info : players) {
                    GameProfile prof = info.getProfile();
                    if (prof == null) continue;
                    String name = prof.getName();
                    if (name == null || name.isEmpty()) continue;
                    if (ImGui.selectable(name)) {
                        selectedPlayerName = name;
                        useOffline = false;
                        ImGui.closeCurrentPopup();
                    }
                }
                ImGui.endChild();
            }

            if (ImGui.button("Cancel", -1, 0)) {
                ImGui.closeCurrentPopup();
            }
            ImGui.endPopup();
        }
    }

    // ================= Выполнение бана =================
    private String buildCommand(String targetName) {
        if (selectedRule == null) return "";
        if ("forever".equals(selectedRule.duration)) {
            return "/ban " + targetName + " " + selectedRule.commandReason();
        }
        return "/tempban " + targetName + " " + selectedRule.duration + " " + selectedRule.commandReason();
    }

    private void executeBan(String targetName) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            setStatus("Error: no local player.");
            return;
        }
        String cmd = buildCommand(targetName);
        if (cmd.isEmpty()) {
            setStatus("Error: nothing to execute.");
            return;
        }
        // Отправляем команду как клиент от своего имени (как если бы игрок ввёл её в чат)
        // 1.16.5: метод называется send(), не sendPacket()
        mc.player.connection.send(new CChatMessagePacket(cmd));
        setStatus("Ban command sent: " + cmd);
    }

    private void setStatus(String msg) {
        statusMessage = msg;
        statusTimer = 6f;
    }

    // ================= Окно Style Color Editor =================
    private void drawStyleEditor() {
        ImGui.begin("Style Color Editor", showStyleEditor, ImGuiWindowFlags.NoCollapse);
        ImGui.text("Change menu colors live (apply immediately):");
        ImGui.separator();

        ImGui.colorEdit4("WindowBg", colWindowBg);
        ImGui.colorEdit4("TitleBg", colTitleBg);
        ImGui.colorEdit4("TitleBgActive", colTitleBgActive);
        ImGui.colorEdit4("TitleBgCollapsed", colTitleBgCollapsed);
        ImGui.separator();
        ImGui.colorEdit4("Text", colText);
        ImGui.colorEdit4("TextDisabled", colTextDisabled);
        ImGui.separator();
        ImGui.colorEdit4("Tab", colTab);
        ImGui.colorEdit4("TabHovered", colTabHovered);
        ImGui.colorEdit4("TabActive", colTabActive);
        ImGui.separator();
        ImGui.colorEdit4("Button", colButton);
        ImGui.colorEdit4("ButtonHovered", colButtonHovered);
        ImGui.colorEdit4("ButtonActive", colButtonActive);
        ImGui.separator();
        ImGui.colorEdit4("CheckMark", colCheckMark);
        ImGui.colorEdit4("FrameBg", colFrameBg);
        ImGui.colorEdit4("FrameBgHovered", colFrameBgHovered);
        ImGui.colorEdit4("FrameBgActive", colFrameBgActive);
        ImGui.colorEdit4("Header", colHeader);
        ImGui.colorEdit4("HeaderHovered", colHeaderHovered);
        ImGui.colorEdit4("HeaderActive", colHeaderActive);

        ImGui.spacing();
        ImGui.separator();
        if (ImGui.button("Reset to Default (pink title)")) {
            resetColorsToDefault();
        }
        ImGui.end();
    }

    // ================= Применение/сброс стилей =================
    private void applyCurrentColors() {
        ImGuiStyle style = ImGui.getStyle();

        style.setColor(ImGuiCol.WindowBg, colWindowBg[0], colWindowBg[1], colWindowBg[2], colWindowBg[3]);
        style.setColor(ImGuiCol.TitleBg, colTitleBg[0], colTitleBg[1], colTitleBg[2], colTitleBg[3]);
        style.setColor(ImGuiCol.TitleBgActive, colTitleBgActive[0], colTitleBgActive[1], colTitleBgActive[2], colTitleBgActive[3]);
        style.setColor(ImGuiCol.TitleBgCollapsed, colTitleBgCollapsed[0], colTitleBgCollapsed[1], colTitleBgCollapsed[2], colTitleBgCollapsed[3]);
        style.setColor(ImGuiCol.Text, colText[0], colText[1], colText[2], colText[3]);
        style.setColor(ImGuiCol.TextDisabled, colTextDisabled[0], colTextDisabled[1], colTextDisabled[2], colTextDisabled[3]);
        style.setColor(ImGuiCol.Tab, colTab[0], colTab[1], colTab[2], colTab[3]);
        style.setColor(ImGuiCol.TabHovered, colTabHovered[0], colTabHovered[1], colTabHovered[2], colTabHovered[3]);
        style.setColor(ImGuiCol.TabActive, colTabActive[0], colTabActive[1], colTabActive[2], colTabActive[3]);
        style.setColor(ImGuiCol.Button, colButton[0], colButton[1], colButton[2], colButton[3]);
        style.setColor(ImGuiCol.ButtonHovered, colButtonHovered[0], colButtonHovered[1], colButtonHovered[2], colButtonHovered[3]);
        style.setColor(ImGuiCol.ButtonActive, colButtonActive[0], colButtonActive[1], colButtonActive[2], colButtonActive[3]);
        style.setColor(ImGuiCol.CheckMark, colCheckMark[0], colCheckMark[1], colCheckMark[2], colCheckMark[3]);
        style.setColor(ImGuiCol.FrameBg, colFrameBg[0], colFrameBg[1], colFrameBg[2], colFrameBg[3]);
        style.setColor(ImGuiCol.FrameBgHovered, colFrameBgHovered[0], colFrameBgHovered[1], colFrameBgHovered[2], colFrameBgHovered[3]);
        style.setColor(ImGuiCol.FrameBgActive, colFrameBgActive[0], colFrameBgActive[1], colFrameBgActive[2], colFrameBgActive[3]);
        style.setColor(ImGuiCol.Header, colHeader[0], colHeader[1], colHeader[2], colHeader[3]);
        style.setColor(ImGuiCol.HeaderHovered, colHeaderHovered[0], colHeaderHovered[1], colHeaderHovered[2], colHeaderHovered[3]);
        style.setColor(ImGuiCol.HeaderActive, colHeaderActive[0], colHeaderActive[1], colHeaderActive[2], colHeaderActive[3]);

        style.setWindowRounding(2.0f);
        style.setFrameRounding(2.0f);
        style.setTabRounding(2.0f);
        style.setGrabRounding(2.0f);
        style.setScrollbarRounding(2.0f);
        style.setPopupRounding(2.0f);
        style.setWindowPadding(10.0f, 10.0f);
        style.setFramePadding(8.0f, 5.0f);
        style.setItemSpacing(8.0f, 6.0f);
    }

    private void resetColorsToDefault() {
        colWindowBg[0] = 30/255f; colWindowBg[1] = 30/255f; colWindowBg[2] = 35/255f; colWindowBg[3] = 220/255f;
        colTitleBg[0] = 245/255f; colTitleBg[1] = 70/255f; colTitleBg[2] = 130/255f; colTitleBg[3] = 220/255f;
        colTitleBgActive[0] = 245/255f; colTitleBgActive[1] = 70/255f; colTitleBgActive[2] = 130/255f; colTitleBgActive[3] = 220/255f;
        colTitleBgCollapsed[0] = 245/255f; colTitleBgCollapsed[1] = 70/255f; colTitleBgCollapsed[2] = 130/255f; colTitleBgCollapsed[3] = 160/255f;
        colText[0] = 1f; colText[1] = 1f; colText[2] = 1f; colText[3] = 1f;
        colTextDisabled[0] = 180/255f; colTextDisabled[1] = 180/255f; colTextDisabled[2] = 190/255f; colTextDisabled[3] = 140/255f;
        colTab[0] = 42/255f; colTab[1] = 42/255f; colTab[2] = 42/255f; colTab[3] = 180/255f;
        colTabHovered[0] = 60/255f; colTabHovered[1] = 60/255f; colTabHovered[2] = 65/255f; colTabHovered[3] = 200/255f;
        colTabActive[0] = 50/255f; colTabActive[1] = 50/255f; colTabActive[2] = 55/255f; colTabActive[3] = 220/255f;
        colButton[0] = 70/255f; colButton[1] = 70/255f; colButton[2] = 80/255f; colButton[3] = 220/255f;
        colButtonHovered[0] = 90/255f; colButtonHovered[1] = 90/255f; colButtonHovered[2] = 100/255f; colButtonHovered[3] = 230/255f;
        colButtonActive[0] = 110/255f; colButtonActive[1] = 110/255f; colButtonActive[2] = 120/255f; colButtonActive[3] = 240/255f;
        colCheckMark[0] = 245/255f; colCheckMark[1] = 70/255f; colCheckMark[2] = 130/255f; colCheckMark[3] = 1f;
        colFrameBg[0] = 50/255f; colFrameBg[1] = 50/255f; colFrameBg[2] = 55/255f; colFrameBg[3] = 220/255f;
        colFrameBgHovered[0] = 70/255f; colFrameBgHovered[1] = 70/255f; colFrameBgHovered[2] = 80/255f; colFrameBgHovered[3] = 230/255f;
        colFrameBgActive[0] = 90/255f; colFrameBgActive[1] = 90/255f; colFrameBgActive[2] = 100/255f; colFrameBgActive[3] = 240/255f;
        colHeader[0] = 90/255f; colHeader[1] = 40/255f; colHeader[2] = 60/255f; colHeader[3] = 220/255f;
        colHeaderHovered[0] = 130/255f; colHeaderHovered[1] = 55/255f; colHeaderHovered[2] = 85/255f; colHeaderHovered[3] = 230/255f;
        colHeaderActive[0] = 245/255f; colHeaderActive[1] = 70/255f; colHeaderActive[2] = 130/255f; colHeaderActive[3] = 220/255f;
    }
}
