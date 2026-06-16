package edu.unl.csce466.imgui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;
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
        System.out.println("\n\n====== [Ban Assistant] FONT LOADING DEBUG ======");

        // Диапазоны для ImGui: ASCII + Cyrillic
        short[] cyrillicRanges = new short[] { 
            0x0020, 0x007E,  // ASCII
            0x0400, 0x044F,  // Cyrillic (Russian)
            0  // End marker
        };

        // ===== СТРАТЕГИЯ 1: Загружаем из ассетс мода =====
        System.out.println("[FA] Attempt 1: Load from assets (csce466:fonts/Roboto-Regular.ttf)");
        try {
            Minecraft mc = Minecraft.getInstance();
            ResourceLocation fontResource = new ResourceLocation("csce466", "fonts/Roboto-Regular.ttf");
            
            // Прочитаем весь файл в байты
            InputStream is = mc.getResourceManager().getResource(fontResource).getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[16384];
            int nRead;
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            is.close();
            byte[] fontBytes = buffer.toByteArray();
            System.out.println("[FA] ✓ Font loaded from assets: " + fontBytes.length + " bytes");
            
            // Сохраняем во временный файл (ImGui требует путь к файлу)
            String tempPath = System.getProperty("java.io.tmpdir") + File.separator + "csce466_font_" + System.currentTimeMillis() + ".ttf";
            FileOutputStream fos = new FileOutputStream(tempPath);
            fos.write(fontBytes);
            fos.close();
            System.out.println("[FA] ✓ Temp file: " + tempPath);
            
            // Загружаем в ImGui с поддержкой кириллицы
            io.getFonts().addFontFromFileTTF(tempPath, 16.0f, null, cyrillicRanges);
            System.out.println("[FA] ✓✓✓ SUCCESS: Font loaded with Cyrillic support!");
            System.out.println("====== [Ban Assistant] FONT READY ======\n");
            return;
        } catch (Exception e) {
            System.out.println("[FA] ✗ Assets failed: " + e.getClass().getSimpleName());
            e.printStackTrace();
        }

        // ===== СТРАТЕГИЯ 2: Системные шрифты с кириллицей =====
        System.out.println("[FA] Attempt 2: Load from system fonts");
        String[] fontPaths = new String[] {
            // Windows (Arial/Segoe UI have Cyrillic)
            "C:\\Windows\\Fonts\\arial.ttf",
            "C:\\Windows\\Fonts\\segoeui.ttf",
            "C:\\Windows\\Fonts\\calibri.ttf",
            // Linux
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
            "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf",
            // macOS
            "/Library/Fonts/Arial.ttf",
            "/System/Library/Fonts/Helvetica.ttc",
        };

        for (String fontPath : fontPaths) {
            try {
                File f = new File(fontPath);
                if (f.exists() && f.canRead()) {
                    System.out.println("[FA] ✓ Found: " + fontPath);
                    io.getFonts().addFontFromFileTTF(fontPath, 16.0f, null, cyrillicRanges);
                    System.out.println("[FA] ✓✓✓ SUCCESS: System font loaded with Cyrillic!");
                    System.out.println("====== [Ban Assistant] FONT READY ======\n");
                    return;
                }
            } catch (Exception e) {
                System.out.println("[FA] ! " + fontPath + " failed: " + e.getClass().getSimpleName());
            }
        }

        // ===== FALLBACK: Встроенный шрифт ImGui =====
        System.out.println("[FA] ✗✗✗ FALLBACK: Using ImGui default font");
        System.out.println("[FA] WARNING: Cyrillic will display as ????? ");
        System.out.println("[FA]");
        System.out.println("[FA] HOW TO FIX:");
        System.out.println("[FA] 1. Place Roboto-Regular.ttf (or any TTF with Cyrillic) in:");
        System.out.println("[FA]    src/main/resources/assets/csce466/fonts/Roboto-Regular.ttf");
        System.out.println("[FA] 2. OR make sure system has fonts: Arial, DejaVu Sans, Liberation Sans");
        System.out.println("[FA] 3. Download Roboto: https://fonts.google.com/?query=roboto");
        System.out.println("====== [Ban Assistant] FONT INITIALIZATION COMPLETE ======\n");
    }
}
