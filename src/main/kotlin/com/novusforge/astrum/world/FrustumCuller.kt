package com.novusforge.astrum.world

/**
 * FrustumCuller - Visibility testing
 * Formula: "Frustum culling(Bounding box of the chunk is within the camera point of view try to render)"
 * Kotlin conversion: Using object singleton and idiomatic math.
 */
object FrustumCuller {
    
    const val LEFT = 0
    const val RIGHT = 1
    const val BOTTOM = 2
    const val TOP = 3
    const val NEAR = 4
    const val FAR = 5

    /**
     * Check if chunk is visible in view frustum
     * Returns true if frustum planes are null (no culling)
     */
    @JvmStatic
    fun isChunkVisible(cx: Int, cy: Int, cz: Int, planes: FloatArray?): Boolean {
        if (planes == null || planes.size < 24) return true

        val minX = cx * Chunk.SIZE - 0.5f
        val minY = cy * Chunk.SIZE - 0.5f
        val minZ = cz * Chunk.SIZE - 0.5f
        val maxX = minX + Chunk.SIZE + 0.5f
        val maxY = minY + Chunk.SIZE + 0.5f
        val maxZ = minZ + Chunk.SIZE + 0.5f

        // Test against all 6 frustum planes
        for (i in 0 until 6) {
            val nx = planes[i * 4]
            val ny = planes[i * 4 + 1]
            val nz = planes[i * 4 + 2]
            val d = planes[i * 4 + 3]

            // Get corner most in plane direction
            var px = if (nx >= 0) maxX else minX
            var py = if (ny >= 0) maxY else minY
            var pz = if (nz >= 0) maxZ else minZ

            if (nx * px + ny * py + nz * pz + d > 0) continue

            // Get opposite corner
            px = if (nx >= 0) minX else maxX
            py = if (ny >= 0) minY else maxY
            pz = if (nz >= 0) minZ else maxZ

            if (nx * px + ny * py + nz * pz + d <= 0) {
                return false // Outside frustum
            }
        }
        return true // Inside or intersecting frustum
    }

    /**
     * Check if individual block is visible
     */
    @JvmStatic
    fun isBlockVisible(wx: Int, wy: Int, wz: Int, planes: FloatArray?): Boolean {
        if (planes == null || planes.size < 24) return true

        val minX = wx - 0.5f
        val minY = wy - 0.5f
        val minZ = wz - 0.5f
        val maxX = minX + 1.0f
        val maxY = minY + 1.0f
        val maxZ = minZ + 1.0f

        for (i in 0 until 6) {
            val nx = planes[i * 4]
            val ny = planes[i * 4 + 1]
            val nz = planes[i * 4 + 2]
            val d = planes[i * 4 + 3]

            var px = if (nx >= 0) maxX else minX
            var py = if (ny >= 0) maxY else minY
            var pz = if (nz >= 0) maxZ else minZ

            if (nx * px + ny * py + nz * pz + d > 0) continue

            px = if (nx >= 0) minX else maxX
            py = if (ny >= 0) minY else maxY
            pz = if (nz >= 0) minZ else maxZ

            if (nx * px + ny * py + nz * pz + d <= 0) {
                return false
            }
        }
        return true
    }
}
