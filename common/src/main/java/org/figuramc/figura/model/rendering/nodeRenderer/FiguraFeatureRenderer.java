package org.figuramc.figura.model.rendering.nodeRenderer;

import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import org.figuramc.figura.utils.FiguraBufferSource;

import java.util.List;

/**
 * Quasar MC (26.3): draws Figura avatars inside Minecraft's feature-render system.
 * Registered through NeoForge's RegisterFeatureRenderersEvent. Figura's renderer asks for a
 * VertexConsumer per RenderType; we hand it the feature system's vertex builders.
 */
public class FiguraFeatureRenderer extends RenderTypeFeatureRenderer<FiguraSubmission> {
    public static final FeatureRendererType<FiguraSubmission> TYPE = FeatureRendererType.create("figura_avatar");

    @Override
    protected void buildGroup(FeatureFrameContext context, List<FiguraSubmission> submissions) {
        FiguraBufferSource bufferSource = this::getVertexBuilder;
        for (FiguraSubmission submission : submissions) {
            if (submission.avatar() == null)
                continue;
            submission.renderer().apply(submission.avatar(), submission.renderState(), bufferSource);
        }
    }
}
