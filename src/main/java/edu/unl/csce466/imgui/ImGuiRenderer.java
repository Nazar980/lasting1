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
        ImGui.getIO().setConfigFlags(ImGui.getIO().getConfigFlags() & ~ImGuiConfigFlags.ViewportsEnable);

        ImGui.getIO().addConfigFlags(ImGuiConfigFlags.DockingEnable);
        imGuiGlfw.init(Minecraft.getInstance().getWindow().getWindow(), true);

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

        for (ImGuiCall drawCall : drawCalls) {
            drawCall.execute();
        }
        drawCalls.clear();

        ImGui.render();
        imGuiGl.renderDrawData(Objects.requireNonNull(ImGui.getDrawData()));

    }
    private void loadCyrillicFont() {
        ImGuiIO io = ImGui.getIO();
        System.out.println("\n\n====== [Ban Assistant] FONT LOADING (ImGuiMemoryTTF Method) ======");

        try {
            System.out.println("[FA] Step 1: Loading font bytes from assets...");
            byte[] fontBytes = loadFontBytesFromAssets("csce466", "fonts/Roboto-Regular.ttf");
            System.out.println("[FA] ✓ Font bytes loaded: " + fontBytes.length + " bytes");
            System.out.println("[FA] Step 2: Adding default font (ASCII)...");
            io.getFonts().addFontDefault();

            System.out.println("[FA] Step 3: Building glyph ranges (ASCII + Cyrillic)...");
            ImFontGlyphRangesBuilder rangesBuilder = new ImFontGlyphRangesBuilder();
            rangesBuilder.addRanges(io.getFonts().getGlyphRangesDefault());  
            rangesBuilder.addRanges(io.getFonts().getGlyphRangesCyrillic()); 
            short[] glyphRanges = rangesBuilder.buildRanges();
            System.out.println("[FA] ✓ Glyph ranges built: " + glyphRanges.length + " entries");

            System.out.println("[FA] Step 4: Creating ImFontConfig with merge mode...");
            ImFontConfig fontConfig = new ImFontConfig();
            fontConfig.setMergeMode(true);
            System.out.println("[FA] Step 5: Adding font from memory TTF...");
            ImFont customFont = io.getFonts().addFontFromMemoryTTF(fontBytes, 16.0f, fontConfig, glyphRanges);
            System.out.println("[FA] ✓ Font added to atlas: " + (customFont != null ? "SUCCESS" : "NULL"));
            System.out.println("[FA] Step 6: Building font atlas...");
            io.getFonts().build();
            System.out.println("[FA] ✓✓✓ Font atlas built successfully!");

            fontConfig.destroy();
            System.out.println("[FA] ✓ Font config destroyed");

            System.out.println("====== [Ban Assistant] FONT READY - Cyrillic should work! ======\n");
            return;

        } catch (Exception e) {
            System.out.println("[FA] ✗ FAILED with exception: " + e.getClass().getSimpleName());
            System.out.println("[FA] Message: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("[FA]");
        System.out.println("[FA] ✗✗✗ Font loading failed. Using ImGui default (ASCII only)");
        System.out.println("[FA] Cyrillic will show as ?????");
        System.out.println("[FA]");
        System.out.println("[FA] FIX: Make sure Roboto-Regular.ttf exists in:");
        System.out.println("[FA]   src/main/resources/assets/csce466/fonts/Roboto-Regular.ttf");
        System.out.println("====== [Ban Assistant] FONT INITIALIZATION COMPLETE ======\n");
    }

    private byte[] loadFontBytesFromAssets(String namespace, String path) throws Exception {
        String resourcePath = "assets/" + namespace + "/" + path;
        System.out.println("[FA] Loading from classpath: " + resourcePath);
        
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new Exception("Font resource not found in classpath: " + resourcePath);
        }
        
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[16384];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        is.close();
        
        byte[] result = buffer.toByteArray();
        System.out.println("[FA] ✓ Loaded " + result.length + " bytes from classpath");
        return result;
    }
}
