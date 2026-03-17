package com.novusforge.astrum.api.world;

/**
 * Interface for chunk data access in the modding API.
 */
public interface IChunkData {
    
    /**
     * Get chunk X coordinate
     */
    int getX();
    
    /**
     * Get chunk Z coordinate
     */
    int getZ();
    
    /**
     * Get block ID in local chunk coordinates
     */
    byte getBlock(int localX, int localY, int localZ);
    
    /**
     * Set block in local chunk coordinates
     */
    void setBlock(int localX, int localY, int localZ, byte blockId);
    
    /**
     * Get chunk height at local X, Z
     */
    int getHeight(int localX, int localZ);
    
    /**
     * Check if chunk is empty (no blocks)
     */
    boolean isEmpty();
    
    /**
     * Mark chunk for mesh rebuild
     */
    void markDirty();
}
