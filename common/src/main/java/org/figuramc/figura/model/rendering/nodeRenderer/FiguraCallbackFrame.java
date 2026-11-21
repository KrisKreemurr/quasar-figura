package org.figuramc.figura.model.rendering.nodeRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import org.figuramc.figura.ducks.FiguraSubmitCallBackExtension;
import org.figuramc.figura.utils.FiguraBufferSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Quasar MC (26.3): per-frame bookkeeping for Figura's "before/after a vanilla model draws" callbacks.
 *
 * In 26.3 models are submitted first and drawn later in batches, and one Model object is shared by every
 * entity using it. So callbacks are added to the model (pending), bound to the submitted render state when
 * the model is submitted, and taken + run when that exact submission is drawn. Anything never drawn
 * (culled, cancelled) is dropped at the end of the frame so callbacks can't pile up.
 */
public final class FiguraCallbackFrame {
    private FiguraCallbackFrame() {}

    public record Bucket(List<BiFunction<FiguraBufferSource, PoseStack, Boolean>> pre, List<Runnable> post) {
        public static Bucket empty() { return new Bucket(new ArrayList<>(), new ArrayList<>()); }
        public boolean isEmpty() { return pre.isEmpty() && post.isEmpty(); }
    }

    private static final Set<FiguraSubmitCallBackExtension> TOUCHED = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<Object, Map<Object, Bucket>> BOUND = new IdentityHashMap<>();
    private static final Object NULL_STATE = new Object();

    /** Remember that this holder has pending callbacks this frame. */
    public static void touch(FiguraSubmitCallBackExtension holder) {
        TOUCHED.add(holder);
    }

    /** At submit time: move the model's (and its root part's) pending callbacks onto this submission. */
    public static void bind(Object model, FiguraSubmitCallBackExtension... holders) {
        bindWithState(model, null, holders);
    }

    public static void bindWithState(Object model, Object state, FiguraSubmitCallBackExtension... holders) {
        Bucket bucket = null;
        for (FiguraSubmitCallBackExtension holder : holders) {
            if (holder == null) continue;
            var pre = holder.figura$getPreRenderingCallbacks();
            var post = holder.figura$getPostRenderingCallbacks();
            if (pre.isEmpty() && post.isEmpty()) continue;
            if (bucket == null) bucket = Bucket.empty();
            bucket.pre().addAll(pre);
            bucket.post().addAll(post);
            pre.clear();
            post.clear();
        }
        if (bucket == null) return;
        Map<Object, Bucket> perModel = BOUND.computeIfAbsent(model, k -> new IdentityHashMap<>());
        Object key = state == null ? NULL_STATE : state;
        Bucket existing = perModel.get(key);
        if (existing == null) perModel.put(key, bucket);
        else { existing.pre().addAll(bucket.pre()); existing.post().addAll(bucket.post()); }
    }

    /** At draw time: take this submission's callbacks (null if none). */
    public static Bucket take(Object model, Object state) {
        Map<Object, Bucket> perModel = BOUND.get(model);
        if (perModel == null) return null;
        Bucket bucket = perModel.remove(state == null ? NULL_STATE : state);
        if (perModel.isEmpty()) BOUND.remove(model);
        return bucket;
    }

    /** End of frame: drop everything that was never drawn. Post callbacks still run so poses get restored. */
    public static void endFrame() {
        for (Map<Object, Bucket> perModel : BOUND.values())
            for (Bucket bucket : perModel.values())
                for (Runnable r : bucket.post()) r.run();
        BOUND.clear();
        for (FiguraSubmitCallBackExtension holder : TOUCHED) {
            holder.figura$getPreRenderingCallbacks().clear();
            for (Runnable r : holder.figura$getPostRenderingCallbacks()) r.run();
            holder.figura$getPostRenderingCallbacks().clear();
        }
        TOUCHED.clear();
    }
}
