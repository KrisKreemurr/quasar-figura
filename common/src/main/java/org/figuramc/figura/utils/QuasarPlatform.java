package org.figuramc.figura.utils;

import org.figuramc.figura.FiguraMod;

import java.net.URI;

/**
 * Quasar MC (26.3): Util.OS.openUri() was removed. Open links/folders through SDL (what Minecraft's
 * window uses now), falling back to java.awt.Desktop.
 */
public final class QuasarPlatform {
    private QuasarPlatform() {
    }

    public static void openUri(URI uri) {
        String url = uri.toString();
        try {
            Class<?> sdlMisc = Class.forName("org.lwjgl.sdl.SDLMisc");
            Object ok = sdlMisc.getMethod("SDL_OpenURL", CharSequence.class).invoke(null, url);
            if (!(ok instanceof Boolean b) || b)
                return;
        } catch (Throwable ignored) {
        }
        try {
            java.awt.Desktop.getDesktop().browse(uri);
        } catch (Throwable t) {
            FiguraMod.LOGGER.warn("Failed to open " + url, t);
        }
    }

    public static void openUri(String uri) {
        try {
            openUri(new URI(uri));
        } catch (Exception e) {
            FiguraMod.LOGGER.warn("Invalid URI " + uri, e);
        }
    }
}
