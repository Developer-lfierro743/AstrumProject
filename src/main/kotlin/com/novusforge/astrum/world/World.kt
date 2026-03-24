package com.novusforge.astrum.world

import java.util.function.BiConsumer

/**
 * World - Manages chunks and block access
 * Pre-Classic implementation
 * Kotlin conversion: Using primary constructor and idiomatic method structure.
 */
class World(val seed: Int = (System.currentTimeMillis() and 0xFFFFFFFFL).toInt()) {
    val chunkManager: ChunkManager = ChunkManager(seed)

    /**
     * Update player position for chunk loading
     */
    fun updatePlayerPosition(x: Float, y: Float, z: Float) {
        val cx = Math.floor((x / Chunk.SIZE).toDouble()).toInt()
        val cz = Math.floor((z / Chunk.SIZE).toDouble()).toInt()
        chunkManager.updatePlayerPosition(cx, 0, cz)
    }

    /**
     * Get block at world coordinates
     */
    fun getBlock(worldX: Int, worldY: Int, worldZ: Int): Short {
        return chunkManager.getBlock(worldX, worldY, worldZ)
    }

    /**
     * Set block at world coordinates
     */
    fun setBlock(worldX: Int, worldY: Int, worldZ: Int, blockId: Short) {
        val cx = Math.floorDiv(worldX, Chunk.SIZE)
        val cz = Math.floorDiv(worldZ, Chunk.SIZE)
        val lx = Math.floorMod(worldX, Chunk.SIZE)
        val ly = Math.floorMod(worldY, Chunk.SIZE)
        val lz = Math.floorMod(worldZ, Chunk.SIZE)

        val chunk = chunkManager.getChunk(cx, cz)
        if (chunk != null) {
            chunk.setBlock(lx, ly, lz, blockId)
            chunkManager.regenerateMesh(cx, 0, cz)
        }
    }

    /**
     * Get chunk at coordinates
     */
    fun getChunk(cx: Int, cz: Int): Chunk? {
        return chunkManager.getChunk(cx, cz)
    }

    /**
     * Get visible chunk meshes (with frustum culling)
     */
    fun getVisibleMeshes(px: Float, py: Float, pz: Float, frustumPlanes: FloatArray?): Map<Long, ChunkMesh> {
        val pcx = Math.floor((px / Chunk.SIZE).toDouble()).toInt()
        val pcz = Math.floor((pz / Chunk.SIZE).toDouble()).toInt()
        return chunkManager.getVisibleMeshes(pcx, 0, pcz, frustumPlanes)
    }

    val loadedChunkCount: Int get() = chunkManager.loadedChunkCount
    val pendingWorldGen: Int get() = chunkManager.pendingWorldGen
    val pendingMeshGen: Int get() = chunkManager.pendingMeshGen

    fun dispose() {
        chunkManager.dispose()
    }

    companion object {
        @JvmStatic
        fun setBufferDeleter(deleter: BiConsumer<Long, Long>) {
            ChunkMesh.setBufferDeleter(deleter)
        }
    }
}
