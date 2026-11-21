package org.figuramc.figura.compat;

// Quasar MC: Iris is OpenGL-only and the Quasar MC pack runs on Minecraft's Vulkan backend,
// so this build does not compile against Iris. Kept as a no-op so callers stay unchanged.
public class IrisCompat {
    public static void assignPipelinesToIrisPrograms() {
    }
}
