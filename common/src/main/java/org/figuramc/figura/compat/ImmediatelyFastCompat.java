package org.figuramc.figura.compat;

import org.figuramc.figura.utils.PlatformUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

// Quasar MC: rewritten to use reflection only, so this build does not need ImmediatelyFast at compile time.
public class ImmediatelyFastCompat {
    public static float getFontWidthIMF() {
        if (!PlatformUtils.isModLoaded("immediatelyfast"))
            return 256.0f;

        // ImmediatelyFast 1.2.0+: ImmediatelyFast.config.getBoolean("font_atlas_resizing", false)
        try {
            Class<?> modClass = Class.forName("net.raphimc.immediatelyfast.ImmediatelyFast");
            Object config = modClass.getField("config").get(null);
            Method getBoolean = config.getClass().getMethod("getBoolean", String.class, boolean.class);
            return (boolean) getBoolean.invoke(config, "font_atlas_resizing", false) ? 2048.0f : 256.0f;
        } catch (ReflectiveOperationException | ClassCastException ignored) {
        }

        // Older versions: a config object with a boolean field "font_atlas_resizing"
        for (String fieldName : new String[]{"runtimeConfig", "config"}) {
            try {
                Class<?> modClass = Class.forName("net.raphimc.immediatelyfast.ImmediatelyFast");
                Field configField = modClass.getDeclaredField(fieldName);
                configField.setAccessible(true);
                Object config = configField.get(null);
                Field resizing = config.getClass().getDeclaredField("font_atlas_resizing");
                resizing.setAccessible(true);
                return resizing.getBoolean(config) ? 2048.0f : 256.0f;
            } catch (ReflectiveOperationException | NullPointerException ignored) {
            }
        }
        return 256.0f;
    }
}
