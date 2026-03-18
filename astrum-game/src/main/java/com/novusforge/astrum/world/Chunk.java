package com.novusforge.astrum.world;

/**
 * High-performance Voxel Chunk using 1D arrays for cache locality.
 * Based on Formulas Part 3: 32x32x32 chunks.
 */
public class Chunk {
    public static final int SIZE = 32;
    public static final int VOLUME = SIZE * SIZE * SIZE;
    
    // Using short[] for block IDs as requested to allow for many block types (exceeding 256)
    private final short[] blocks;

    public Chunk() {
        this.blocks = new short[VOLUME];
    }

    public void setBlock(int x, int y, int z, short blockId) {
        if (isOutOfBounds(x, y, z)) return;
        blocks[getIndex(x, y, z)] = blockId;
    }

    public short getBlock(int x, int y, int z) {
        if (isOutOfBounds(x, y, z)) return 0;
        return blocks[getIndex(x, y, z)];
    }

    private int getIndex(int x, int y, int z) {
        return x | (y << 5) | (z << 10); // 5 bits for 32 size
    }

    private boolean isOutOfBounds(int x, int y, int z) {
        return x < 0 || x >= SIZE || y < 0 || y >= SIZE || z < 0 || z >= SIZE;
    }

    public short[] getBlocks() {
        return blocks;
    }

    public void dispose() {
        for (int i = 0; i < blocks.length; i++) {
            blocks[i] = 0;
        }
    }
}
