package org.figuramc.figura.mixin.gui;

import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Hud.class) // Quasar MC (26.3): titles/action bar live on Gui.hud
public interface GuiAccessor {
    @Accessor("title")
    Component getTitle();

    @Accessor("subtitle")
    Component getSubtitle();

    @Accessor("overlayMessageString")
    Component getActionbar();

    @Accessor("titleTime")
    int getTime();

    @Accessor("overlayMessageTime")
    int getActionbarTime();
}
