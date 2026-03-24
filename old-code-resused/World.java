package com.novusforge.astrum.world;

import java.util.Map;

/**
 * World - Manages chunks and block access
 * Pre-Classic implementation
 */
public class World {
    private final ChunkManager chunkManager;
    private final int seed;

    public World() {
        this((int) (System.currentTimeMillis() & 0xFFFFFFFF));
    }

    public World(int seed) {
        this.seed = seed;
        this.chunkManager = new ChunkManager(seed);
    }

    /**
     * Update player position for chunk loading
     */
    public void updatePlayerPosition(float x, float y, float z) {
        int cx = (int) Math.floor(x / Chunk.SIZE);
        int cz = (int) Math.floor(z / Chunk.SIZE);
        chunkManager.updatePlayerPosition(cx, 0, cz);
    }

    /**
     * Get block at world coordinates
     */
    public short getBlock(int worldX, int worldY, int worldZ) {
        return chunkManager.getBlock(worldX, worldY, worldZ);
    }

    /**
     * Set block at world coordinates
     */
    public void setBlock(int worldX, int worldY, int worldZ, short blockId) {
        int cx = Math.floorDiv(worldX, Chunk.SIZE);
        int cz = Math.floorDiv(worldZ, Chunk.SIZE);
        int lx = Math.floorMod(worldX, Chunk.SIZE);
        int ly = Math.floorMod(worldY, Chunk.SIZE);
        int lz = Math.floorMod(worldZ, Chunk.SIZE);

        Chunk chunk = chunkManager.getChunk(cx, cz);
        if (chunk != null) {
            chunk.setBlock(lx, ly, lz, blockId);
            chunkManager.regenerateMesh(cx, 0, cz);
        }
    }

    /**
     * Get chunk at coordinates
     */
    public Chunk getChunk(int cx, int cz) {
        return chunkManager.getChunk(cx, cz);
    }

    /**
     * Get visible chunk meshes (with frustum culling)
     */
    public Map<Long, ChunkMesh> getVisibleMeshes(float px, float py, float pz, float[] frustumPlanes) {
        int pcx = (int) Math.floor(px / Chunk.SIZE);
        int pcz = (int) Math.floor(pz / Chunk.SIZE);
        return chunkManager.getVisibleMeshes(pcx, 0, pcz, frustumPlanes);
    }

    public ChunkManager getChunkManager() { return chunkManager; }
    public int getSeed() { return seed; }
    public int getLoadedChunkCount() { return chunkManager.getLoadedChunkCount(); }
    public int getPendingWorldGen() { return chunkManager.getPendingWorldGen(); }
    public int getPendingMeshGen() { return chunkManager.getPendingMeshGen(); }

    public static void setBufferDeleter(java.util.function.BiConsumer<Long, Long> deleter) {
        ChunkMesh.setBufferDeleter(deleter);
    }

    public void dispose() {
        chunkManager.dispose();
    }
}
