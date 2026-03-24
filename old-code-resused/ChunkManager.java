package com.novusforge.astrum.world;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ChunkManager - Manages chunk loading, generation, and meshing
 * Formula: "the multithreading - 1.main thread 2.Worldgen 3.Mesh Generation"
 */
public class ChunkManager {
    public static final int RENDER_DISTANCE = 3;
    public static final int UNLOAD_DISTANCE = 5;

    private final Map<Long, Chunk> chunks = new ConcurrentHashMap<>();
    private final Map<Long, ChunkMesh> meshes = new ConcurrentHashMap<>();
    private final FastNoiseLite noise;
    
    // Multithreading (Formula optimization)
    private final ExecutorService worldGenExecutor;
    private final ExecutorService meshGenExecutor;
    private final ReadWriteLock meshLock = new ReentrantReadWriteLock();

    private volatile int playerChunkX;
    private volatile int playerChunkZ;
    private final AtomicInteger pendingWorldGen = new AtomicInteger(0);
    private final AtomicInteger pendingMeshGen = new AtomicInteger(0);

    public ChunkManager(int seed) {
        this.noise = new FastNoiseLite(seed);
        this.noise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        
        // World gen thread (separate from mesh gen)
        this.worldGenExecutor = Executors.newFixedThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() - 1),
            r -> {
                Thread t = new Thread(r, "WorldGen");
                t.setDaemon(true);
                return t;
            }
        );
        
        // Mesh gen thread (single thread, synchronized)
        this.meshGenExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "MeshGen");
            t.setDaemon(true);
            return t;
        });
    }

    public void updatePlayerPosition(int cx, int cy, int cz) {
        this.playerChunkX = cx;
        this.playerChunkZ = cz;
    }

    /**
     * Tick - Load chunks around player
     */
    public void tick() {
        int px = playerChunkX;
        int pz = playerChunkZ;

        // Load chunks in render distance
        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                int cx = px + dx;
                int cz = pz + dz;
                long key = getChunkKey(cx, 0, cz);
                int distSq = dx * dx + dz * dz;

                if (distSq <= RENDER_DISTANCE * RENDER_DISTANCE) {
                    if (!chunks.containsKey(key)) {
                        queueWorldGen(cx, 0, cz);
                    } else if (!meshes.containsKey(key)) {
                        queueMeshGen(cx, cz);
                    }
                }
            }
        }

        // Unload far chunks
        unloadFarChunks(px, pz);
    }

    private void queueWorldGen(int cx, int cy, int cz) {
        long key = getChunkKey(cx, cy, cz);
        worldGenExecutor.submit(() -> {
            Chunk chunk = generateChunk(cx, cy, cz);
            chunks.put(key, chunk);
            pendingWorldGen.decrementAndGet();
            queueMeshGen(cx, cz);
        });
        pendingWorldGen.incrementAndGet();
    }

    private void queueMeshGen(int cx, int cz) {
        long key = getChunkKey(cx, 0, cz);
        meshGenExecutor.submit(() -> {
            try {
                Chunk chunk = chunks.get(key);
                if (chunk != null) {
                    ChunkMesh mesh = GreedyMesher.generateMesh(chunk, this, cx, cz);
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
            } finally {
                pendingMeshGen.decrementAndGet();
            }
        });
        pendingMeshGen.incrementAndGet();
    }

    /**
     * Generate chunk terrain using FastNoiseLite
     * Formula: "better world generation(Perlin + simplex using FastnoiseLite)"
     */
    private Chunk generateChunk(int cx, int cy, int cz) {
        Chunk chunk = new Chunk();
        int baseX = cx * Chunk.SIZE;
        int baseZ = cz * Chunk.SIZE;

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                float nx = (baseX + x) * 0.01f;
                float nz = (baseZ + z) * 0.01f;

                // Terrain height (30-70 range)
                float height = (float) noise.GetNoise(nx, nz) * 20 + 50;

                // Cave generation
                float cave = (float) noise.GetNoise(nx * 2, nz * 2);
                float cave2 = (float) noise.GetNoise(nx * 3, nz * 3);

                for (int y = 0; y < Chunk.SIZE; y++) {
                    int worldY = cy * Chunk.SIZE + y;
                    if (worldY < 0) continue;

                    if (worldY < height - 4) {
                        // Underground
                        if (cave > 0.3f && cave2 > 0.3f) {
                            chunk.setBlock(x, y, z, (short) 0); // Air (cave)
                        } else if (worldY < height - 8) {
                            chunk.setBlock(x, y, z, (short) 2); // Stone
                        } else {
                            chunk.setBlock(x, y, z, (short) 1); // Dirt
                        }
                    } else if (worldY < height) {
                        chunk.setBlock(x, y, z, (short) 3); // Grass
                    }
                }
            }
        }
        return chunk;
    }

    private void unloadFarChunks(int px, int pz) {
        long[] keys = chunks.keySet().stream().mapToLong(Long::longValue).toArray();
        for (long key : keys) {
            int[] coords = getChunkCoords(key);
            int dx = coords[0] - px;
            int dz = coords[2] - pz;
            int distSq = dx * dx + dz * dz;

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
        int cz = Math.floorDiv(worldZ, Chunk.SIZE);
        int lx = Math.floorMod(worldX, Chunk.SIZE);
        int ly = Math.floorMod(worldY, Chunk.SIZE);
        int lz = Math.floorMod(worldZ, Chunk.SIZE);

        Chunk chunk = chunks.get(getChunkKey(cx, 0, cz));
        if (chunk == null) return 0;
        return chunk.getBlock(lx, ly, lz);
    }

    public Chunk getChunk(int cx, int cz) {
        return chunks.get(getChunkKey(cx, 0, cz));
    }

    public ChunkMesh getMesh(int cx, int cz) {
        meshLock.readLock().lock();
        try {
            return meshes.get(getChunkKey(cx, 0, cz));
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
                int dz = coords[2] - pz;
                if (dx * dx + dz * dz > RENDER_DISTANCE * RENDER_DISTANCE) continue;

                // Frustum culling (stub - always visible for now)
                if (FrustumCuller.isChunkVisible(coords[0], 0, coords[2], frustumPlanes)) {
                    visible.put(entry.getKey(), entry.getValue());
                }
            }
        } finally {
            meshLock.readLock().unlock();
        }
        return visible;
    }

    public void regenerateMesh(int cx, int cy, int cz) {
        queueMeshGen(cx, cz);
    }

    public int getPendingWorldGen() { return pendingWorldGen.get(); }
    public int getPendingMeshGen() { return pendingMeshGen.get(); }
    public int getLoadedChunkCount() { return chunks.size(); }

    public void dispose() {
        worldGenExecutor.shutdownNow();
        meshGenExecutor.shutdownNow();
        for (Chunk chunk : chunks.values()) chunk.dispose();
        chunks.clear();
        for (ChunkMesh mesh : meshes.values()) mesh.dispose();
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
