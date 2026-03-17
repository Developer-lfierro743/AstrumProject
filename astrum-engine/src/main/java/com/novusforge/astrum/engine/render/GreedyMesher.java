package com.novusforge.astrum.engine.render;

import com.novusforge.astrum.common.world.BlockID;
import com.novusforge.astrum.common.world.VoxelData;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Greedy Meshing optimization for Project Astrum.
 * Combines adjacent faces of the same block type into larger quads,
 * significantly reducing triangle count and improving GPU performance.
 */
public class GreedyMesher {
    
    public static class GreedyMeshData {
        public float[] vertices;
        public float[] colors;
        public int triangles;
        
        public GreedyMeshData(float[] vertices, float[] colors, int triangles) {
            this.vertices = vertices;
            this.colors = colors;
            this.triangles = triangles;
        }
    }
    
    private static final int[][][] FACES = {
        {{0, 1}, {2, 3}},  // Face 0: Left (-X)
        {{0, 1}, {2, 3}},  // Face 1: Right (+X)
        {{0, 1}, {2, 3}},  // Face 2: Bottom (-Y)
        {{0, 1}, {2, 3}},  // Face 3: Top (+Y)
        {{0, 1}, {2, 3}},  // Face 4: Back (-Z)
        {{0, 1}, {2, 3}}   // Face 5: Front (+Z)
    };
    
    public GreedyMeshData generateGreedyMesh(VoxelData data) {
        List<Float> vertices = new ArrayList<>();
        List<Float> colors = new ArrayList<>();
        
        int width = VoxelData.CHUNK_SIZE;
        int height = VoxelData.CHUNK_SIZE;
        int depth = VoxelData.CHUNK_SIZE;
        
        for (int face = 0; face < 6; face++) {
            int d, w, h;
            int[] mask = new int[width * height];
            
            switch (face) {
                case 0, 1 -> { // Left/Right
                    d = 0;
                    w = 2;
                    h = 1;
                }
                case 2, 3 -> { // Bottom/Top
                    d = 1;
                    w = 0;
                    h = 2;
                }
                default -> { // Back/Front
                    d = 2;
                    w = 0;
                    h = 1;
                }
            }
            
            int[] dims = {width, height, depth};
            int u = (face == 0 || face == 1 || face == 4 || face == 5) ? 0 : 
                     (face == 2 || face == 3) ? 0 : 1;
            int v = (face == 2 || face == 3) ? 0 : 
                     (face == 4 || face == 5) ? 1 : 2;
            
            int[] q = new int[3];
            int[] x = {0, 0, 0};
            int[] maskPos = new int[3];
            
            for (int slice = 0; slice < dims[d]; slice++) {
                int n = 0;
                for (int j = 0; j < height; j++) {
                    for (int i = 0; i < width; i++) {
                        maskPos[d] = slice;
                        maskPos[v] = j;
                        maskPos[u] = i;
                        
                        byte block = getBlock(data, maskPos[0], maskPos[1], maskPos[2]);
                        byte neighbor = getNeighborBlock(data, maskPos[0], maskPos[1], maskPos[2], face);
                        
                        boolean blockExists = block != BlockID.AIR;
                        boolean neighborExists = neighbor != BlockID.AIR;
                        
                        if (blockExists && !neighborExists) {
                            mask[n] = block & 0xFF;
                        } else {
                            mask[n] = 0;
                        }
                        n++;
                    }
                }
                
                n = 0;
                for (int j = 0; j < height; j++) {
                    for (int i = 0; i < width;) {
                        byte c = (byte) mask[n];
                        
                        if (c == 0) {
                            i++;
                            n++;
                            continue;
                        }
                        
                        int quadWidth = 1;
                        while (i + quadWidth < width && mask[n + quadWidth] == c) {
                            quadWidth++;
                        }
                        
                        int quadHeight = 1;
                        boolean done = false;
                        while (j + quadHeight < height) {
                            for (int k = 0; k < quadWidth; k++) {
                                if (mask[n + k + (width * quadHeight)] != c) {
                                    done = true;
                                    break;
                                }
                            }
                            if (done) break;
                            quadHeight++;
                        }
                        
                        x[d] = slice;
                        x[v] = j;
                        x[u] = i;
                        
                        q[d] = slice + (face == 0 || face == 2 || face == 4 ? 0 : 1);
                        q[v] = j + quadHeight;
                        q[u] = i + quadWidth;
                        
                        byte block = (byte) c;
                        Vector3f color = getBlockColor(block);
                        addQuad(vertices, colors, face, x, q, color);
                        
                        for (int l = 0; l < quadHeight; l++) {
                            for (int k = 0; k < quadWidth; k++) {
                                mask[n + k + (width * l)] = 0;
                            }
                        }
                        
                        i += w;
                        n += w;
                    }
                }
            }
        }
        
        float[] verts = toFloatArray(vertices);
        float[] cols = toFloatArray(colors);
        int triangles = verts.length / 9;
        
        return new GreedyMeshData(verts, cols, triangles);
    }
    
