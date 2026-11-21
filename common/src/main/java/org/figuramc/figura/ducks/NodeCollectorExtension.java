package org.figuramc.figura.ducks;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.apache.commons.lang3.function.TriFunction;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.utils.FiguraBufferSource;

public interface NodeCollectorExtension {
    <S extends EntityRenderState> void submitFiguraModel(Avatar avatar, S renderState, TriFunction<Avatar, S, FiguraBufferSource, Void> renderer);
}
