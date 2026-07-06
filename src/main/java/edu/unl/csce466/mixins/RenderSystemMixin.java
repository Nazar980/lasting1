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
    private static final Logger LOGGER = LogManager.getLogger();

    @Inject(at = @At(value = "TAIL"), method = "initRenderer", remap = true)
    private static void initRenderer(CallbackInfo cbi) {
        if (!RenderSystem.isOnRenderThread()) {
            throw new IllegalStateException("Expected to be on render thread");
        }

        if (ImGuiRenderer.getInstance().isInitialized()) {
            return;
        }

        LOGGER.info("Initializing ImGui");

        ImGuiRenderer.getInstance().init(() -> {
            ImGui.getIO().addConfigFlags(ImGuiConfigFlags.DockingEnable);
        });
    }

    @Inject(at = @At(value = "HEAD"), method = "flipFrame(J)V", remap = true)
    private static void flipFrame(long p_69496_, CallbackInfo cbi) {
        RenderSystem.recordRenderCall(() -> {
            ImGuiRenderer.getInstance().render();
        });
    }
}
