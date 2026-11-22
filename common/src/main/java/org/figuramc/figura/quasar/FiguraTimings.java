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

    // model builds per frame, split by why they happened
    public enum Build { WORLD_OTHERS, WORLD_SELF_THIRD_PERSON, WORLD_SELF_FIRST_PERSON, GUI }
    private static final int[] frameBuilds = new int[Build.values().length];
    private static final double[] smoothedBuilds = new double[Build.values().length];

    public static void countBuild(Build build) {
        frameBuilds[build.ordinal()]++;
    }
    private static final long[] frameNanos = new long[Part.values().length];
    private static final double[] smoothedMs = new double[Part.values().length];

    // Quasar MC: where the MODEL time goes (the rest is walking the part tree and its matrices)
    public enum ModelPart { PART_SCRIPTS, VERTICES, SUBMIT }
    private static final long[] frameModelNanos = new long[ModelPart.values().length];
    private static final double[] smoothedModelMs = new double[ModelPart.values().length];

    public static void stopModel(ModelPart part, long startNanos) {
        frameModelNanos[part.ordinal()] += System.nanoTime() - startNanos;
    }

    public static String f3ModelLine() {
        double model = ms(Part.MODEL);
        double scripts = smoothedModelMs[0], verts = smoothedModelMs[1], submit = smoothedModelMs[2];
        // part scripts run inside the tree walk, vertices too; so the walk is what's left over
        double walk = Math.max(0, model - scripts - verts - submit);
        return String.format(Locale.ROOT, "Figura model split: part scripts %.3f, vertices %.3f, submit %.3f, tree/matrices %.3f",
                scripts, verts, submit, walk);
    }

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
        for (int i = 0; i < frameModelNanos.length; i++) {
            double ms = frameModelNanos[i] / 1_000_000.0;
            smoothedModelMs[i] += (ms - smoothedModelMs[i]) * SMOOTHING;
            frameModelNanos[i] = 0;
        }
        for (int i = 0; i < frameBuilds.length; i++) {
            smoothedBuilds[i] += (frameBuilds[i] - smoothedBuilds[i]) * SMOOTHING;
            frameBuilds[i] = 0;
        }
    }

    public static double ms(Part part) {
        return smoothedMs[part.ordinal()];
    }

    public static String f3BuildsLine() {
        return String.format(Locale.ROOT, "Figura model builds/frame: others %.1f, self 3rd person %.1f, self 1st person %.1f, GUI %.1f",
                smoothedBuilds[0], smoothedBuilds[1], smoothedBuilds[2], smoothedBuilds[3]);
    }

    public static String f3Line() {
        double total = 0;
        for (double v : smoothedMs) total += v;
        return String.format(Locale.ROOT, "Figura CPU: %.3f ms/frame (render events %.3f, model %.3f, world events %.3f, anims %.3f, tick %.3f)",
                total, ms(Part.RENDER_EVENTS), ms(Part.MODEL), ms(Part.WORLD_EVENTS), ms(Part.ANIMATIONS), ms(Part.TICK));
    }
}
