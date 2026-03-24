package com.novusforge.astrum.world

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * ChunkManager - Manages chunk loading, generation, and meshing
 * Formula: "the multithreading - 1.main thread 2.Worldgen 3.Mesh Generation"
 * Kotlin conversion: Using idiomatic properties, thread pooling, and locks.
 */
class ChunkManager(seed: Int) {
    companion object {
        const val RENDER_DISTANCE = 3
        const val UNLOAD_DISTANCE = 5
        
        fun getChunkKey(x: Int, y: Int, z: Int): Long {
            return (x.toLong() and 0xFFFFFFL) or ((y.toLong() and 0xFFFFFFL) shl 24) or ((z.toLong() and 0xFFFFFFL) shl 48)
        }

        fun getChunkCoords(key: Long): IntArray {
            return intArrayOf(
                (key and 0xFFFFFFL).toInt(),
                ((key shr 24) and 0xFFFFFFL).toInt(),
                ((key shr 48) and 0xFFFFFFL).toInt()
            )
        }
    }

    private val chunks = ConcurrentHashMap<Long, Chunk>()
    private val meshes = ConcurrentHashMap<Long, ChunkMesh>()
    private val noise: FastNoiseLite = FastNoiseLite(seed).apply {
        SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2)
    }
    
    // Multithreading (Formula optimization)
    private val worldGenExecutor: ExecutorService = Executors.newFixedThreadPool(
        Math.max(1, Runtime.getRuntime().availableProcessors() - 1)
    ) { r ->
        Thread(r, "WorldGen").apply { isDaemon = true }
    }
    
    // Mesh gen thread (single thread, synchronized)
    private val meshGenExecutor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "MeshGen").apply { isDaemon = true }
    }
    
    private val meshLock: ReadWriteLock = ReentrantReadWriteLock()

    @Volatile
    private var playerChunkX: Int = 0
    @Volatile
    private var playerChunkZ: Int = 0
    
    private val _pendingWorldGen = AtomicInteger(0)
    private val _pendingMeshGen = AtomicInteger(0)

    fun updatePlayerPosition(cx: Int, cy: Int, cz: Int) {
        this.playerChunkX = cx
        this.playerChunkZ = cz
    }

    /**
     * Tick - Load chunks around player
     */
    fun tick() {
        val px = playerChunkX
        val pz = playerChunkZ

        // Load chunks in render distance
        for (dx in -RENDER_DISTANCE..RENDER_DISTANCE) {
            for (dz in -RENDER_DISTANCE..RENDER_DISTANCE) {
                val cx = px + dx
                val cz = pz + dz
                val key = getChunkKey(cx, 0, cz)
                val distSq = dx * dx + dz * dz

                if (distSq <= RENDER_DISTANCE * RENDER_DISTANCE) {
                    if (!chunks.containsKey(key)) {
                        queueWorldGen(cx, 0, cz)
                    } else if (!meshes.containsKey(key)) {
                        queueMeshGen(cx, cz)
                    }
                }
            }
        }

        // Unload far chunks
        unloadFarChunks(px, pz)
    }

    private fun queueWorldGen(cx: Int, cy: Int, cz: Int) {
        val key = getChunkKey(cx, cy, cz)
        worldGenExecutor.submit {
            val chunk = generateChunk(cx, cy, cz)
            chunks[key] = chunk
            _pendingWorldGen.decrementAndGet()
            queueMeshGen(cx, cz)
        }
        _pendingWorldGen.incrementAndGet()
    }

    private fun queueMeshGen(cx: Int, cz: Int) {
        val key = getChunkKey(cx, 0, cz)
        meshGenExecutor.submit {
            try {
                val chunk = chunks[key]
                if (chunk != null) {
                    val mesh = GreedyMesher.generateMesh(chunk, this, cx, cz)
                    meshLock.writeLock().lock()
                    try {
                        val oldMesh = meshes.put(key, mesh)
                        oldMesh?.dispose()
                    } finally {
                        meshLock.writeLock().unlock()
                    }
                }
            } finally {
                _pendingMeshGen.decrementAndGet()
            }
        }
        _pendingMeshGen.incrementAndGet()
    }

    /**
     * Generate chunk terrain using FastNoiseLite
     * Formula: "better world generation(Perlin + simplex using FastnoiseLite)"
     */
    private fun generateChunk(cx: Int, cy: Int, cz: Int): Chunk {
        val chunk = Chunk()
        val baseX = cx * Chunk.SIZE
        val baseZ = cz * Chunk.SIZE

        for (x in 0 until Chunk.SIZE) {
            for (z in 0 until Chunk.SIZE) {
                val nx = (baseX + x) * 0.01f
                val nz = (baseZ + z) * 0.01f

                // Terrain height (30-70 range)
                val height = noise.GetNoise(nx, nz) * 20 + 50

                // Cave generation
                val cave = noise.GetNoise(nx * 2, nz * 2)
                val cave2 = noise.GetNoise(nx * 3, nz * 3)

                for (y in 0 until Chunk.SIZE) {
                    val worldY = cy * Chunk.SIZE + y
                    if (worldY < 0) continue

                    if (worldY < height - 4) {
                        // Underground
                        if (cave > 0.3f && cave2 > 0.3f) {
                            chunk.setBlock(x, y, z, 0.toShort()) // Air (cave)
                        } else if (worldY < height - 8) {
                            chunk.setBlock(x, y, z, 2.toShort()) // Stone
                        } else {
                            chunk.setBlock(x, y, z, 1.toShort()) // Dirt
                        }
                    } else if (worldY < height) {
                        chunk.setBlock(x, y, z, 3.toShort()) // Grass
                    }
                }
            }
        }
        return chunk
    }

    private fun unloadFarChunks(px: Int, pz: Int) {
        val keys = chunks.keys().toList()
        for (key in keys) {
            val coords = getChunkCoords(key)
            val dx = coords[0] - px
            val dz = coords[2] - pz
            val distSq = dx * dx + dz * dz

            if (distSq > UNLOAD_DISTANCE * UNLOAD_DISTANCE) {
                val removed = chunks.remove(key)
                removed?.dispose()
                
                meshLock.writeLock().lock()
                try {
                    val oldMesh = meshes.remove(key)
                    oldMesh?.dispose()
                } finally {
                    meshLock.writeLock().unlock()
                }
            }
        }
    }

    fun getBlock(worldX: Int, worldY: Int, worldZ: Int): Short {
        val cx = Math.floorDiv(worldX, Chunk.SIZE)
        val cz = Math.floorDiv(worldZ, Chunk.SIZE)
        val lx = Math.floorMod(worldX, Chunk.SIZE)
        val ly = Math.floorMod(worldY, Chunk.SIZE)
        val lz = Math.floorMod(worldZ, Chunk.SIZE)

        val chunk = chunks[getChunkKey(cx, 0, cz)] ?: return 0
        return chunk.getBlock(lx, ly, lz)
    }

    fun getChunk(cx: Int, cz: Int): Chunk? {
        return chunks[getChunkKey(cx, 0, cz)]
    }

    fun getMesh(cx: Int, cz: Int): ChunkMesh? {
        meshLock.readLock().lock()
        try {
            return meshes[getChunkKey(cx, 0, cz)]
        } finally {
            meshLock.readLock().unlock()
        }
    }

    fun getVisibleMeshes(px: Int, py: Int, pz: Int, frustumPlanes: FloatArray?): Map<Long, ChunkMesh> {
        val visible = ConcurrentHashMap<Long, ChunkMesh>()
        meshLock.readLock().lock()
        try {
            for ((key, mesh) in meshes) {
                val coords = getChunkCoords(key)
                val dx = coords[0] - px
                val dz = coords[2] - pz
                if (dx * dx + dz * dz > RENDER_DISTANCE * RENDER_DISTANCE) continue

                // Frustum culling (stub - always visible for now)
                if (FrustumCuller.isChunkVisible(coords[0], 0, coords[2], frustumPlanes)) {
                    visible[key] = mesh
                }
            }
        } finally {
            meshLock.readLock().unlock()
        }
        return visible
    }

    fun regenerateMesh(cx: Int, cy: Int, cz: Int) {
        queueMeshGen(cx, cz)
    }

    val pendingWorldGen: Int get() = _pendingWorldGen.get()
    val pendingMeshGen: Int get() = _pendingMeshGen.get()
    val loadedChunkCount: Int get() = chunks.size

    fun dispose() {
        worldGenExecutor.shutdownNow()
        meshGenExecutor.shutdownNow()
        for (chunk in chunks.values) chunk.dispose()
        chunks.clear()
        for (mesh in meshes.values) mesh.dispose()
        meshes.clear()
    }
}
