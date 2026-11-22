package org.figuramc.figura.quasar;

import java.util.Locale;

/**
 * Quasar MC: CPU time Figura spends per frame, shown on the F3 screen.
 * Times are summed over every avatar in a frame, then smoothed.
 */
public final class FiguraTimings {
    public enum Part {
        /** Lua render / post_render events of avatars being drawn */
        RENDER_EVENTS,
        /** building the avatar's model geometry (and its part callbacks) */
        MODEL,
        /** Lua world_render / post_world_render events */
        WORLD_EVENTS,
        /** Blockbench animations being applied/cleared (once per frame) */
        ANIMATIONS,
        /** Lua tick events (20 per second, averaged per frame) */
        TICK
    }

    private static final double SMOOTHING = 0.05;
    private static final long[] frameNanos = new long[Part.values().length];
    private static final double[] smoothedMs = new double[Part.values().length];

    private FiguraTimings() {}

    public static long start() {
        return System.nanoTime();
    }

    public static void stop(Part part, long startNanos) {
        frameNanos[part.ordinal()] += System.nanoTime() - startNanos;
    }

    /** Called once per frame (end of GameRenderer.render). */
    public static void endFrame() {
        for (int i = 0; i < frameNanos.length; i++) {
            double ms = frameNanos[i] / 1_000_000.0;
            smoothedMs[i] += (ms - smoothedMs[i]) * SMOOTHING;
            frameNanos[i] = 0;
        }
    }

    public static double ms(Part part) {
        return smoothedMs[part.ordinal()];
    }

    public static String f3Line() {
        double total = 0;
        for (double v : smoothedMs) total += v;
        return String.format(Locale.ROOT, "Figura CPU: %.3f ms/frame (render events %.3f, model %.3f, world events %.3f, anims %.3f, tick %.3f)",
                total, ms(Part.RENDER_EVENTS), ms(Part.MODEL), ms(Part.WORLD_EVENTS), ms(Part.ANIMATIONS), ms(Part.TICK));
    }
}
