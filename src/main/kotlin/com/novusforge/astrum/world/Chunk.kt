package com.novusforge.astrum.world

/**
 * Chunk - 32×32×32 voxel container
 * Formula: "32(WIDTH)x32(HEIGHT)x32(LENGTH) and better performance"
 * "one-dimensional byte array (better performance)"
 * "upgrade chunk data blocks as shorts (more blocks)"
 * Kotlin conversion: Using const val and bit-shift optimization.
 */
class Chunk {
    companion object {
        const val SIZE = 32
        const val SIZE_BITS = 5 // log2(32) = 5
        const val VOLUME = SIZE * SIZE * SIZE
    }

    // Using short[] for block IDs (supports 65536 block types)
    // 1D array for cache locality (Formula optimization)
    private val blocks = ShortArray(VOLUME)

    /**
     * Set block at local coordinates
     */
    fun setBlock(x: Int, y: Int, z: Int, blockId: Short) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE || z < 0 || z >= SIZE) return
        blocks[getIndex(x, y, z)] = blockId
    }

    /**
     * Get block at local coordinates
     */
    fun getBlock(x: Int, y: Int, z: Int): Short {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE || z < 0 || z >= SIZE) return 0
        return blocks[getIndex(x, y, z)]
    }

    /**
     * Fast 1D index calculation using bit shifts
     * Formula: "x | (y << 5) | (z << 10)" for 32-size chunks
     */
    private fun getIndex(x: Int, y: Int, z: Int): Int {
        return (x and 0x1F) or ((y and 0x1F) shl 5) or ((z and 0x1F) shl 10)
    }

    /**
     * Get all blocks (for mesh generation)
     */
    fun getBlocks(): ShortArray = blocks

    /**
     * Clear chunk data
     */
    fun dispose() {
        blocks.fill(0)
    }
}
