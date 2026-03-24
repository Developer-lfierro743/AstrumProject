package com.novusforge.astrum.world;

/**
 * Chunk - 32×32×32 voxel container
 * Formula: "32(WIDTH)x32(HEIGHT)x32(LENGTH) and better performance"
 * "one-dimensional byte array (better performance)"
 * "upgrade chunk data blocks as shorts (more blocks)"
 */
public class Chunk {
    public static final int SIZE = 32;
    public static final int SIZE_BITS = 5; // log2(32) = 5
    public static final int VOLUME = SIZE * SIZE * SIZE;

    // Using short[] for block IDs (supports 65536 block types)
    // 1D array for cache locality (Formula optimization)
    private final short[] blocks;

    public Chunk() {
        this.blocks = new short[VOLUME];
    }

    /**
     * Set block at local coordinates
     */
    public void setBlock(int x, int y, int z, short blockId) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE || z < 0 || z >= SIZE) return;
        blocks[getIndex(x, y, z)] = blockId;
    }

    /**
     * Get block at local coordinates
     */
    public short getBlock(int x, int y, int z) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE || z < 0 || z >= SIZE) return 0;
        return blocks[getIndex(x, y, z)];
    }

    /**
     * Fast 1D index calculation using bit shifts
     * Formula: "x | (y << 5) | (z << 10)" for 32-size chunks
     */
    private int getIndex(int x, int y, int z) {
        return (x & 0x1F) | ((y & 0x1F) << 5) | ((z & 0x1F) << 10);
    }

    /**
     * Get all blocks (for mesh generation)
     */
    public short[] getBlocks() {
        return blocks;
    }

    /**
     * Clear chunk data
     */
    public void dispose() {
        for (int i = 0; i < blocks.length; i++) {
            blocks[i] = 0;
        }
    }
}
