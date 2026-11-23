package org.figuramc.figura.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import org.figuramc.figura.math.matrix.FiguraMat4;
import org.figuramc.figura.model.rendering.Vertex;
import org.figuramc.figura.parsers.BlockbenchCommonTypes;

import java.util.*;

/**
 * Quasar MC: skinning for meshes inside a Blockbench 5 armature.
 * <p>
 * Bones are regular model parts (groups), so animations, scripts and vanilla parent types move them as usual.
 * Every frame, each bone's skin matrix is its current transform (relative to the armature) times the inverse of its
 * rest transform; a vertex is moved by the weighted blend of the skin matrices of the bones that influence it.
 * This is linear blend skinning, the same as Blockbench's own preview, so meshes bend in-game like in the editor.
 */
public class SkinnedMesh {
    private static final int N = BlockbenchCommonTypes.SKIN_INFLUENCES;

    /// bone index per influence, {@link #N} per source vertex, -1 = unused
    private final int[] boneIdx;
    /// weight per influence, {@link #N} per source vertex
    private final float[] weights;
    private final int sourceCount;

    // bound by bindArmature
    private FiguraModelPart[] bones = new FiguraModelPart[0];
    private FiguraModelPart[][] chains = new FiguraModelPart[0][];
    private FiguraMat4[] restInverse = new FiguraMat4[0];
    private FiguraMat4[] skin = new FiguraMat4[0];

    // per-frame scratch
    private final FiguraMat4 chain = FiguraMat4.of();
    /// blended 3x4 matrix per source vertex (row major), valid where hasBlend is true
    private final double[] blended;
    private final boolean[] hasBlend;
    private final Map<Integer, List<Vertex>> output = new HashMap<>();

    private SkinnedMesh(int[] boneIdx, float[] weights) {
        this.boneIdx = boneIdx;
        this.weights = weights;
        this.sourceCount = boneIdx.length / N;
        this.blended = new double[sourceCount * 12];
        this.hasBlend = new boolean[sourceCount];
    }

    /// @return skin data for a mesh part, or null if the mesh isn't skinned
    public static SkinnedMesh read(CompoundTag meshData) {
        if (!(meshData.get("ski") instanceof IntArrayTag ski)) return null;
        int[] idx = ski.getAsIntArray();
        ListTag skw = meshData.getListOrEmpty("skw");
        if (idx.length == 0 || idx.length % N != 0 || skw.size() != idx.length) return null;
        float[] w = new float[idx.length];
        for (int i = 0; i < w.length; i++)
            w[i] = skw.getFloatOr(i, 0f);
        return new SkinnedMesh(idx, w);
    }

    /**
     * Called once an armature part and all its children are loaded: links skinned meshes to the bones
     * and stores the bones' rest transforms. Also moves the bones in front of the meshes, so bone transforms
     * are already up to date for the frame when the meshes render.
     */
    public static void bindArmature(FiguraModelPart armature) {
        Map<Integer, FiguraModelPart> boneMap = new HashMap<>();
        List<FiguraModelPart> meshes = new ArrayList<>();
        collect(armature, boneMap, meshes);
        if (meshes.isEmpty()) return;

        int count = 0;
        for (int i : boneMap.keySet()) count = Math.max(count, i + 1);

        FiguraModelPart[] bones = new FiguraModelPart[count];
        FiguraModelPart[][] chains = new FiguraModelPart[count][];
        FiguraMat4[] restInverse = new FiguraMat4[count];
        for (Map.Entry<Integer, FiguraModelPart> e : boneMap.entrySet()) {
            int i = e.getKey();
            FiguraModelPart bone = e.getValue();
            bones[i] = bone;
            chains[i] = chainFrom(armature, bone);

            FiguraMat4 rest = FiguraMat4.of();
            for (FiguraModelPart p : chains[i])
                rest.rightMultiply(restLocal(p));
            restInverse[i] = rest.inverted();
        }

        for (FiguraModelPart mesh : meshes) {
            SkinnedMesh s = mesh.skin;
            s.bones = bones;
            s.chains = chains;
            s.restInverse = restInverse;
            s.skin = new FiguraMat4[count];
            for (int i = 0; i < count; i++) s.skin[i] = FiguraMat4.of();
        }

        // bones (and their subtrees) first, so they are rendered (and their matrices recalculated) before the meshes
        armature.children.sort(Comparator.comparingInt(c -> containsBone(c) ? 0 : 1));
    }

    private static void collect(FiguraModelPart part, Map<Integer, FiguraModelPart> bones, List<FiguraModelPart> meshes) {
        for (FiguraModelPart child : part.children) {
            if (child.boneIndex >= 0) bones.put(child.boneIndex, child);
            if (child.skin != null) meshes.add(child);
            collect(child, bones, meshes);
        }
    }

    private static boolean containsBone(FiguraModelPart part) {
        if (part.boneIndex >= 0) return true;
        for (FiguraModelPart child : part.children)
            if (containsBone(child)) return true;
        return false;
    }

