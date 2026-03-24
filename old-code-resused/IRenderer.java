package com.novusforge.astrum.engine;

import org.joml.Matrix4f;
import java.util.Map;
import com.novusforge.astrum.world.ChunkMesh;

/**
 * Renderer Interface - Common API for Vulkan and OpenGL
 * Following Formula: "universal unification"
 */
public interface IRenderer {
    boolean init();
    void render(Matrix4f view, Matrix4f projection, Map<Long, ChunkMesh> meshes);
    boolean windowShouldClose();
    long getWindow();
    float getAspectRatio();
    void setRenderTestCube(boolean render);
    void cleanup();
    void deleteBuffer(long bufferId, long memoryId);
    String getRendererName();
}
