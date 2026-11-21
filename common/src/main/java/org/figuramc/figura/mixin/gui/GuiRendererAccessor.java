package org.figuramc.figura.mixin.gui;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.gui.render.GuiRenderer;
import org.figuramc.figura.utils.FiguraBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiRenderer.class)
public interface GuiRendererAccessor {
    @Accessor("bufferBuilder")
    BufferBuilder figura$getBufferBuilder();

    @Accessor("bufferSource")
    FiguraBufferSource figura$getBufferSource();
}
