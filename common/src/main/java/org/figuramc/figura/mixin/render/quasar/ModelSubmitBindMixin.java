package org.figuramc.figura.mixin.render.quasar;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollection;
import org.figuramc.figura.ducks.FiguraSubmitCallBackExtension;
import org.figuramc.figura.model.rendering.nodeRenderer.FiguraCallbackFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Quasar MC (26.3): when a vanilla model is submitted, bind the Figura callbacks waiting on that model
 * (and on its root part, for submitModelPart) to this submission's render state.
 */
@Mixin(SubmitNodeCollection.class)
public class ModelSubmitBindMixin {
    @Inject(method = "submitModel", at = @At("HEAD"))
    private void figura$bindCallbacks(CallbackInfo ci, @Local(argsOnly = true) Model<?> model, @Local(argsOnly = true) Object state) {
        FiguraCallbackFrame.bindWithState(model, state,
                (FiguraSubmitCallBackExtension) model,
                (FiguraSubmitCallBackExtension) (Object) model.root());
    }
}
