package com.novusforge.astrum.world;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChunkMesh {
    public static final int VERTEX_SIZE = 8;

    private final List<float[]> opaquePositions = new ArrayList<>();
    private final List<int[]> opaqueIndices = new ArrayList<>();
    private final List<float[]> transparentPositions = new ArrayList<>();
    private final List<int[]> transparentIndices = new ArrayList<>();

    private int opaqueVertexCount = 0;
    private int opaqueIndexCount = 0;
    private int transparentVertexCount = 0;
    private int transparentIndexCount = 0;

    private long opaqueVboId = 0;
    private long opaqueIboId = 0;
    private long transparentVboId = 0;
    private long transparentIboId = 0;

    private static Consumer<Long> bufferDeleter;

    public static void setBufferDeleter(Consumer<Long> deleter) {
        bufferDeleter = deleter;
    }

    public void addOpaqueQuad(float[] vertices, int[] indices) {
        int baseVertex = opaqueVertexCount;
        for (int i : indices) {
            opaqueIndices.add(new int[]{baseVertex + i});
        }
        opaquePositions.add(vertices);
        opaqueVertexCount += 4;
        opaqueIndexCount += indices.length;
    }

    public void addTransparentQuad(float[] vertices, int[] indices) {
        int baseVertex = transparentVertexCount;
        for (int i : indices) {
            transparentIndices.add(new int[]{baseVertex + i});
        }
        transparentPositions.add(vertices);
        transparentVertexCount += 4;
        transparentIndexCount += indices.length;
    }

    public boolean hasOpaqueData() {
        return opaqueVertexCount > 0;
    }

    public boolean hasTransparentData() {
        return transparentVertexCount > 0;
    }

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

    public FloatBuffer buildTransparentVertexData() {
        float[] data = new float[transparentVertexCount * VERTEX_SIZE];
        int idx = 0;
        for (float[] verts : transparentPositions) {
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

    public IntBuffer buildTransparentIndexData() {
        int[] data = new int[transparentIndexCount];
        int idx = 0;
        for (int[] indices : transparentIndices) {
            data[idx++] = indices[0];
        }
        return ByteBuffer.allocateDirect(data.length * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
            .put(data)
            .flip();
    }

    public int getOpaqueVertexCount() { return opaqueVertexCount; }
    public int getOpaqueIndexCount() { return opaqueIndexCount; }
    public int getTransparentVertexCount() { return transparentVertexCount; }
    public int getTransparentIndexCount() { return transparentIndexCount; }

    public void setOpaqueVboId(long id) { this.opaqueVboId = id; }
    public void setOpaqueIboId(long id) { this.opaqueIboId = id; }
    public void setTransparentVboId(long id) { this.transparentVboId = id; }
    public void setTransparentIboId(long id) { this.transparentIboId = id; }
    public long getOpaqueVboId() { return opaqueVboId; }
    public long getOpaqueIboId() { return opaqueIboId; }
    public long getTransparentVboId() { return transparentVboId; }
    public long getTransparentIboId() { return transparentIboId; }

    public void dispose() {
        if (bufferDeleter != null) {
            if (opaqueVboId != 0) bufferDeleter.accept(opaqueVboId);
            if (opaqueIboId != 0) bufferDeleter.accept(opaqueIboId);
            if (transparentVboId != 0) bufferDeleter.accept(transparentVboId);
            if (transparentIboId != 0) bufferDeleter.accept(transparentIboId);
        }
        opaqueVboId = 0;
        opaqueIboId = 0;
        transparentVboId = 0;
        transparentIboId = 0;
        opaquePositions.clear();
        opaqueIndices.clear();
        transparentPositions.clear();
        transparentIndices.clear();
    }

    public void clearLists() {
        opaquePositions.clear();
        opaqueIndices.clear();
        transparentPositions.clear();
        transparentIndices.clear();
    }
}
