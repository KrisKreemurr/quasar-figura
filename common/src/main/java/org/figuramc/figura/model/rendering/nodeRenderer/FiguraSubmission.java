package org.figuramc.figura.model.rendering.nodeRenderer;

import org.figuramc.figura.utils.FiguraBufferSource;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.apache.commons.lang3.function.TriFunction;
import org.figuramc.figura.avatar.Avatar;

public record FiguraSubmission(Avatar avatar, EntityRenderState renderState, TriFunction<Avatar, EntityRenderState, FiguraBufferSource, Void> renderer) {

}
