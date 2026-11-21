package org.figuramc.figura.mixin.render.nodeRenderer;

import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.neoforged.neoforge.client.submit.RenderPhaseKeys;
import org.apache.commons.lang3.function.TriFunction;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.ducks.NodeCollectorExtension;
import org.figuramc.figura.model.rendering.nodeRenderer.FiguraSubmission;
import org.figuramc.figura.utils.FiguraBufferSource;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Quasar MC (26.3): Figura avatars are submitted as a custom SubmitNode through NeoForge's
 * submitSpecial and drawn by FiguraFeatureRenderer.
 *
 * TODO(quasar): upstream also copied pre/post rendering callbacks from vanilla Models/ModelParts/items
 * onto their submit records here (used to attach Figura parts to vanilla models and to cancel vanilla
 * rendering). In 26.3 those records are ModelFeatureRenderer$Submit / ItemFeatureRenderer$Submit /
 * FlameFeatureRenderer$Submit; re-add with @ModifyExpressionValue on their NEW in submitModel/submitItem/submitFlame.
 */
@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionMixin implements NodeCollectorExtension {
    @Override
    @SuppressWarnings("unchecked")
    public <S extends EntityRenderState> void submitFiguraModel(Avatar avatar, S renderState, TriFunction<Avatar, S, FiguraBufferSource, Void> renderer) {
        ((SubmitNodeCollection) (Object) this).submitSpecial(RenderPhaseKeys.SOLID,
                new FiguraSubmission(avatar, renderState, (TriFunction<Avatar, EntityRenderState, FiguraBufferSource, Void>) (TriFunction<?, ?, ?, ?>) renderer));
    }
}
