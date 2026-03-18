package com.novusforge.astrum.world;

import java.util.Map;
import java.util.function.Consumer;

public class World {
    private final ChunkManager chunkManager;
    private final int seed;

    public World(int seed) {
        this.seed = seed;
        this.chunkManager = new ChunkManager(seed);
    }

    public void update(float dt) {
        chunkManager.tick();
    }

    public void updatePlayerPosition(float x, float y, float z) {
        int cx = (int) Math.floor(x / Chunk.SIZE);
        int cy = (int) Math.floor(y / Chunk.SIZE);
        int cz = (int) Math.floor(z / Chunk.SIZE);
        chunkManager.updatePlayerPosition(cx, cy, cz);
    }

    public short getBlock(int worldX, int worldY, int worldZ) {
        return chunkManager.getBlock(worldX, worldY, worldZ);
    }

    public Chunk getChunk(int cx, int cy, int cz) {
        return chunkManager.getChunk(cx, cy, cz);
    }

    public ChunkMesh getMesh(int cx, int cy, int cz) {
        return chunkManager.getMesh(cx, cy, cz);
    }

    public Map<Long, ChunkMesh> getVisibleMeshes(float px, float py, float pz, float[] frustumPlanes) {
        int pcx = (int) Math.floor(px / Chunk.SIZE);
        int pcy = (int) Math.floor(py / Chunk.SIZE);
        int pcz = (int) Math.floor(pz / Chunk.SIZE);
        return chunkManager.getVisibleMeshes(pcx, pcy, pcz, frustumPlanes);
    }

    public boolean isChunkReady(int cx, int cy, int cz) {
        return chunkManager.isChunkReady(cx, cy, cz);
    }

    public int getPendingWorldGen() { return chunkManager.getPendingWorldGen(); }
    public int getPendingMeshGen() { return chunkManager.getPendingMeshGen(); }
    public int getLoadedChunkCount() { return chunkManager.getLoadedChunkCount(); }

    public ChunkManager getChunkManager() {
        return chunkManager;
    }

    public static void setBufferDeleter(Consumer<Long> deleter) {
        ChunkMesh.setBufferDeleter(deleter);
    }

    public void dispose() {
        chunkManager.dispose();
    }
}
