package org.figuramc.figura.neoforge;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod("figura")
public class FiguraModForge {
    // dummy empty mod class, we are client only
    public FiguraModForge() {
        NeoForge.EVENT_BUS.addListener(FiguraModClientNeoForge::cancelVanillaOverlays);
        // Quasar MC (26.3): register Figura's avatar feature renderer
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RegisterFeatureRenderersEvent event) ->
                event.register(org.figuramc.figura.model.rendering.nodeRenderer.FiguraFeatureRenderer.TYPE,
                        new org.figuramc.figura.model.rendering.nodeRenderer.FiguraFeatureRenderer()));
    }
}
