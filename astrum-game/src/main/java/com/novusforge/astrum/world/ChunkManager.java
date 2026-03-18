package com.novusforge.astrum.world;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ChunkManager {
    public static final int RENDER_DISTANCE = 8;
    public static final int UNLOAD_DISTANCE = 12;
    
    private final Map<Long, Chunk> chunks = new ConcurrentHashMap<>();
    private final Map<Long, ChunkMesh> meshes = new ConcurrentHashMap<>();
    private final FastNoiseLite noise;
    private final ExecutorService worldGenExecutor;
    private final ExecutorService meshGenExecutor;
    private final ReadWriteLock meshLock = new ReentrantReadWriteLock();
    
    private volatile int playerChunkX;
    private volatile int playerChunkY;
    private volatile int playerChunkZ;
    private final AtomicInteger pendingWorldGen = new AtomicInteger(0);
    private final AtomicInteger pendingMeshGen = new AtomicInteger(0);

    public ChunkManager(int seed) {
        this.noise = new FastNoiseLite(seed);
        this.noise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        this.worldGenExecutor = Executors.newFixedThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() - 1),
            r -> {
                Thread t = new Thread(r, "WorldGen-" + System.nanoTime());
                t.setDaemon(true);
                return t;
            }
        );
        this.meshGenExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "MeshGen");
            t.setDaemon(true);
            return t;
        });
    }

    public void updatePlayerPosition(int cx, int cy, int cz) {
        this.playerChunkX = cx;
        this.playerChunkY = cy;
        this.playerChunkZ = cz;
    }

    public void tick() {
        int px = playerChunkX;
        int py = playerChunkY;
        int pz = playerChunkZ;

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                    int cx = px + dx;
                    int cy = py + dy;
                    int cz = pz + dz;
                    long key = getChunkKey(cx, cy, cz);
                    int distSq = dx * dx + dy * dy + dz * dz;

                    if (distSq <= RENDER_DISTANCE * RENDER_DISTANCE) {
                        if (!chunks.containsKey(key)) {
                            queueWorldGen(cx, cy, cz);
                        } else if (!meshes.containsKey(key)) {
                            queueMeshGen(cx, cy, cz);
                        }
                    }
                }
            }
        }

        unloadFarChunks(px, py, pz);
    }

    private void queueWorldGen(int cx, int cy, int cz) {
        long key = getChunkKey(cx, cy, cz);
        worldGenExecutor.submit(() -> {
            Chunk chunk = generateChunk(cx, cy, cz);
            chunks.put(key, chunk);
            pendingWorldGen.decrementAndGet();
            queueMeshGen(cx, cy, cz);
        });
        pendingWorldGen.incrementAndGet();
    }

    private void queueMeshGen(int cx, int cy, int cz) {
        long key = getChunkKey(cx, cy, cz);
        meshGenExecutor.submit(() -> {
            Chunk chunk = chunks.get(key);
            if (chunk != null) {
                ChunkMesh mesh = GreedyMesher.generateMesh(chunk, this, cx, cy, cz);
                meshLock.writeLock().lock();
                try {
                    ChunkMesh oldMesh = meshes.put(key, mesh);
                    if (oldMesh != null) {
                        oldMesh.dispose();
                    }
                } finally {
                    meshLock.writeLock().unlock();
                }
            }
            pendingMeshGen.decrementAndGet();
        });
        pendingMeshGen.incrementAndGet();
    }

    private Chunk generateChunk(int cx, int cy, int cz) {
        Chunk chunk = new Chunk();
        int baseX = cx * Chunk.SIZE;
        int baseZ = cz * Chunk.SIZE;

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                float nx = (baseX + x) * 0.01f;
                float nz = (baseZ + z) * 0.01f;
                float height = noise.GetNoise(nx, nz) * 16 + 16;
                float cave = noise.GetNoise(nx * 2, nz * 2);
                float cave2 = noise.GetNoise(nx * 3, nz * 3);

                for (int y = 0; y < Chunk.SIZE; y++) {
                    int worldY = cy * Chunk.SIZE + y;
                    if (worldY < 0) continue;

                    if (worldY < height - 4) {
                        if (cave > 0.3f && cave2 > 0.3f) {
                            chunk.setBlock(x, y, z, (short) 0);
                        } else if (worldY < height - 8) {
                            chunk.setBlock(x, y, z, (short) 2);
                        } else {
                            chunk.setBlock(x, y, z, (short) 1);
                        }
                    } else if (worldY < height) {
                        chunk.setBlock(x, y, z, (short) 3);
                    } else if (worldY == (int) height && worldY < 30) {
                        chunk.setBlock(x, y, z, (short) 4);
                    }
                }
            }
        }
        return chunk;
    }

    private void unloadFarChunks(int px, int py, int pz) {
        long[] keys = chunks.keySet().stream().mapToLong(Long::longValue).toArray();
        for (long key : keys) {
            int[] coords = getChunkCoords(key);
            int dx = coords[0] - px;
            int dy = coords[1] - py;
            int dz = coords[2] - pz;
            int distSq = dx * dx + dy * dy + dz * dz;

            if (distSq > UNLOAD_DISTANCE * UNLOAD_DISTANCE) {
                Chunk removed = chunks.remove(key);
                if (removed != null) {
                    removed.dispose();
                }
                meshLock.writeLock().lock();
                try {
                    ChunkMesh oldMesh = meshes.remove(key);
                    if (oldMesh != null) {
                        oldMesh.dispose();
                    }
                } finally {
                    meshLock.writeLock().unlock();
                }
            }
        }
    }

    public short getBlock(int worldX, int worldY, int worldZ) {
        int cx = Math.floorDiv(worldX, Chunk.SIZE);
        int cy = Math.floorDiv(worldY, Chunk.SIZE);
        int cz = Math.floorDiv(worldZ, Chunk.SIZE);
        int lx = Math.floorMod(worldX, Chunk.SIZE);
        int ly = Math.floorMod(worldY, Chunk.SIZE);
        int lz = Math.floorMod(worldZ, Chunk.SIZE);

        Chunk chunk = chunks.get(getChunkKey(cx, cy, cz));
        if (chunk == null) return 0;
        return chunk.getBlock(lx, ly, lz);
    }

    public Chunk getChunk(int cx, int cy, int cz) {
        return chunks.get(getChunkKey(cx, cy, cz));
    }

    public ChunkMesh getMesh(int cx, int cy, int cz) {
        meshLock.readLock().lock();
        try {
            return meshes.get(getChunkKey(cx, cy, cz));
        } finally {
            meshLock.readLock().unlock();
        }
    }

    public Map<Long, ChunkMesh> getVisibleMeshes(int px, int py, int pz, float[] frustumPlanes) {
        Map<Long, ChunkMesh> visible = new ConcurrentHashMap<>();
        meshLock.readLock().lock();
        try {
            for (Map.Entry<Long, ChunkMesh> entry : meshes.entrySet()) {
                int[] coords = getChunkCoords(entry.getKey());
                int dx = coords[0] - px;
                int dy = coords[1] - py;
                int dz = coords[2] - pz;
                if (dx * dx + dy * dy + dz * dz > RENDER_DISTANCE * RENDER_DISTANCE) continue;

                if (FrustumCuller.isChunkVisible(coords[0], coords[1], coords[2], frustumPlanes)) {
                    visible.put(entry.getKey(), entry.getValue());
                }
            }
        } finally {
            meshLock.readLock().unlock();
        }
        return visible;
    }

    public boolean isChunkReady(int cx, int cy, int cz) {
        return meshes.containsKey(getChunkKey(cx, cy, cz));
    }

    public int getPendingWorldGen() { return pendingWorldGen.get(); }
    public int getPendingMeshGen() { return pendingMeshGen.get(); }
    public int getLoadedChunkCount() { return chunks.size(); }

    public void dispose() {
        worldGenExecutor.shutdownNow();
        meshGenExecutor.shutdownNow();
        for (Chunk chunk : chunks.values()) {
            chunk.dispose();
        }
        chunks.clear();
        for (ChunkMesh mesh : meshes.values()) {
            mesh.dispose();
        }
        meshes.clear();
    }

    public static long getChunkKey(int x, int y, int z) {
        return ((long) x & 0xFFFFFF) | (((long) y & 0xFFFFFF) << 24) | (((long) z & 0xFFFFFF) << 48);
    }

    public static int[] getChunkCoords(long key) {
        return new int[] {
            (int) (key & 0xFFFFFF),
            (int) ((key >> 24) & 0xFFFFFF),
            (int) ((key >> 48) & 0xFFFFFF)
        };
    }
}
