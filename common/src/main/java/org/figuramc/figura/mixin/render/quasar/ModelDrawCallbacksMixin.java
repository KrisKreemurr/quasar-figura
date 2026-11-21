package org.figuramc.figura.mixin.render.quasar;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.figuramc.figura.model.rendering.nodeRenderer.FiguraCallbackFrame;
import org.figuramc.figura.utils.FiguraBufferSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Quasar MC (26.3): runs Figura's pre/post callbacks around the actual draw of a vanilla model.
 * Pre callbacks apply vanilla_model changes (hiding/moving parts) and may cancel the draw; post callbacks restore.
 */
@Mixin(ModelFeatureRenderer.class)
public abstract class ModelDrawCallbacksMixin extends RenderTypeFeatureRenderer<ModelFeatureRenderer.Submit<?>> {
    @Shadow @Final private PoseStack poseStack;

    @WrapOperation(method = "prepareModel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    private void figura$aroundDraw(Model<?> model, PoseStack stack, VertexConsumer consumer, int light, int overlay, int color,
                                   Operation<Void> original, @Local(argsOnly = true) ModelFeatureRenderer.Submit<?> submit) {
        FiguraCallbackFrame.Bucket bucket = FiguraCallbackFrame.take(submit.model(), submit.state());
        if (bucket == null || bucket.isEmpty()) {
            original.call(model, stack, consumer, light, overlay, color);
            return;
        }

        boolean[] switched = {false};
        FiguraBufferSource bufferSource = renderType -> {
            switched[0] = true;
            return this.getVertexBuilder(renderType);
        };

        boolean draw = true;
        for (var callback : bucket.pre()) {
            if (!callback.apply(bufferSource, this.poseStack))
                draw = false;
        }

        if (draw) {
            VertexConsumer target = consumer;
            if (switched[0]) {
                // a callback drew into another render type; re-fetch this model's builder to be safe
                RenderType renderType = submit.renderType();
                target = this.getVertexBuilder(renderType);
                if (submit.uvMapping() != null) target = submit.uvMapping().wrap(target);
            }
            original.call(model, stack, target, light, overlay, color);
        }

        for (Runnable callback : bucket.post())
            callback.run();
    }
}
