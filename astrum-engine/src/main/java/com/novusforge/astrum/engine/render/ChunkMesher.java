package com.novusforge.astrum.engine.render;

import com.novusforge.astrum.common.world.BlockID;
import com.novusforge.astrum.common.world.VoxelData;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * High-performance Chunk Mesher for Project Astrum.
 * Converts 1D VoxelData into optimized triangle meshes using Face Culling.
 */
public class ChunkMesher {
    public static class MeshData {
        public float[] vertices;
        public float[] colors;
    }

    public MeshData generateMesh(VoxelData data) {
        List<Float> vertices = new ArrayList<>();
        List<Float> colors = new ArrayList<>();

        for (int x = 0; x < VoxelData.CHUNK_SIZE; x++) {
            for (int y = 0; y < VoxelData.CHUNK_SIZE; y++) {
                for (int z = 0; z < VoxelData.CHUNK_SIZE; z++) {
                    byte block = data.getBlock(x, y, z);
                    if (block == BlockID.AIR) continue;

                    Vector3f color = getBlockColor(block);

                    // Check all 6 faces for neighbor AIR to perform face culling
                    if (isAir(data, x - 1, y, z)) addFace(vertices, colors, x, y, z, 0, color); // Left (-X)
                    if (isAir(data, x + 1, y, z)) addFace(vertices, colors, x, y, z, 1, color); // Right (+X)
                    if (isAir(data, x, y - 1, z)) addFace(vertices, colors, x, y, z, 2, color); // Bottom (-Y)
                    if (isAir(data, x, y + 1, z)) addFace(vertices, colors, x, y, z, 3, color); // Top (+Y)
                    if (isAir(data, x, y, z - 1)) addFace(vertices, colors, x, y, z, 4, color); // Back (-Z)
                    if (isAir(data, x, y, z + 1)) addFace(vertices, colors, x, y, z, 5, color); // Front (+Z)
                }
            }
        }

        MeshData mesh = new MeshData();
        mesh.vertices = toFloatArray(vertices);
        mesh.colors = toFloatArray(colors);
        return mesh;
    }

    private boolean isAir(VoxelData data, int x, int y, int z) {
        if (x < 0 || x >= VoxelData.CHUNK_SIZE || y < 0 || y >= VoxelData.CHUNK_SIZE || z < 0 || z >= VoxelData.CHUNK_SIZE) {
            return true;
        }
        return data.getBlock(x, y, z) == BlockID.AIR;
    }

    private Vector3f getBlockColor(byte block) {
        switch (block) {
            case BlockID.STONE: return new Vector3f(0.5f, 0.5f, 0.55f);
            case BlockID.GRASS: return new Vector3f(0.1f, 0.9f, 0.1f);
            case BlockID.DIRT:  return new Vector3f(0.4f, 0.3f, 0.2f);
            case BlockID.FERROUS: return new Vector3f(0.8f, 0.8f, 0.85f);
            case BlockID.AURUM: return new Vector3f(1.0f, 0.9f, 0.0f);
            case BlockID.GREENSTONE: return new Vector3f(0.0f, 1.0f, 0.4f);
            case BlockID.INFERNITE: return new Vector3f(0.3f, 0.05f, 0.05f);
            default: return new Vector3f(1.0f, 0.0f, 1.0f); // Error Magenta
        }
    }

    private void addFace(List<Float> vertices, List<Float> colors, int x, int y, int z, int face, Vector3f color) {
        // Voxel corners relative to (x, y, z)
        float x0 = x, x1 = x + 1;
        float y0 = y, y1 = y + 1;
        float z0 = z, z1 = z + 1;

        float[][] faceVertices = switch (face) {
            case 0 -> new float[][]{ {x0, y1, z1}, {x0, y1, z0}, {x0, y0, z0}, {x0, y0, z1} }; // Left
            case 1 -> new float[][]{ {x1, y1, z0}, {x1, y1, z1}, {x1, y0, z1}, {x1, y0, z0} }; // Right
            case 2 -> new float[][]{ {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1} }; // Bottom
            case 3 -> new float[][]{ {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}, {x0, y1, z0} }; // Top
            case 4 -> new float[][]{ {x0, y1, z0}, {x1, y1, z0}, {x1, y0, z0}, {x0, y0, z0} }; // Back
            case 5 -> new float[][]{ {x1, y1, z1}, {x0, y1, z1}, {x0, y0, z1}, {x1, y0, z1} }; // Front
            default -> new float[0][0];
        };

        // Add two triangles per face (Tri 1: 0,1,2 | Tri 2: 2,3,0)
        int[] indices = {0, 1, 2, 2, 3, 0};
        for (int index : indices) {
            vertices.add(faceVertices[index][0]);
            vertices.add(faceVertices[index][1]);
            vertices.add(faceVertices[index][2]);
            colors.add(color.x);
            colors.add(color.y);
            colors.add(color.z);
        }
    }

    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) array[i] = list.get(i);
        return array;
    }
}
