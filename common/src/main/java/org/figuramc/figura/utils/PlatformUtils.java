package org.figuramc.figura.utils;


import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;

public class PlatformUtils {

    // Quasar MC: NeoForge-only build, so call the NeoForge implementation directly.
    public static Path getGameDir() {
        return org.figuramc.figura.utils.neoforge.PlatformUtilsImpl.getGameDir();
    }

    // Quasar MC: NeoForge-only build, so call the NeoForge implementation directly.
    public static String getFiguraModVersionString() {
        return org.figuramc.figura.utils.neoforge.PlatformUtilsImpl.getFiguraModVersionString();
    }

    // Quasar MC: NeoForge-only build, so call the NeoForge implementation directly.
    public static Path getConfigDir() {
        return org.figuramc.figura.utils.neoforge.PlatformUtilsImpl.getConfigDir();
    }

    // Quasar MC: NeoForge-only build, so call the NeoForge implementation directly.
    public static boolean isModLoaded(String modId) {
        return org.figuramc.figura.utils.neoforge.PlatformUtilsImpl.isModLoaded(modId);
    }

    // Quasar MC: NeoForge-only build, so call the NeoForge implementation directly.
    public static String getModVersion(String modId) {
        return org.figuramc.figura.utils.neoforge.PlatformUtilsImpl.getModVersion(modId);
    }

    public static int compareVersionTo(String v1, String v2) {
        if(v1 == null)
            return 1;
        String[] v1Parts = v1.split("[+,_]")[0].split("\\.");
        String[] v2Parts = v2.split("[+,_]")[0].split("\\.");
        int length = Math.max(v1Parts.length, v2Parts.length);
        for(int i = 0; i < length; i++) {
            int v1Part = i < v1Parts.length ?
                    Integer.parseInt(v1Parts[i]) : 0;
            int v2Part = i < v2Parts.length ?
                    Integer.parseInt(v2Parts[i]) : 0;
            if(v1Part < v2Part)
                return -1;
            if(v1Part > v2Part)
                return 1;
        }
        return 0;
    }

    public enum ModLoader {
        FORGE,
        FABRIC
    }

    // Quasar MC: NeoForge-only build, so call the NeoForge implementation directly.
    public static ModLoader getModLoader() {
        return org.figuramc.figura.utils.neoforge.PlatformUtilsImpl.getModLoader();
    }

    // Quasar MC: NeoForge-only build, so call the NeoForge implementation directly.
    public static InputStream loadFileFromRoot(String file) throws FileNotFoundException {
        return org.figuramc.figura.utils.neoforge.PlatformUtilsImpl.loadFileFromRoot(file);
    }
}
