package org.figuramc.figura.utils;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import org.joml.Matrix4f;

/**
 * Quasar MC (26.3): stand-in for 1.21.x's removed CachedOrthoProjectionMatrixBuffer.
 * Builds an orthographic projection for the given size and uploads it through ProjectionMatrixBuffer.
 */
public class QuasarOrthoProjection implements AutoCloseable {
    private final ProjectionMatrixBuffer buffer;
    private final float zNear;
    private final float zFar;
    private final boolean invertY;
    private final Matrix4f matrix = new Matrix4f();

    public QuasarOrthoProjection(String name, float zNear, float zFar, boolean invertY) {
        this.buffer = new ProjectionMatrixBuffer(name);
        this.zNear = zNear;
        this.zFar = zFar;
        this.invertY = invertY;
    }

    public GpuBufferSlice getBuffer(float width, float height) {
        if (invertY)
            matrix.setOrtho(0.0F, width, height, 0.0F, zNear, zFar);
        else
            matrix.setOrtho(0.0F, width, 0.0F, height, zNear, zFar);
        return buffer.getBuffer(matrix);
    }

    @Override
    public void close() {
        buffer.close();
    }
}
