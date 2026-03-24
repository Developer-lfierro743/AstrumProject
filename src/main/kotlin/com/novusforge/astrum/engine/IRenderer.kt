package com.novusforge.astrum.engine

import org.joml.Matrix4f
import com.novusforge.astrum.world.ChunkMesh

/**
 * Renderer Interface - Common API for Vulkan and OpenGL
 * Following Formula: "universal unification"
 * Kotlin Conversion: Using standard Kotlin types and interface structure.
 */
interface IRenderer {
    fun init(): Boolean
    fun render(view: Matrix4f, projection: Matrix4f, meshes: Map<Long, ChunkMesh>)
    fun windowShouldClose(): Boolean
    fun getWindow(): Long
    fun getAspectRatio(): Float
    fun setRenderTestCube(render: Boolean)
    fun cleanup()
    fun deleteBuffer(bufferId: Long, memoryId: Long)
    val rendererName: String
}
