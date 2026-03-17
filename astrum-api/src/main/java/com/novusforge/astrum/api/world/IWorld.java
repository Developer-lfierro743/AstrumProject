package com.novusforge.astrum.api.world;

/**
 * Interface for world access in the modding API.
 * Provides modders with a safe way to interact with the world.
 */
public interface IWorld {
    
    /**
     * Get block ID at specified position
     */
    byte getBlock(int x, int y, int z);
    
    /**
     * Set block at specified position
     */
    void setBlock(int x, int y, int z, byte blockId);
    
    /**
     * Get world seed for procedural generation
     */
    long getSeed();
    
    /**
     * Get world dimension (overworld, nether, end)
     */
    String getDimension();
    
    /**
     * Check if chunk is loaded
     */
    boolean isChunkLoaded(int chunkX, int chunkZ);
    
    /**
     * Get chunk data
     */
    IChunkData getChunk(int chunkX, int chunkZ);
    
    /**
     * Get world time
     */
    long getTime();
    
    /**
     * Set world time
     */
    void setTime(long time);
    
    /**
     * Get weather state (0 = clear, 1 = rain, 2 = snow)
     */
    int getWeather();
    
    /**
     * Set weather state
     */
    void setWeather(int weatherId);
}
