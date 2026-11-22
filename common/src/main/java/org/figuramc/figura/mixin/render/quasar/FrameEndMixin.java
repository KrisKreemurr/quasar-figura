package org.figuramc.figura.mixin.render.quasar;

import net.minecraft.client.renderer.GameRenderer;
import org.figuramc.figura.model.rendering.nodeRenderer.FiguraCallbackFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Quasar MC (26.3): drop any Figura model callbacks that were never drawn this frame. */
@Mixin(GameRenderer.class)
public class FrameEndMixin {
    @Inject(method = "render", at = @At("RETURN"))
    private void figura$endFrame(CallbackInfo ci) {
        FiguraCallbackFrame.endFrame();
        org.figuramc.figura.quasar.FiguraTimings.endFrame();
        org.figuramc.figura.utils.ui.UIHelper.paperdoll = false;
    }
}
