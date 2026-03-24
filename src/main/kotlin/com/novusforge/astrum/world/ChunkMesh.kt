package com.novusforge.astrum.world

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.function.BiConsumer

/**
 * ChunkMesh - GPU-ready mesh data for a chunk
 * Formula: "separating Chunk into different Meshes for opaque and transparent blocks"
 * Kotlin conversion: Using property accessors, lists, and idiomatic buffer building.
 */
class ChunkMesh {
    
    companion object {
        // Vertex format: position (x,y,z) + color (r,g,b) = 6 floats = 24 bytes
        const val VERTEX_SIZE = 6
        
        // Callback for buffer deletion
        private var bufferDeleter: BiConsumer<Long, Long>? = null

        @JvmStatic
        fun setBufferDeleter(deleter: BiConsumer<Long, Long>?) {
            bufferDeleter = deleter
        }
    }
    
    // Opaque mesh data
    private val opaquePositions = mutableListOf<FloatArray>()
    private val opaqueIndices = mutableListOf<IntArray>()
    var opaqueVertexCount = 0
        private set
    var opaqueIndexCount = 0
        private set
    
    // Transparent mesh data (for water, glass, etc.)
    private val transparentPositions = mutableListOf<FloatArray>()
    private val transparentIndices = mutableListOf<IntArray>()
    var transparentVertexCount = 0
        private set
    var transparentIndexCount = 0
        private set
    
    // GPU buffer IDs (Vulkan)
    var opaqueVboId: Long = 0
    var opaqueIboId: Long = 0
    var opaqueVboMemId: Long = 0
    var opaqueIboMemId: Long = 0

    /**
     * Add opaque quad (4 vertices, 2 triangles = 6 indices)
     */
    fun addOpaqueQuad(vertices: FloatArray, indices: IntArray) {
        val baseVertex = opaqueVertexCount
        for (i in indices) {
            opaqueIndices.add(intArrayOf(baseVertex + i))
        }
        opaquePositions.add(vertices)
        opaqueVertexCount += 4
        opaqueIndexCount += indices.size
    }

    /**
     * Add transparent quad
     */
    fun addTransparentQuad(vertices: FloatArray, indices: IntArray) {
        val baseVertex = transparentVertexCount
        for (i in indices) {
            transparentIndices.add(intArrayOf(baseVertex + i))
        }
        transparentPositions.add(vertices)
        transparentVertexCount += 4
        transparentIndexCount += indices.size
    }

    fun hasOpaqueData(): Boolean = opaqueVertexCount > 0
    fun hasTransparentData(): Boolean = transparentVertexCount > 0

    /**
     * Build vertex buffer for GPU upload
     */
    fun buildOpaqueVertexData(): FloatBuffer {
        val data = FloatArray(opaqueVertexCount * VERTEX_SIZE)
        var idx = 0
        for (verts in opaquePositions) {
            for (v in verts) {
                data[idx++] = v
            }
        }
        return ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(data)
            .flip()
    }

    /**
     * Build index buffer for GPU upload
     */
    fun buildOpaqueIndexData(): IntBuffer {
        val data = IntArray(opaqueIndexCount)
        var idx = 0
        for (indices in opaqueIndices) {
            data[idx++] = indices[0]
        }
        return ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
            .put(data)
            .flip()
    }

    /**
     * Cleanup GPU resources
     */
    fun dispose() {
        bufferDeleter?.let { deleter ->
            if (opaqueVboId != 0L) deleter.accept(opaqueVboId, opaqueVboMemId)
            if (opaqueIboId != 0L) deleter.accept(opaqueIboId, opaqueIboMemId)
        }
        opaqueVboId = 0
        opaqueIboId = 0
        opaqueVboMemId = 0
        opaqueIboMemId = 0
        opaquePositions.clear()
        opaqueIndices.clear()
    }
}
