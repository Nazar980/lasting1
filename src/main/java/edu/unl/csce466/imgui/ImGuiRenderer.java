package edu.unl.csce466.imgui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import imgui.ImFont;
import imgui.ImFontConfig;
import imgui.ImFontGlyphRangesBuilder;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class ImGuiRenderer {
    private static ImGuiRenderer INSTANCE = null;

    public static ImGuiRenderer getInstance() {
        if (INSTANCE == null) INSTANCE = new ImGuiRenderer();
        return INSTANCE;
    }

    private ArrayList<ImGuiCall> preDrawCalls = new ArrayList<ImGuiCall>();
    private ArrayList<ImGuiCall> drawCalls = new ArrayList<ImGuiCall>();

    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl = new ImGuiImplGl3();

    // Флаг того, что ImGui-контекст уже создан.
    // Инициализация вызывается один раз из миксина RenderSystemMixin#initRenderer
    // (в момент старта рендера Minecraft), повторные вызовы init() игнорируются.
    private boolean initialized = false;

    private ImGuiRenderer() {
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void init() {
        init(() -> {});
    }

    public void init(ImGuiCall config) {
        ImGui.createContext();
        config.execute();

        // Явно отключаем Viewports - это перекрывает любой дефолт
        ImGui.getIO().setConfigFlags(ImGui.getIO().getConfigFlags() & ~ImGuiConfigFlags.ViewportsEnable);

        // Docking оставляем (для вкладок внутри окна)
        ImGui.getIO().addConfigFlags(ImGuiConfigFlags.DockingEnable);

        // installCallbacks = true: ImGui сам ставит GLFW-коллбэки (key / char / mouse / scroll / cursorpos)
        // и автоматически вызывает предыдущие (майнкрафтовские) через встроенную цепочку.
        imGuiGlfw.init(Minecraft.getInstance().getWindow().getWindow(), true);

        // ===== Загрузка шрифта с поддержкой кириллицы (ПОСЛЕ imGuiGlfw.init!) =====
        loadCyrillicFont();

        try {
            initGl3Renderer("#version 410 core");
            initialized = true;
            return;
        } catch (Exception ignored) {
        }

        try {
            initGl3Renderer("#version 150 core");
            initialized = true;
            return;
        } catch (Exception ignored) {
        }

        throw new RuntimeException("Failed to initialize ImGuiImplGl3");
    }

    private void initGl3Renderer(String glslVersion) {
        try {
            try {
                imGuiGl.getClass().getMethod("init", String.class).invoke(imGuiGl, glslVersion);
                return;
            } catch (NoSuchMethodException ignored) {
            }

            try {
                imGuiGl.getClass().getMethod("init").invoke(imGuiGl);
                return;
            } catch (NoSuchMethodException ignored) {
            }

            try {
                imGuiGl.getClass().getMethod("init", String.class, boolean.class).invoke(imGuiGl, glslVersion, false);
                return;
            } catch (NoSuchMethodException ignored) {
            }

            throw new IllegalStateException("Unsupported ImGuiImplGl3 API: no compatible init() method found");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to initialize ImGuiImplGl3 via reflection", e);
        }
    }

    private void newFrameGl3Renderer() {
        try {
            imGuiGl.getClass().getMethod("newFrame").invoke(imGuiGl);
        } catch (NoSuchMethodException ignored) {
            // Старые версии не требуют newFrame для GL3
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to call ImGuiImplGl3.newFrame() via reflection", e);
        }
    }

    public void preDraw(ImGuiCall drawCall) {
        preDrawCalls.add(drawCall);
    }

    public void draw(ImGuiCall drawCall) {
        drawCalls.add(drawCall);
    }

    public void render() {
        for (ImGuiCall preDrawCall : preDrawCalls) {
            preDrawCall.execute();
        }
        preDrawCalls.clear();

        imGuiGlfw.newFrame();
        newFrameGl3Renderer();
        ImGui.newFrame();

        // Твой ImGui контент здесь (drawCalls)
        for (ImGuiCall drawCall : drawCalls) {
            drawCall.execute();
        }
        drawCalls.clear();

        ImGui.render();
        imGuiGl.renderDrawData(Objects.requireNonNull(ImGui.getDrawData()));

        // Никакого кода для Viewports - он удалён
    }

    // ================= Загрузка шрифта с кириллицей =================
    private void loadCyrillicFont() {
        ImGuiIO io = ImGui.getIO();
        System.out.println("\n\n====== [Ban Assistant] FONT LOADING (ImGuiMemoryTTF Method) ======");

        try {
            // ===== Шаг 1: Читаем шрифт из ассетс в байты =====
            System.out.println("[FA] Step 1: Loading font bytes from assets...");
            byte[] fontBytes = loadFontBytesFromAssets("csce466", "fonts/Roboto-Regular.ttf");
            System.out.println("[FA] ✓ Font bytes loaded: " + fontBytes.length + " bytes");

            // ===== Шаг 2: Добавляем дефолтный шрифт (ASCII) =====
            System.out.println("[FA] Step 2: Adding default font (ASCII)...");
            io.getFonts().addFontDefault();

            // ===== Шаг 3: Создаём builder для диапазонов глифов =====
            System.out.println("[FA] Step 3: Building glyph ranges (ASCII + Cyrillic)...");
            ImFontGlyphRangesBuilder rangesBuilder = new ImFontGlyphRangesBuilder();
            rangesBuilder.addRanges(io.getFonts().getGlyphRangesDefault());  // ASCII
            rangesBuilder.addRanges(io.getFonts().getGlyphRangesCyrillic());  // Cyrillic
            short[] glyphRanges = rangesBuilder.buildRanges();
            System.out.println("[FA] ✓ Glyph ranges built: " + glyphRanges.length + " entries");

            // ===== Шаг 4: Создаём ImFontConfig с merge mode =====
            System.out.println("[FA] Step 4: Creating ImFontConfig with merge mode...");
            ImFontConfig fontConfig = new ImFontConfig();
            fontConfig.setMergeMode(true);  // ВАЖНО: объединяем с дефолтным шрифтом

            // ===== Шаг 5: Добавляем наш шрифт из памяти =====
            System.out.println("[FA] Step 5: Adding font from memory TTF...");
            ImFont customFont = io.getFonts().addFontFromMemoryTTF(fontBytes, 16.0f, fontConfig, glyphRanges);
            System.out.println("[FA] ✓ Font added to atlas: " + (customFont != null ? "SUCCESS" : "NULL"));

            // ===== Шаг 6: Пересобираем атлас (КРИТИЧНО!) =====
            System.out.println("[FA] Step 6: Building font atlas...");
            io.getFonts().build();
            System.out.println("[FA] ✓✓✓ Font atlas built successfully!");

            // ===== Шаг 7: Очищаем config =====
            fontConfig.destroy();
            System.out.println("[FA] ✓ Font config destroyed");

            System.out.println("====== [Ban Assistant] FONT READY - Cyrillic should work! ======\n");
            return;

        } catch (Exception e) {
            System.out.println("[FA] ✗ FAILED with exception: " + e.getClass().getSimpleName());
            System.out.println("[FA] Message: " + e.getMessage());
            e.printStackTrace();
        }

        // ===== FALLBACK: Если всё сломалось =====
        System.out.println("[FA]");
        System.out.println("[FA] ✗✗✗ Font loading failed. Using ImGui default (ASCII only)");
        System.out.println("[FA] Cyrillic will show as ?????");
        System.out.println("[FA]");
        System.out.println("[FA] FIX: Make sure Roboto-Regular.ttf exists in:");
        System.out.println("[FA]   src/main/resources/assets/csce466/fonts/Roboto-Regular.ttf");
        System.out.println("====== [Ban Assistant] FONT INITIALIZATION COMPLETE ======\n");
    }

    // Вспомогательный метод для загрузки файла из ассетс
    private byte[] loadFontBytesFromAssets(String namespace, String path) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        ResourceLocation fontResource = new ResourceLocation(namespace, path);
        
        InputStream is = mc.getResourceManager().getResource(fontResource).getInputStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[16384];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        is.close();
        return buffer.toByteArray();
    }
}
