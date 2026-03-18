package com.novusforge.astrum.world;

public class GreedyMesher {

    private static final int[][][] FACE_OFFSETS = {
        {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}},
        {{0, 0, 1}, {0, 0, -1}, {0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0}}
    };

    private static final int[][] QUAD_INDICES = {
        {0, 1, 2, 0, 2, 3}
    };

    public static ChunkMesh generateMesh(Chunk chunk, ChunkManager manager, int cx, int cy, int cz) {
        ChunkMesh mesh = new ChunkMesh();
        int baseX = cx * Chunk.SIZE;
        int baseY = cy * Chunk.SIZE;
        int baseZ = cz * Chunk.SIZE;

        for (int face = 0; face < 6; face++) {
            int[] off = FACE_OFFSETS[0][face];
            int d = face % 2 == 0 ? 1 : -1;

            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int x = 0; x < Chunk.SIZE; ) {
                    int z = 0;
                    while (z < Chunk.SIZE) {
                        short current = chunk.getBlock(x, y, z);
                        if (current == 0) {
                            z++;
                            continue;
                        }

                        int w = 1;
                        while (x + w < Chunk.SIZE && 
                               chunk.getBlock(x + w, y, z) == current &&
                               shouldMerge(chunk, x + w, y, z, x + w - 1, y, z, face, manager)) {
                            w++;
                        }

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

                        addFaceToMesh(mesh, x + baseX, y + baseY, z + baseZ, 
                                      w, h, face, current, d);

                        x += w;
                        z++;
                    }
                }
            }
        }

        return mesh;
    }

    private static boolean shouldMerge(Chunk chunk, int x, int y, int z, int nx, int ny, int nz, int face, ChunkManager manager) {
        if (x < 0 || x >= Chunk.SIZE || y < 0 || y >= Chunk.SIZE || z < 0 || z >= Chunk.SIZE) {
            return false;
        }
        short block = chunk.getBlock(x, y, z);
        return block != 0;
    }

    private static void addFaceToMesh(ChunkMesh mesh, int x, int y, int z, int w, int h, int face, short blockType, int d) {
        float u0 = 0, u1 = w;
        float v0 = 0, v1 = h;
        int blockColor = getBlockColor(blockType);

        float[] vertices = new float[32];

        switch (face) {
            case 0:
                for (int i = 0; i < 4; i++) {
                    int vi = i * 8;
                    vertices[vi] = x + (i < 2 ? w : 0);
                    vertices[vi + 1] = y + (i % 2 == 0 ? 0 : h);
                    vertices[vi + 2] = z + 1;
                    vertices[vi + 3] = 0; vertices[vi + 4] = 0; vertices[vi + 5] = 1;
                    vertices[vi + 6] = (i < 2 ? u1 : u0);
                    vertices[vi + 7] = (i % 2 == 0 ? v0 : v1);
                }
                break;
            case 1:
                for (int i = 0; i < 4; i++) {
                    int vi = i * 8;
                    vertices[vi] = x + (i < 2 ? 0 : -w);
                    vertices[vi + 1] = y + (i % 2 == 0 ? 0 : h);
                    vertices[vi + 2] = z - 1;
                    vertices[vi + 3] = 0; vertices[vi + 4] = 0; vertices[vi + 5] = -1;
                    vertices[vi + 6] = (i < 2 ? u1 : u0);
                    vertices[vi + 7] = (i % 2 == 0 ? v0 : v1);
                }
                break;
            case 2:
                for (int i = 0; i < 4; i++) {
                    int vi = i * 8;
                    vertices[vi] = x + (i < 2 ? 0 : w);
                    vertices[vi + 1] = y + h;
                    vertices[vi + 2] = z + (i % 2 == 0 ? 0 : h);
                    vertices[vi + 3] = 0; vertices[vi + 4] = 1; vertices[vi + 5] = 0;
                    vertices[vi + 6] = (i < 2 ? u1 : u0);
                    vertices[vi + 7] = (i % 2 == 0 ? v0 : v1);
                }
                break;
            case 3:
                for (int i = 0; i < 4; i++) {
                    int vi = i * 8;
                    vertices[vi] = x + (i < 2 ? 0 : w);
                    vertices[vi + 1] = y - 1;
                    vertices[vi + 2] = z + (i % 2 == 0 ? h : 0);
                    vertices[vi + 3] = 0; vertices[vi + 4] = -1; vertices[vi + 5] = 0;
                    vertices[vi + 6] = (i < 2 ? u1 : u0);
                    vertices[vi + 7] = (i % 2 == 0 ? v0 : v1);
                }
                break;
            case 4:
                for (int i = 0; i < 4; i++) {
                    int vi = i * 8;
                    vertices[vi] = x + (i < 2 ? w : 0);
                    vertices[vi + 1] = y + (i % 2 == 0 ? 0 : h);
                    vertices[vi + 2] = z + h;
                    vertices[vi + 3] = 0; vertices[vi + 4] = 0; vertices[vi + 5] = 1;
                    vertices[vi + 6] = (i < 2 ? u1 : u0);
                    vertices[vi + 7] = (i % 2 == 0 ? v0 : v1);
                }
                break;
            case 5:
                for (int i = 0; i < 4; i++) {
                    int vi = i * 8;
                    vertices[vi] = x + (i < 2 ? 0 : w);
                    vertices[vi + 1] = y + (i % 2 == 0 ? h : 0);
                    vertices[vi + 2] = z - 1;
                    vertices[vi + 3] = 0; vertices[vi + 4] = 0; vertices[vi + 5] = -1;
                    vertices[vi + 6] = (i < 2 ? u1 : u0);
                    vertices[vi + 7] = (i % 2 == 0 ? v0 : v1);
                }
                break;
        }

        if (isTransparent(blockType)) {
            mesh.addTransparentQuad(vertices, QUAD_INDICES[0]);
        } else {
            mesh.addOpaqueQuad(vertices, QUAD_INDICES[0]);
        }
    }

    private static int getBlockColor(short blockType) {
        switch (blockType) {
            case 1: return 0x808080;
            case 2: return 0x8B4513;
            case 3: return 0x228B22;
            case 4: return 0x4169E1;
            default: return 0xFFFFFF;
        }
    }

    private static boolean isTransparent(short blockType) {
        return blockType == 0;
    }
}
