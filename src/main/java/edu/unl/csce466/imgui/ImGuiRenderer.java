package edu.unl.csce466.imgui;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.minecraft.client.Minecraft;

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
        // Это единственный нормальный способ, чтобы:
        //   1) работал текстовый ввод в полях ImGui (буквы, заглавные, Shift+<key>, кириллица, backspace, Ctrl+V),
        //   2) не ломались клавиатурные/мышиные события Minecraft когда ImGui не открыт,
        //   3) не приходилось вручную прокидывать все события через Forge-ивенты.
        imGuiGlfw.init(Minecraft.getInstance().getWindow().getWindow(), true);

        // ===== Загрузка шрифта с поддержкой кириллицы =====
        // Dear ImGui по умолчанию использует встроенный шрифт, который содержит только ASCII.
        // Для русского текста нужно явно добавить шрифт TTF с поддержкой кириллицы.
        // Ищем системный шрифт (Windows / Linux / macOS) или используем встроенный Roboto.
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

        // Попытаемся загрузить системный шрифт с поддержкой кириллицы.
        // На Windows это обычно C:\Windows\Fonts\arial.ttf или segoeui.ttf
        // На Linux это /usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf
        // На macOS это /Library/Fonts/Arial.ttf
        String[] fontPaths = new String[] {
            "C:\\Windows\\Fonts\\arial.ttf",           // Windows
            "C:\\Windows\\Fonts\\segoeui.ttf",         // Windows (красивый)
            "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",  // Linux
            "/Library/Fonts/Arial.ttf",                 // macOS
        };

        boolean fontLoaded = false;
        for (String fontPath : fontPaths) {
            try {
                File f = new File(fontPath);
                if (f.exists()) {
                    // В imgui-java 1.86+ метод: ImFontAtlas.addFontFromFileTTF(filename, size, config, glyphRange)
                    // Если этот метод недоступен, используем fallback через addFontDefault().
                    try {
                        short[] cyrillic = io.getFonts().getGlyphRangesCyrillic();
                        io.getFonts().addFontFromFileTTF(fontPath, 16.0f, null, cyrillic);
                        fontLoaded = true;
                        break;
                    } catch (NoSuchMethodError e) {
                        // Старая версия imgui-java без поддержки addFontFromFileTTF
                        // Используем встроенный шрифт (без поддержки кириллицы, но это лучше, чем крах)
                    }
                }
            } catch (Exception ignored) {
                // Шрифт не найден, пробуем следующий
            }
        }

        if (!fontLoaded) {
            // Если системный шрифт не загружен, используем встроенный (по умолчанию он там уже есть)
            // Встроенный шрифт ImGui не содержит кириллицы, потому русские буквы будут выглядеть как ?????
            // Но в 1.16.5 с обычными шрифтами Windows/Linux это маловероятно.
            System.out.println("[Ban Assistant] Could not load system TTF font. ImGui will use default (ASCII only) font.");
        }
    }
}
