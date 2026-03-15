package com.novusforge.astrum.common.world;

/**
 * High-performance voxel data storage for a single chunk.
 * Uses a 1D byte array for cache locality and minimal memory overhead.
 * Chunk size is hardcoded to 32x32x32 according to "The Formula".
 */
public class VoxelData {
    public static final int CHUNK_SIZE = 32;
    public static final int CHUNK_VOLUME = CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE;

    private final byte[] blocks;

    public VoxelData() {
        this.blocks = new byte[CHUNK_VOLUME];
    }

    public void setBlock(int x, int y, int z, byte blockId) {
        blocks[getIndex(x, y, z)] = blockId;
    }

    public byte getBlock(int x, int y, int z) {
        return blocks[getIndex(x, y, z)];
    }

    private int getIndex(int x, int y, int z) {
        return (x * CHUNK_SIZE + y) * CHUNK_SIZE + z;
    }

    public byte[] getRawData() {
        return blocks;
    }
}
