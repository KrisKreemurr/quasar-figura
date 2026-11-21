package org.figuramc.figura.model.rendering.nodeRenderer;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import org.apache.commons.lang3.function.TriFunction;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.utils.FiguraBufferSource;

/**
 * Quasar MC (26.3): a Figura avatar render request, submitted to Minecraft's feature-render system
 * through NeoForge's {@code submitSpecial} and drawn by {@link FiguraFeatureRenderer}.
 */
public record FiguraSubmission(Avatar avatar, EntityRenderState renderState, TriFunction<Avatar, EntityRenderState, FiguraBufferSource, Void> renderer) implements SubmitNode {
    @Override
    public FeatureRendererType<FiguraSubmission> featureType() {
        return FiguraFeatureRenderer.TYPE;
    }
}