    private byte getBlock(VoxelData data, int x, int y, int z) {
        if (x < 0 || x >= VoxelData.CHUNK_SIZE || 
            y < 0 || y >= VoxelData.CHUNK_SIZE || 
            z < 0 || z >= VoxelData.CHUNK_SIZE) {
            return BlockID.AIR;
        }
        return data.getBlock(x, y, z);
    }
    
    private byte getNeighborBlock(VoxelData data, int x, int y, int z, int face) {
        int nx = x, ny = y, nz = z;
        switch (face) {
            case 0 -> nx--; // Left
            case 1 -> nx++; // Right
            case 2 -> ny--; // Bottom
            case 3 -> ny++; // Top
            case 4 -> nz--; // Back
            case 5 -> nz++; // Front
        }
        return getBlock(data, nx, ny, nz);
    }
    
    private void addQuad(List<Float> vertices, List<Float> colors, int face, int[] x, int[] q, Vector3f color) {
        float[][] quadVerts = getQuadVertices(face, x, q);
        
        vertices.add(quadVerts[0][0]); vertices.add(quadVerts[0][1]); vertices.add(quadVerts[0][2]);
        colors.add(color.x); colors.add(color.y); colors.add(color.z);
        
        vertices.add(quadVerts[1][0]); vertices.add(quadVerts[1][1]); vertices.add(quadVerts[1][2]);
        colors.add(color.x); colors.add(color.y); colors.add(color.z);
        
        vertices.add(quadVerts[2][0]); vertices.add(quadVerts[2][1]); vertices.add(quadVerts[2][2]);
        colors.add(color.x); colors.add(color.y); colors.add(color.z);
        
        vertices.add(quadVerts[2][0]); vertices.add(quadVerts[2][1]); vertices.add(quadVerts[2][2]);
        colors.add(color.x); colors.add(color.y); colors.add(color.z);
        
        vertices.add(quadVerts[3][0]); vertices.add(quadVerts[3][1]); vertices.add(quadVerts[3][2]);
        colors.add(color.x); colors.add(color.y); colors.add(color.z);
        
        vertices.add(quadVerts[0][0]); vertices.add(quadVerts[0][1]); vertices.add(quadVerts[0][2]);
        colors.add(color.x); colors.add(color.y); colors.add(color.z);
    }
    
    private float[][] getQuadVertices(int face, int[] x, int[] q) {
        float[][] verts = new float[4][3];
        
        switch (face) {
            case 0 -> { // Left (-X)
                verts[0] = new float[]{x[0], x[2], q[2]};
                verts[1] = new float[]{x[0], x[2], x[2]};
                verts[2] = new float[]{x[0], q[2], x[2]};
                verts[3] = new float[]{x[0], q[2], q[2]};
            }
            case 1 -> { // Right (+X)
                verts[0] = new float[]{q[0], x[2], x[2]};
                verts[1] = new float[]{q[0], x[2], q[2]};
                verts[2] = new float[]{q[0], q[2], q[2]};
                verts[3] = new float[]{q[0], q[2], x[2]};
            }
            case 2 -> { // Bottom (-Y)
                verts[0] = new float[]{x[0], x[1], x[2]};
                verts[1] = new float[]{q[0], x[1], x[2]};
                verts[2] = new float[]{q[0], x[1], q[2]};
                verts[3] = new float[]{x[0], x[1], q[2]};
            }
            case 3 -> { // Top (+Y)
                verts[0] = new float[]{x[0], q[1], q[2]};
                verts[1] = new float[]{q[0], q[1], q[2]};
                verts[2] = new float[]{q[0], q[1], x[2]};
                verts[3] = new float[]{x[0], q[1], x[2]};
            }
            case 4 -> { // Back (-Z)
                verts[0] = new float[]{x[0], q[1], x[2]};
                verts[1] = new float[]{x[0], q[1], q[2]};
                verts[2] = new float[]{x[0], x[1], q[2]};
                verts[3] = new float[]{x[0], x[1], x[2]};
            }
            case 5 -> { // Front (+Z)
                verts[0] = new float[]{q[0], q[1], q[2]};
                verts[1] = new float[]{x[0], q[1], q[2]};
                verts[2] = new float[]{x[0], x[1], q[2]};
                verts[3] = new float[]{q[0], x[1], q[2]};
            }
        }
        
        return verts;
    }
    
    private Vector3f getBlockColor(byte block) {
        switch (block) {
            case BlockID.STONE: return new Vector3f(0.5f, 0.5f, 0.55f);
            case BlockID.GRASS: return new Vector3f(0.1f, 0.9f, 0.1f);
            case BlockID.DIRT: return new Vector3f(0.4f, 0.3f, 0.2f);
            case BlockID.FERROUS: return new Vector3f(0.8f, 0.8f, 0.85f);
            case BlockID.AURUM: return new Vector3f(1.0f, 0.9f, 0.0f);
            case BlockID.GREENSTONE: return new Vector3f(0.0f, 1.0f, 0.4f);
            case BlockID.INFERNITE: return new Vector3f(0.3f, 0.05f, 0.05f);
            default: return new Vector3f(1.0f, 0.0f, 1.0f);
        }
    }
    
    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) array[i] = list.get(i);
        return array;
    }
}
