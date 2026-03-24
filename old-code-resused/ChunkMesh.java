package com.novusforge.astrum.world;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * ChunkMesh - GPU-ready mesh data for a chunk
 * Formula: "separating Chunk into different Meshes for opaque and transparent blocks"
 */
public class ChunkMesh {
    
    // Vertex format: position (x,y,z) + color (r,g,b) = 6 floats = 24 bytes
    public static final int VERTEX_SIZE = 6;
    
    // Opaque mesh data
    private final List<float[]> opaquePositions = new ArrayList<>();
    private final List<int[]> opaqueIndices = new ArrayList<>();
    private int opaqueVertexCount = 0;
    private int opaqueIndexCount = 0;
    
    // Transparent mesh data (for water, glass, etc.)
    private final List<float[]> transparentPositions = new ArrayList<>();
    private final List<int[]> transparentIndices = new ArrayList<>();
    private int transparentVertexCount = 0;
    private int transparentIndexCount = 0;
    
    // GPU buffer IDs (Vulkan)
    private long opaqueVboId = 0;
    private long opaqueIboId = 0;
    private long opaqueVboMemId = 0;
    private long opaqueIboMemId = 0;
    
    // Callback for buffer deletion
    private static BiConsumer<Long, Long> bufferDeleter;

    public static void setBufferDeleter(BiConsumer<Long, Long> deleter) {
        bufferDeleter = deleter;
    }

    /**
     * Add opaque quad (4 vertices, 2 triangles = 6 indices)
     */
    public void addOpaqueQuad(float[] vertices, int[] indices) {
        int baseVertex = opaqueVertexCount;
        for (int i : indices) {
            opaqueIndices.add(new int[]{baseVertex + i});
        }
        opaquePositions.add(vertices);
        opaqueVertexCount += 4;
        opaqueIndexCount += indices.length;
    }

    /**
     * Add transparent quad
     */
    public void addTransparentQuad(float[] vertices, int[] indices) {
        int baseVertex = transparentVertexCount;
        for (int i : indices) {
            transparentIndices.add(new int[]{baseVertex + i});
        }
        transparentPositions.add(vertices);
        transparentVertexCount += 4;
        transparentIndexCount += indices.length;
    }

    public boolean hasOpaqueData() { return opaqueVertexCount > 0; }
    public boolean hasTransparentData() { return transparentVertexCount > 0; }
    public int getOpaqueVertexCount() { return opaqueVertexCount; }
    public int getOpaqueIndexCount() { return opaqueIndexCount; }

    /**
     * Build vertex buffer for GPU upload
     */
    public FloatBuffer buildOpaqueVertexData() {
        float[] data = new float[opaqueVertexCount * VERTEX_SIZE];
        int idx = 0;
        for (float[] verts : opaquePositions) {
            for (float v : verts) {
                data[idx++] = v;
            }
        }
        return ByteBuffer.allocateDirect(data.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(data)
            .flip();
    }

    /**
     * Build index buffer for GPU upload
     */
    public IntBuffer buildOpaqueIndexData() {
        int[] data = new int[opaqueIndexCount];
        int idx = 0;
        for (int[] indices : opaqueIndices) {
            data[idx++] = indices[0];
        }
        return ByteBuffer.allocateDirect(data.length * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
            .put(data)
            .flip();
    }

    // Setters for GPU buffer IDs
    public void setOpaqueVboId(long id) { this.opaqueVboId = id; }
    public void setOpaqueIboId(long id) { this.opaqueIboId = id; }
    public void setOpaqueVboMemId(long id) { this.opaqueVboMemId = id; }
    public void setOpaqueIboMemId(long id) { this.opaqueIboMemId = id; }
    
    public long getOpaqueVboId() { return opaqueVboId; }
    public long getOpaqueIboId() { return opaqueIboId; }

    /**
     * Cleanup GPU resources
     */
    public void dispose() {
        if (bufferDeleter != null) {
            if (opaqueVboId != 0) bufferDeleter.accept(opaqueVboId, opaqueVboMemId);
            if (opaqueIboId != 0) bufferDeleter.accept(opaqueIboId, opaqueIboMemId);
        }
        opaqueVboId = 0;
        opaqueIboId = 0;
        opaqueVboMemId = 0;
        opaqueIboMemId = 0;
        opaquePositions.clear();
        opaqueIndices.clear();
    }
}
