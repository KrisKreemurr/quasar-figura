package org.figuramc.figura.utils;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * Quasar MC: Minecraft 26.x removed {@code MultiBufferSource}. Figura's renderer is built around
 * "give me a VertexConsumer for this RenderType", so this small interface keeps that shape inside
 * Figura, and the integration points adapt it to 26.x's submit/feature-renderer system.
 */
@FunctionalInterface
public interface FiguraBufferSource {
    VertexConsumer getBuffer(RenderType renderType);

    /** Flushes pending geometry, if the implementation batches. No-op by default. */
    default void endBatch() {
    }

    /** No-op in the 26.3 feature renderer (batches are flushed by Minecraft). */
    default void endBatch(RenderType renderType) {
    }

    /** No-op in the 26.3 feature renderer (batches are flushed by Minecraft). */
    default void endLastBatch() {
    }
}
