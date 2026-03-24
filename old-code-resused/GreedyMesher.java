package com.novusforge.astrum.world;

/**
 * GreedyMesher - Optimized mesh generation
 * Formula: "Make a bitmap of the adjacent block surrounding it(and it saves gpus power)"
 */
public class GreedyMesher {

    // Face directions
    private static final int[][][] FACE_OFFSETS = {
        {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}},
        {{0, 0, 1}, {0, 0, -1}, {0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0}}
    };

    // Quad indices (2 triangles)
    private static final int[][] QUAD_INDICES = {{0, 1, 2, 0, 2, 3}};

    // Block colors (RGB)
    private static final float[] BLOCK_COLORS = {
        0.0f, 0.0f, 0.0f,      // 0: Air
        0.6f, 0.6f, 0.6f,      // 1: Stone (gray)
        0.55f, 0.35f, 0.2f,    // 2: Dirt (brown)
        0.2f, 0.6f, 0.2f,      // 3: Grass (green)
        0.3f, 0.5f, 0.8f,      // 4: Water (blue)
        0.5f, 0.3f, 0.1f       // 5: Wood (dark brown)
    };

    /**
     * Generate mesh for chunk using greedy meshing
     */
    public static ChunkMesh generateMesh(Chunk chunk, ChunkManager manager, int cx, int cz) {
        ChunkMesh mesh = new ChunkMesh();
        int baseX = cx * Chunk.SIZE;
        int baseZ = cz * Chunk.SIZE;

        // Generate faces for all 6 directions
        for (int face = 0; face < 6; face++) {
            int[] off = FACE_OFFSETS[0][face];

            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int x = 0; x < Chunk.SIZE; x++) {
                    for (int z = 0; z < Chunk.SIZE; z++) {
                        short current = chunk.getBlock(x, y, z);
                        if (current == 0) continue;

                        // Check if block behind is same (skip if so)
                        int px = x - off[0];
                        int py = y - off[1];
                        int pz = z - off[2];
                        if (px >= 0 && px < Chunk.SIZE && 
                            py >= 0 && py < Chunk.SIZE && 
                            pz >= 0 && pz < Chunk.SIZE) {
                            if (chunk.getBlock(px, py, pz) == current) continue;
                        }

                        // Find width of mergeable area
                        int w = 1;
                        while (x + w < Chunk.SIZE &&
                               chunk.getBlock(x + w, y, z) == current &&
                               shouldMerge(chunk, x + w, y, z, x + w - 1, y, z, face, manager)) {
                            w++;
                        }

                        // Find height of mergeable area
                        int h = 1;
                        boolean canExtend = true;
                        while (y + h < Chunk.SIZE && canExtend) {
                            for (int i = 0; i < w; i++) {
                                short block = chunk.getBlock(x + i, y + h, z);
                                if (block != current ||
                                    !shouldMerge(chunk, x + i, y + h, z, x + i, y + h - 1, z, face, manager)) {
                                    canExtend = false;
                                    break;
                                }
                            }
                            if (canExtend) h++;
                        }

                        // Add face to mesh
                        addFaceToMesh(mesh, x + baseX, y, z + baseZ, w, h, face, current);

                        x += w - 1; // Skip merged blocks
                        break;
                    }
                }
            }
        }

        return mesh;
    }

    private static boolean shouldMerge(Chunk chunk, int x, int y, int z, int nx, int ny, int nz, int face, ChunkManager manager) {
        if (x < 0 || x >= Chunk.SIZE || y < 0 || y >= Chunk.SIZE || z < 0 || z >= Chunk.SIZE) {
            short neighbor = chunk.getBlock(x, y, z);
            return neighbor != 0;
        }
        short block = chunk.getBlock(x, y, z);
        return block != 0;
    }

    private static void addFaceToMesh(ChunkMesh mesh, int x, int y, int z, int w, int h, int face, short blockType) {
        float[] color = getBlockColor(blockType);
        float[] vertices = new float[24]; // 4 vertices × 6 floats

        // Generate quad vertices based on face direction
        switch (face) {
            case 0: // +X
                for (int i = 0; i < 4; i++) {
                    int vi = i * 6;
                    vertices[vi] = x + (i < 2 ? w : 0);
                    vertices[vi + 1] = y + (i % 2 == 0 ? 0 : h);
                    vertices[vi + 2] = z + 1;
                    vertices[vi + 3] = color[0];
                    vertices[vi + 4] = color[1];
                    vertices[vi + 5] = color[2];
                }
                break;
            case 1: // -X
                for (int i = 0; i < 4; i++) {
                    int vi = i * 6;
                    vertices[vi] = x + (i < 2 ? 0 : -w);
                    vertices[vi + 1] = y + (i % 2 == 0 ? 0 : h);
                    vertices[vi + 2] = z - 1;
                    vertices[vi + 3] = color[0] * 0.8f;
                    vertices[vi + 4] = color[1] * 0.8f;
                    vertices[vi + 5] = color[2] * 0.8f;
                }
                break;
            case 2: // +Y (top)
                for (int i = 0; i < 4; i++) {
                    int vi = i * 6;
                    vertices[vi] = x + (i < 2 ? 0 : w);
                    vertices[vi + 1] = y + h;
                    vertices[vi + 2] = z + (i % 2 == 0 ? 0 : h);
                    vertices[vi + 3] = color[0] * 1.1f;
                    vertices[vi + 4] = color[1] * 1.1f;
                    vertices[vi + 5] = color[2] * 1.1f;
                }
                break;
            case 3: // -Y (bottom)
                for (int i = 0; i < 4; i++) {
                    int vi = i * 6;
                    vertices[vi] = x + (i < 2 ? 0 : w);
                    vertices[vi + 1] = y - 1;
                    vertices[vi + 2] = z + (i % 2 == 0 ? h : 0);
                    vertices[vi + 3] = color[0] * 0.6f;
                    vertices[vi + 4] = color[1] * 0.6f;
                    vertices[vi + 5] = color[2] * 0.6f;
                }
                break;
            case 4: // +Z
                for (int i = 0; i < 4; i++) {
                    int vi = i * 6;
                    vertices[vi] = x + (i < 2 ? w : 0);
                    vertices[vi + 1] = y + (i % 2 == 0 ? 0 : h);
                    vertices[vi + 2] = z + h;
                    vertices[vi + 3] = color[0] * 0.9f;
                    vertices[vi + 4] = color[1] * 0.9f;
                    vertices[vi + 5] = color[2] * 0.9f;
                }
                break;
            case 5: // -Z
                for (int i = 0; i < 4; i++) {
                    int vi = i * 6;
                    vertices[vi] = x + (i < 2 ? 0 : w);
                    vertices[vi + 1] = y + (i % 2 == 0 ? h : 0);
                    vertices[vi + 2] = z - 1;
                    vertices[vi + 3] = color[0] * 0.7f;
                    vertices[vi + 4] = color[1] * 0.7f;
                    vertices[vi + 5] = color[2] * 0.7f;
                }
                break;
        }

        mesh.addOpaqueQuad(vertices, QUAD_INDICES[0]);
    }

    private static float[] getBlockColor(short blockType) {
        int idx = blockType * 3;
        if (idx + 2 >= BLOCK_COLORS.length) {
            return new float[]{1.0f, 1.0f, 1.0f};
        }
        return new float[]{BLOCK_COLORS[idx], BLOCK_COLORS[idx + 1], BLOCK_COLORS[idx + 2]};
    }
}