    /// parts from just below the armature down to (and including) the bone
    private static FiguraModelPart[] chainFrom(FiguraModelPart armature, FiguraModelPart bone) {
        ArrayDeque<FiguraModelPart> chain = new ArrayDeque<>();
        for (FiguraModelPart p = bone; p != null && p != armature; p = p.parent)
            chain.addFirst(p);
        return chain.toArray(new FiguraModelPart[0]);
    }

    /// a part's local transform in its rest pose (only its pivot and load-time rotation)
    private static FiguraMat4 restLocal(FiguraModelPart part) {
        PartCustomization rest = new PartCustomization();
        rest.partType = part.customization.partType;
        rest.setPivot(part.customization.getPivot());
        rest.setRot(part.customization.getRot());
        rest.recalculate();
        return rest.positionMatrix.copy();
    }

    /**
     * @param source the mesh's own (rest pose) vertices, by texture
     * @return vertices moved by the bones; the returned lists are reused every frame
     */
    public Map<Integer, List<Vertex>> deform(Map<Integer, List<Vertex>> source) {
        // 1. skin matrix of every bone for this frame
        for (int b = 0; b < bones.length; b++) {
            if (bones[b] == null) continue;
            chain.reset();
            for (FiguraModelPart p : chains[b])
                chain.rightMultiply(p.customization.positionMatrix);
            skin[b].set(chain);
            skin[b].rightMultiply(restInverse[b]);
        }

        // 2. blend the skin matrices per source vertex
        for (int v = 0; v < sourceCount; v++) {
            int o = v * 12;
            double total = 0;
            for (int k = 0; k < 12; k++) blended[o + k] = 0;
            for (int k = 0; k < N; k++) {
                int b = boneIdx[v * N + k];
                float w = weights[v * N + k];
                if (b < 0 || b >= bones.length || bones[b] == null || w <= 0f) continue;
                FiguraMat4 m = skin[b];
                blended[o] += w * m.v11; blended[o + 1] += w * m.v12; blended[o + 2] += w * m.v13; blended[o + 3] += w * m.v14;
                blended[o + 4] += w * m.v21; blended[o + 5] += w * m.v22; blended[o + 6] += w * m.v23; blended[o + 7] += w * m.v24;
                blended[o + 8] += w * m.v31; blended[o + 9] += w * m.v32; blended[o + 10] += w * m.v33; blended[o + 11] += w * m.v34;
                total += w;
            }
            hasBlend[v] = total > 0;
            // renormalize if some influencing bone was not exported
            if (hasBlend[v] && Math.abs(total - 1) > 1e-6)
                for (int k = 0; k < 12; k++) blended[o + k] /= total;
        }

        // 3. move the vertices
        for (Map.Entry<Integer, List<Vertex>> entry : source.entrySet()) {
            List<Vertex> src = entry.getValue();
            List<Vertex> dst = output.get(entry.getKey());
            if (dst == null || dst.size() != src.size()) {
                dst = new ArrayList<>(src.size());
                for (Vertex vertex : src) dst.add(vertex.copy());
                output.put(entry.getKey(), dst);
            }
            for (int i = 0; i < src.size(); i++) {
                Vertex s = src.get(i), d = dst.get(i);
                d.u = s.u; d.v = s.v;
                int v = s.sourceIndex;
                if (v < 0 || v >= sourceCount || !hasBlend[v]) {
                    d.x = s.x; d.y = s.y; d.z = s.z;
                    d.nx = s.nx; d.ny = s.ny; d.nz = s.nz;
                    continue;
                }
                int o = v * 12;
                double x = s.x, y = s.y, z = s.z;
                d.x = (float) (blended[o] * x + blended[o + 1] * y + blended[o + 2] * z + blended[o + 3]);
                d.y = (float) (blended[o + 4] * x + blended[o + 5] * y + blended[o + 6] * z + blended[o + 7]);
                d.z = (float) (blended[o + 8] * x + blended[o + 9] * y + blended[o + 10] * z + blended[o + 11]);

                double nx = s.nx, ny = s.ny, nz = s.nz;
                double tx = blended[o] * nx + blended[o + 1] * ny + blended[o + 2] * nz;
                double ty = blended[o + 4] * nx + blended[o + 5] * ny + blended[o + 6] * nz;
                double tz = blended[o + 8] * nx + blended[o + 9] * ny + blended[o + 10] * nz;
                double len = Math.sqrt(tx * tx + ty * ty + tz * tz);
                if (len > 1e-9) {
                    d.nx = (float) (tx / len); d.ny = (float) (ty / len); d.nz = (float) (tz / len);
                } else {
                    d.nx = s.nx; d.ny = s.ny; d.nz = s.nz;
                }
            }
        }
        // drop lists for textures the mesh no longer has
        if (output.size() != source.size())
            output.keySet().retainAll(source.keySet());
        return output;
    }
}
