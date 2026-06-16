package edu.unl.csce466.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import edu.unl.csce466.imgui.ImGuiRenderer;
import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {
    // 1.16.5: com.mojang.logging.LogUtils не существует - используем log4j напрямую
    private static final Logger LOGGER = LogManager.getLogger();

    // 1.16.5: initRenderer(int, boolean), а не initRenderer().
    // Mixin сам найдёт метод по имени, remap = true позволяет работать в обфусцированной среде.
    @Inject(at = @At(value = "TAIL"), method = "initRenderer", remap = true)
    private static void initRenderer(CallbackInfo cbi) {
        // 1.16.5: метода assertOnRenderThread() нет. Заменяем на эквивалентную проверку.
        if (!RenderSystem.isOnRenderThread()) {
            throw new IllegalStateException("Expected to be on render thread");
        }

        // Сделаем init идемпотентным: повторный вызов createContext() крашит ImGui,
        // поэтому оборачиваем в флаг.
        if (ImGuiRenderer.getInstance().isInitialized()) {
            return;
        }

        LOGGER.info("Initializing ImGui");

        ImGuiRenderer.getInstance().init(() -> {
            // Viewports отключены — убрано полностью, чтобы окна не вылазили и не крашились
            // ImGui.getIO().addConfigFlags(ImGuiConfigFlags.ViewportsEnable);  // УДАЛЕНО

            // Docking оставляем (для вкладок внутри окна). Если не нужен — закомментируй
            ImGui.getIO().addConfigFlags(ImGuiConfigFlags.DockingEnable);
        });
    }

    // 1.16.5: сигнатура flipFrame(long) совпадает, но recordRenderCall принимает
    // RenderSystem.IRenderCall (SAM), а не java.lang.Runnable.
    @Inject(at = @At(value = "HEAD"), method = "flipFrame(J)V", remap = true)
    private static void flipFrame(long p_69496_, CallbackInfo cbi) {
        RenderSystem.recordRenderCall(() -> {
            ImGuiRenderer.getInstance().render();
        });
    }
}
