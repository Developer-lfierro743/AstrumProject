package com.novusforge.astrum.world;

import java.util.Map;
import java.util.function.Consumer;
import org.joml.Vector3i;

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

    public void update(float dt) {
        chunkManager.tick();
    }

    public void updatePlayerPosition(float x, float y, float z) {
        int cx = (int) Math.floor(x / Chunk.SIZE);
        int cz = (int) Math.floor(z / Chunk.SIZE);
        chunkManager.updatePlayerPosition(cx, 0, cz);
    }

    public short getBlock(int worldX, int worldY, int worldZ) {
        return chunkManager.getBlock(worldX, worldY, worldZ);
    }

    public void setBlock(int worldX, int worldY, int worldZ, short blockId) {
        int cx = Math.floorDiv(worldX, Chunk.SIZE);
        int cy = Math.floorDiv(worldY, Chunk.SIZE);
        int cz = Math.floorDiv(worldZ, Chunk.SIZE);
        int lx = Math.floorMod(worldX, Chunk.SIZE);
        int ly = Math.floorMod(worldY, Chunk.SIZE);
        int lz = Math.floorMod(worldZ, Chunk.SIZE);
        
        Chunk chunk = chunkManager.getChunk(cx, cz);
        if (chunk != null) {
            chunk.setBlock(lx, ly, lz, blockId);
            chunkManager.regenerateMesh(cx, cy, cz);
        }
    }

    public Chunk getChunk(int cx, int cz) {
        return chunkManager.getChunk(cx, cz);
    }

    public Vector3i getChunkCoord(int worldX, int worldY, int worldZ) {
        int cx = Math.floorDiv(worldX, Chunk.SIZE);
        int cy = Math.floorDiv(worldY, Chunk.SIZE);
        int cz = Math.floorDiv(worldZ, Chunk.SIZE);
        return new Vector3i(cx, cy, cz);
    }

    public ChunkMesh getMesh(int cx, int cz) {
        return chunkManager.getMesh(cx, cz);
    }

    public Map<Long, ChunkMesh> getVisibleMeshes(float px, float py, float pz, float[] frustumPlanes) {
        int pcx = (int) Math.floor(px / Chunk.SIZE);
        int pcz = (int) Math.floor(pz / Chunk.SIZE);
        return chunkManager.getVisibleMeshes(pcx, 0, pcz, frustumPlanes);
    }

    public boolean isChunkReady(int cx, int cz) {
        return chunkManager.isChunkReady(cx, cz);
    }

    public int getPendingWorldGen() { return chunkManager.getPendingWorldGen(); }
    public int getPendingMeshGen() { return chunkManager.getPendingMeshGen(); }
    public int getLoadedChunkCount() { return chunkManager.getLoadedChunkCount(); }

    public ChunkManager getChunkManager() {
        return chunkManager;
    }

    public int getSeed() {
        return seed;
    }

    public static void setBufferDeleter(Consumer<Long> deleter) {
        ChunkMesh.setBufferDeleter(deleter);
    }

    public void dispose() {
        chunkManager.dispose();
    }
}
